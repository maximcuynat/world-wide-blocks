# scripts/dev status: read-only overview of where the repository stands. Meant to be sourced.

# Local lot/, release/ and hotfix/ branches already merged and gone from origin, one per line.
merged_branches() {
    local ref upstream track
    git for-each-ref --format='%(refname:short)|%(upstream:short)|%(upstream:track)' \
        refs/heads/lot refs/heads/release refs/heads/hotfix | while IFS='|' read -r ref upstream track; do
        [ "$ref" = "$(current_branch)" ] && continue
        # Only branches pushed once and since deleted on GitHub (automatic deletion after merge).
        [ -n "$upstream" ] && [ "$track" = "[gone]" ] || continue
        if git merge-base --is-ancestor "$ref" origin/developement 2>/dev/null ||
            git merge-base --is-ancestor "$ref" origin/main 2>/dev/null; then
            echo "$ref"
        fi
    done
}

cmd_status() {
    local slug branch upstream changes phase latest id title issues prs merged
    [ $# -eq 0 ] || die "status takes no argument."
    slug="$(repo_slug)"
    git fetch --quiet --prune --tags origin 2>/dev/null ||
        warn "git fetch origin failed: local information only."

    branch="$(current_branch)"
    echo "Repository   $slug"
    echo "Branch       ${branch:-detached HEAD at $(git rev-parse --short HEAD)}"
    if upstream="$(git rev-parse --abbrev-ref --symbolic-full-name '@{u}' 2>/dev/null)"; then
        echo "Upstream     $upstream: $(git rev-list --count '@{u}..HEAD') commit(s) to push, $(git rev-list --count 'HEAD..@{u}') to pull"
    else
        echo "Upstream     none: $(git rev-list --count HEAD --not --remotes=origin) commit(s) on no branch of origin"
    fi
    changes="$(git status --porcelain | wc -l | tr -d ' ')"
    if [ "$changes" -eq 0 ]; then
        echo "Working tree clean"
    else
        echo "Working tree $changes changed or untracked file(s)"
    fi

    echo
    echo "Mod          $(prop mod_id) $(prop mod_version) (gradle.properties)"
    latest="$(latest_version)"
    echo "Latest tag   ${latest:+v$latest}${latest:-none}"
    echo "Next         $(next_versions | sed -n '1p')"
    next_versions | sed -n '2,$p' | sed 's/^/             /'
    phase="$(dev_phase)"
    case "$phase" in
        freeze*) echo "Phase        feature freeze for ${phase#freeze }: fixes only until it is released" ;;
        *) echo "Phase        development" ;;
    esac

    echo
    if ! gh_ready; then
        echo "GitHub       gh missing or not logged in (gh auth login): issues and pull requests not shown"
    else
        case "$branch" in
            lot/*)
                id="${branch#lot/}"
                title="$(lot_title "$id")"
                if [ -z "$title" ]; then
                    echo "Lot          $id: unknown in $(milestones_file)"
                else
                    issues="$(milestone_open_issues "$title")"
                    echo "Lot          $title, target version $(lot_version "$id")"
                    if [ -n "$issues" ]; then
                        echo "$issues" | sed 's/^/             /'
                    else
                        echo "             no open issue (milestone missing? scripts/dev project sync)"
                    fi
                fi
                ;;
        esac
        prs="$(gh pr list --repo "$slug" --state open --limit 50 \
            --json number,title,headRefName,baseRefName \
            --jq '.[] | "#\(.number) \(.title) (\(.headRefName) -> \(.baseRefName))"' 2>/dev/null || true)"
        if [ -n "$prs" ]; then
            echo "Open PRs"
            echo "$prs" | sed 's/^/             /'
        else
            echo "Open PRs     none"
        fi
    fi

    merged="$(merged_branches)"
    if [ -n "$merged" ]; then
        echo
        echo "Merged branches still present locally (deleted on GitHub):"
        echo "$merged" | sed 's/^/  git branch -d /'
    fi
}
