# scripts/dev release prepare | release publish, hotfix start. Meant to be sourced.
#
# Cycle (CONTRIBUTING.md, "Versions and releases"):
#   1. release prepare <version>: from developement (or hotfix/<version> for a patch), commits the
#      version and the changelog on release/v<version> (or on the hotfix branch), runs the checks,
#      pushes and opens the pull request to main.
#   2. The maintainer merges it on GitHub with "Create a merge commit".
#   3. release publish <version>: signed tag on the merge commit of main, jar built from it, draft
#      GitHub release (pre-release for alpha and beta), main brought back into developement,
#      finished milestones closed.

release_check_version() {
    version_valid "$1" ||
        die "invalid version \"$1\": expected X.Y.Z, X.Y.Z-alpha.N or X.Y.Z-beta.N, without \"v\"."
}

release_tag_absent() {
    if git rev-parse -q --verify "refs/tags/$1" >/dev/null; then
        die "the tag $1 already exists locally."
    fi
    if [ -n "$(git ls-remote --tags origin "refs/tags/$1")" ]; then
        die "the tag $1 already exists on origin."
    fi
}

# Release notes: a technical first line, then the changelog sections of the version (stdin).
release_write_notes() {
    local version="$1" out="$2"
    {
        echo "**$(prop mod_name) $version**: $(version_channel "$version") channel, Minecraft $(prop minecraft_version), NeoForge $(prop neo_version) or newer."
        echo
        # The "## [version] - date" title would repeat the title of the release.
        sed -e '/^## \[/d' | cat -s
    } >"$out"
}

# Commits adding a feature in a range, one "<hash> <subject>" per line, except those reverted in the
# same range (git revert writes "Revert \"<subject>\"").
freeze_features() {
    local reverted
    reverted="$(git log --no-merges --format='%s' "$1" | sed -n 's/^Revert "\(.*\)"$/\1/p')"
    git log --no-merges --format='%h %s' "$1" | grep -E '^[0-9a-f]+ feat(\(|!|:)' |
        while read -r hash subject; do
            printf '%s\n' "$reverted" | grep -F -x -q -- "$subject" || echo "$hash $subject"
        done || true
}

cmd_release_prepare() {
    local allow_features=0 version="" tag channel core patch branch source head_branch latest prefix
    local last_beta features breaking notes jar
    while [ $# -gt 0 ]; do
        case "$1" in
            --allow-features) allow_features=1; shift ;;
            -*) die "unknown option $1" ;;
            *) [ -z "$version" ] || die "usage: scripts/dev release prepare <version> [--allow-features]"; version="$1"; shift ;;
        esac
    done
    [ -n "$version" ] || die "usage: scripts/dev release prepare <version> [--allow-features]"
    release_check_version "$version"
    tag="v$version"
    channel="$(version_channel "$version")"
    core="$(version_core "$version")"
    patch="${core##*.}"
    require_cliff

    branch="$(current_branch)"
    if [ "$patch" -gt 0 ]; then
        [ "$channel" = release ] || die "a patch version is a hotfix of a release: X.Y.Z without suffix."
        [ "$branch" = "hotfix/$version" ] ||
            die "a patch version is prepared from hotfix/$version (scripts/dev hotfix start $version); current branch: ${branch:-detached HEAD}."
        source=hotfix
        head_branch="$branch"
    else
        [ "$branch" = developement ] ||
            die "a release is prepared from developement (current branch: ${branch:-detached HEAD})."
        source=developement
        head_branch="release/$tag"
    fi

    require_clean
    fetch_origin
    release_tag_absent "$tag"
    if [ "$source" = developement ]; then
        require_branch_absent "$head_branch"
        [ "$(git rev-parse HEAD)" = "$(git rev-parse origin/developement)" ] ||
            die "developement differs from origin/developement: pull or push it first (git status)."
        git merge-base --is-ancestor origin/main HEAD ||
            die "developement does not contain origin/main: a published version was not brought back. Run git merge origin/main, check, git push origin developement, then run the command again."
        latest="$(latest_version)"
    else
        git merge-base --is-ancestor origin/main HEAD ||
            die "$branch does not contain origin/main: it must start from the release it fixes."
        prefix="${core%.*}."
        latest="$(latest_version "$prefix")"
        breaking="$(breaking_commits origin/main..HEAD)"
        [ -z "$breaking" ] ||
            die "a patch version must not break existing worlds, but these commits are marked \"!\":
$breaking"
    fi
    if [ -n "$latest" ] && ! version_gt "$version" "$latest"; then
        die "$version is not newer than the latest version $latest."
    fi

    # Feature freeze: after the first beta of X.Y.Z, only fixes until X.Y.Z is released.
    last_beta="$(latest_version "$core-beta.")"
    if [ -n "$last_beta" ] && [ "$channel" != alpha ]; then
        features="$(freeze_features "v$last_beta..HEAD")"
        if [ -n "$features" ]; then
            if [ "$allow_features" -eq 1 ]; then
                warn "feature freeze since v$last_beta, features allowed by --allow-features:"
                echo "$features" | sed 's/^/    /' >&2
            else
                die "feature freeze since v$last_beta: these commits add features (--allow-features to release them anyway):
$features"
            fi
        fi
    fi

    notes="$DEV_TMPDIR/notes.md"
    cliff --unreleased --tag "$tag" --strip all | release_write_notes "$version" "$notes"
    jar="build/libs/$(prop mod_id)-$(prop minecraft_version)-$version.jar"

    echo
    echo "Release $tag ($channel), from $branch:"
    if [ "$source" = developement ]; then
        plan "create the local branch $head_branch from developement"
    fi
    plan "write mod_version=$version in gradle.properties (now $(prop mod_version)) and regenerate CHANGELOG.md" \
        "run ./gradlew build test and ./gradlew runGameTestServer (log: build/dev-checks.log), check $jar" \
        "commit \"chore(release): $tag\" (signed) on $head_branch" \
        "after confirmation: push $head_branch and open the pull request $head_branch -> main \"Release $tag\""
    if signing_configured; then
        echo "Signing key: configured (format $(git config --get gpg.format || echo openpgp))."
    else
        warn "no signing key (git config user.signingkey): the release commit and tag must be signed. See docs/repository-security.md."
    fi
    echo
    echo "Release notes:"
    sed 's/^/  | /' "$notes"
    echo

    if [ "$DRY_RUN" -eq 1 ]; then
        gh_ready || warn "gh missing or not logged in (gh auth login): needed for the pull request."
        run_checks build/dev-checks.log dev || die "checks failed."
        stop_if_dry_run
    fi

    signing_configured || die "no signing key: nothing was changed. See docs/repository-security.md."
    require_gh

    [ "$source" = hotfix ] || git switch --quiet --create "$head_branch"
    say "mod_version=$version in gradle.properties, CHANGELOG.md"
    awk -v v="$version" '/^mod_version=/ { print "mod_version=" v; next } { print }' gradle.properties >gradle.properties.tmp
    mv gradle.properties.tmp gradle.properties
    cliff --tag "$tag" -o CHANGELOG.md

    if ! run_checks build/dev-checks.log dev || [ ! -f "$jar" ]; then
        [ -f "$jar" ] || echo "dev: jar not found: $jar" >&2
        git checkout -- gradle.properties CHANGELOG.md
        if [ "$source" = developement ]; then
            git switch --quiet developement
            git branch --quiet -D "$head_branch"
        fi
        die "release not prepared: gradle.properties and CHANGELOG.md restored, back on $branch."
    fi

    git add gradle.properties CHANGELOG.md
    git commit --quiet -S -m "chore(release): $tag"
    say "signed commit \"chore(release): $tag\" created on $head_branch."

    echo
    if ! confirm "Push $head_branch and open the pull request to main?"; then
        say "nothing was pushed. To drop the preparation:"
        if [ "$source" = developement ]; then
            echo "    git switch developement && git branch -D $head_branch"
        else
            echo "    git reset --hard HEAD~1"
        fi
        exit 0
    fi
    # The checks have just passed on this exact commit: the pre-push hook would only repeat them.
    git push --no-verify --set-upstream origin "$head_branch"
    gh pr create --repo "$(repo_slug)" --base main --head "$head_branch" --title "Release $tag" --body-file "$notes" ||
        warn "pull request not created (already open?). Open it on GitHub: $head_branch -> main."
    [ "$source" = hotfix ] || git switch --quiet developement

    echo
    say "next steps:"
    echo "  1. On GitHub, merge the pull request with \"Create a merge commit\" (neither squash nor rebase)."
    echo "  2. scripts/dev release publish $version --dry-run, then without --dry-run."
}

# Notes kept between runs, so that they can be rewritten by hand before publishing.
release_notes_file() {
    echo "build/release/notes-$1.md"
}

# Milestones to close after a release, one "<number> <title>" per line: for a final release, the
# milestone of the version; for an alpha or a beta, the lot milestones of the version. Only open
# milestones with no open issue and at least one closed issue: a milestone never used stays open.
milestones_to_close() {
    local version="$1" core titles
    core="$(version_core "$version")"
    if [ "$(version_channel "$version")" = release ]; then
        titles="$(version_title "$core")"
    else
        titles="$(lots_of_version "$core" | while read -r key; do lot_title "$key"; done)"
    fi
    [ -n "$titles" ] || return 0
    gh api "repos/$(repo_slug)/milestones?state=open&per_page=100" --paginate \
        --jq '.[] | select(.open_issues == 0 and .closed_issues > 0) | "\(.number) \(.title)"' 2>/dev/null |
        while read -r number title; do
            if printf '%s\n' "$titles" | grep -F -x -q -- "$title"; then
                echo "$number $title"
            fi
        done
}

# Brings main back into developement after a release: fast-forward when possible, otherwise a
# merge commit. Conflicts limited to gradle.properties and CHANGELOG.md (a hotfix while developement
# has moved on) keep developement's side: the next release rewrites both.
merge_main_into_developement() {
    local tag="$1" target conflicts other
    target="$(git rev-parse "$tag^{commit}")"
    git switch --quiet developement
    git merge --quiet --ff-only origin/developement ||
        die "developement has diverged from origin/developement: sort it out, then run: git merge $tag && git push origin developement"
    if git merge-base --is-ancestor "$target" HEAD; then
        say "developement already contains $tag."
        return 0
    fi
    if git merge --quiet --no-edit -m "Merge $tag into developement" "$target" 2>/dev/null; then
        return 0
    fi
    conflicts="$(git diff --name-only --diff-filter=U)"
    other="$(printf '%s\n' "$conflicts" | grep -v -x -E 'gradle\.properties|CHANGELOG\.md' || true)"
    if [ -n "$other" ]; then
        git merge --abort
        die "conflicts while merging $tag into developement, to solve by hand:
$other
Run: git switch developement && git merge $tag, solve, commit, then git push origin developement"
    fi
    git checkout --ours -- gradle.properties CHANGELOG.md
    git add gradle.properties CHANGELOG.md
    git commit --quiet --no-edit
    say "gradle.properties and CHANGELOG.md kept as on developement."
}

cmd_release_publish() {
    local version="" tag channel target subject jar wt notes title prerelease close_list
    while [ $# -gt 0 ]; do
        case "$1" in
            -*) die "unknown option $1" ;;
            *) [ -z "$version" ] || die "usage: scripts/dev release publish <version>"; version="$1"; shift ;;
        esac
    done
    [ -n "$version" ] || die "usage: scripts/dev release publish <version>"
    release_check_version "$version"
    tag="v$version"
    channel="$(version_channel "$version")"
    require_cliff
    require_clean
    fetch_origin
    release_tag_absent "$tag"

    target="$(git rev-parse origin/main)"
    [ "$(git show origin/main:gradle.properties | sed -n 's/^mod_version=//p' | head -n 1)" = "$version" ] ||
        die "origin/main does not contain mod_version=$version: merge the pull request \"Release $tag\" first."
    git rev-parse -q --verify "origin/main^2" >/dev/null ||
        die "origin/main is not a merge commit: the pull request must be merged with \"Create a merge commit\"."
    subject="$(git log -1 --format=%s "origin/main^2")"
    [ "$subject" = "chore(release): $tag" ] ||
        die "the commit merged last into main is \"$subject\", not \"chore(release): $tag\"."
    [ -z "$(git tag --points-at origin/main)" ] ||
        die "origin/main is already tagged: $(git tag --points-at origin/main | tr '\n' ' ')"

    # The jar is built from main in a separate worktree: the current branch is left alone.
    git worktree prune
    wt="$DEV_TMPDIR/main"
    say "build of the jar from origin/main in a temporary worktree (the first build in a new directory takes several minutes)"
    git worktree add --quiet --detach "$wt" "$target"
    jar="$(prop mod_id)-$(prop minecraft_version)-$version.jar"
    if ! (cd "$wt" && ./gradlew build >"$DEV_TMPDIR/build.log" 2>&1) || [ ! -f "$wt/build/libs/$jar" ]; then
        tail -n 40 "$DEV_TMPDIR/build.log" | sed 's/^/    /' >&2
        mkdir -p build/release
        cp "$DEV_TMPDIR/build.log" build/release/build.log
        git worktree remove --force "$wt"
        die "the jar could not be built from origin/main (log: build/release/build.log)."
    fi
    mkdir -p build/release
    cp "$wt/build/libs/$jar" "build/release/$jar"

    notes="$(release_notes_file "$tag")"
    if [ -f "$notes" ]; then
        say "release notes: $notes (kept from a previous run; delete it to regenerate)"
    else
        (cd "$wt" && cliff --unreleased --tag "$tag" --strip all) | release_write_notes "$version" "$notes"
        say "release notes generated: $notes"
    fi
    git worktree remove --force "$wt"

    prerelease=""
    [ "$channel" = release ] || prerelease="--prerelease"
    title="$(prop mod_name) $version (Minecraft $(prop minecraft_version))"
    if gh_ready; then
        close_list="$(milestones_to_close "$version")"
    else
        close_list=""
        [ "$DRY_RUN" -eq 1 ] || require_gh
        warn "gh missing or not logged in: milestones not checked."
    fi

    echo
    echo "Publication of $tag ($channel):"
    plan "signed tag $tag on origin/main ($(git log -1 --format='%h %s' "$target"))" \
        "push the tag $tag" \
        "GitHub release \"$title\" as a draft${prerelease:+, marked pre-release}, with build/release/$jar and the notes below" \
        "bring main back into developement (fast-forward when possible) and push developement"
    if [ -n "$close_list" ]; then
        printf '%s\n' "$close_list" | while read -r number ms; do plan "close the milestone \"$ms\" (no open issue)"; done
    fi
    echo
    echo "Release notes ($notes):"
    sed 's/^/  | /' "$notes"
    echo
    if [ "$DRY_RUN" -eq 1 ]; then
        echo "Edit $notes now if needed: it is kept and used by the real run."
        stop_if_dry_run
    fi

    signing_configured || die "no signing key: the tag must be signed. See docs/repository-security.md."
    if confirm "Edit the release notes before publishing (\$VISUAL, \$EDITOR or vi)?"; then
        ${VISUAL:-${EDITOR:-vi}} "$notes" </dev/tty >/dev/tty
        echo "Release notes after editing:"
        sed 's/^/  | /' "$notes"
        echo
    fi
    if ! confirm "Tag $tag, push it and create the draft release?"; then
        say "nothing was changed. The notes are kept in $notes."
        exit 0
    fi
    git tag -s "$tag" -m "$title" "$target"
    if ! git push --quiet origin "refs/tags/$tag"; then
        git tag -d "$tag" >/dev/null
        die "the tag $tag could not be pushed: local tag removed, nothing was published. Run the command again."
    fi
    # shellcheck disable=SC2086 # prerelease is empty or one word
    gh release create "$tag" "build/release/$jar" --repo "$(repo_slug)" --verify-tag --draft $prerelease \
        --title "$title" --notes-file "$notes" ||
        die "the tag $tag is pushed but the release was not created. Run again by hand:
    gh release create $tag build/release/$jar --verify-tag --draft $prerelease --title \"$title\" --notes-file $notes"
    say "draft release created: review it on GitHub (Releases), then publish it yourself."

    echo
    if confirm "Bring main back into developement and push developement?"; then
        merge_main_into_developement "$tag"
        git push origin developement
    else
        say "developement not updated. Later: git switch developement && git merge $tag && git push origin developement"
    fi

    if [ -n "$close_list" ]; then
        echo
        if confirm "Close the finished milestones listed above?"; then
            printf '%s\n' "$close_list" | while read -r number ms; do
                gh api --method PATCH "repos/$(repo_slug)/milestones/$number" -f state=closed >/dev/null
                say "milestone \"$ms\" closed."
            done
        fi
    fi
    if git rev-parse -q --verify "refs/heads/release/$tag" >/dev/null; then
        git branch --quiet -D "release/$tag" && say "local branch release/$tag deleted (merged into main)."
    fi
}

cmd_hotfix_start() {
    [ $# -eq 1 ] || die "usage: scripts/dev hotfix start <X.Y.Z>"
    local version="$1" core patch previous branch
    release_check_version "$version"
    [ "$(version_channel "$version")" = release ] || die "a hotfix version has no suffix: X.Y.Z."
    core="$(version_core "$version")"
    patch="${core##*.}"
    [ "$patch" -gt 0 ] || die "a hotfix increments the patch number: X.Y.Z with Z > 0."
    previous="${core%.*}.$((patch - 1))"
    branch="hotfix/$version"

    require_clean
    fetch_origin
    git rev-parse -q --verify "refs/tags/v$previous" >/dev/null || die "no tag v$previous: nothing to fix."
    git tag --points-at origin/main | grep -F -x -q "v$previous" ||
        die "origin/main is not at v$previous (it is at: $(git describe --tags origin/main 2>/dev/null || echo unknown)). Only the latest release on main can get a hotfix."
    require_branch_absent "$branch"
    release_tag_absent "v$version"

    echo
    echo "Hotfix $version of v$previous:"
    plan "create the local branch $branch from origin/main (v$previous); nothing is pushed"
    stop_if_dry_run
    git switch --quiet --no-track --create "$branch" origin/main
    say "on $branch. Next steps:"
    echo "  1. Commit the fixes (fix: ..., with \"Fixes #<issue>\" in the message: the issue closes when"
    echo "     the fix reaches developement, the default branch)."
    echo "  2. scripts/dev release prepare $version --dry-run, then without --dry-run."
}
