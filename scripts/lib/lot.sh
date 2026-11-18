# scripts/dev lot start | lot finish. Meant to be sourced.
#
# A lot is developed on lot/<id>, created from developement, and merged back into developement by a
# pull request (merge commit). Its tasks are the issues of the milestone named in
# scripts/project/milestones.tsv.

lot_known_ids() {
    awk -F '\t' '$0 !~ /^#/ && $1 == "lot" { printf "%s ", $2 }' "$(milestones_file)"
}

show_lot_issues() {
    local title="$1" issues
    if ! gh_ready; then
        warn "gh missing or not logged in: the issues of the lot are not shown."
        return 0
    fi
    issues="$(milestone_open_issues "$title")"
    if [ -n "$issues" ]; then
        echo "Open issues of the milestone \"$title\":"
        echo "$issues" | sed 's/^/  /'
    else
        echo "No open issue in the milestone \"$title\" (not created yet? scripts/dev project sync)."
    fi
}

cmd_lot_start() {
    [ $# -eq 1 ] || die "usage: scripts/dev lot start <id> (lots: $(lot_known_ids))"
    local id="$1" title branch ahead phase
    title="$(lot_title "$id")"
    [ -n "$title" ] || die "unknown lot \"$id\". Lots in $(milestones_file): $(lot_known_ids)"
    branch="lot/$id"

    require_clean
    fetch_origin
    require_branch_absent "$branch"
    git rev-parse -q --verify refs/remotes/origin/developement >/dev/null ||
        die "origin/developement not found."

    echo
    echo "$title (target version $(lot_version "$id"))"
    echo "Plan:"
    plan "switch to developement and fast-forward it to origin/developement" \
        "create the local branch $branch from developement (nothing is pushed)"
    if git rev-parse -q --verify refs/heads/developement >/dev/null; then
        ahead="$(git rev-list --count origin/developement..developement)"
        [ "$ahead" -eq 0 ] ||
            warn "developement has $ahead commit(s) not pushed: they will be part of $branch."
    fi
    if board_configured; then
        plan "then ask which open issues of the milestone go to In Progress on the board $(board_label)"
    fi
    phase="$(dev_phase)"
    case "$phase" in
        freeze*) warn "feature freeze for ${phase#freeze }: the lot can be developed, but its pull request should wait for that release." ;;
    esac
    echo
    show_lot_issues "$title"
    stop_if_dry_run

    git switch --quiet developement
    git merge --quiet --ff-only origin/developement ||
        die "developement has diverged from origin/developement: sort it out (git status), then run the command again."
    git switch --quiet --create "$branch"
    say "on $branch."
    lot_start_board "$title"
    say "when the lot is done: scripts/dev lot finish --dry-run, then scripts/dev lot finish."
}

# Asks which open issues of the milestone go to In Progress on the board, then moves them. A board
# problem is only a warning: the branch is already created.
lot_start_board() {
    local title="$1" all answer numbers="" num
    board_ready_for_lot || return 0
    all="$(milestone_open_issues "$title" | sed -n 's/^#\([0-9]*\) .*/\1/p')"
    [ -n "$all" ] || return 0
    echo
    answer="$(ask "Issues to move to In Progress on the board (numbers separated by commas, all, or empty for none):")"
    answer="$(printf '%s' "$answer" | tr -d '#' | tr ' ' ',')"
    case "$answer" in
        "") say "board unchanged."; return 0 ;;
        all | ALL | All) numbers="$(printf '%s\n' "$all" | paste -s -d ',' -)" ;;
        *)
            for num in $(printf '%s' "$answer" | tr ',' ' '); do
                if printf '%s\n' "$all" | grep -F -x -q -- "$num"; then
                    numbers="${numbers:+$numbers,}$num"
                else
                    warn "#$num is not an open issue of the milestone \"$title\": left alone."
                fi
            done
            ;;
    esac
    [ -n "$numbers" ] || { say "board unchanged."; return 0; }
    ( board_move "In Progress" "$numbers" ) ||
        warn "the board was not updated. Once the problem is fixed: scripts/dev project board --start $numbers"
}

# Exact title of a GitHub milestone if it exists, empty otherwise.
milestone_exists() {
    gh api "repos/$(repo_slug)/milestones?state=all&per_page=100" --paginate --jq '.[].title' 2>/dev/null |
        grep -F -x -- "$1" || true
}

label_exists() {
    gh label list --repo "$(repo_slug)" --limit 500 --json name --jq '.[].name' 2>/dev/null |
        grep -F -x -- "$1" || true
}

cmd_lot_finish() {
    local except="" branch id title range commits breaking issues closes slug pr_url body labels milestone n verb review
    while [ $# -gt 0 ]; do
        case "$1" in
            --except)
                [ $# -ge 2 ] || die "--except needs a list of issue numbers, for example --except 12,14"
                except="$2"
                shift 2
                ;;
            *) die "usage: scripts/dev lot finish [--except <n>,<n>...]" ;;
        esac
    done
    [ -z "$except" ] || printf '%s\n' "$except" | grep -q -E '^[0-9]+(,[0-9]+)*$' ||
        die "--except: expected issue numbers separated by commas, for example 12,14"

    branch="$(current_branch)"
    case "$branch" in
        lot/*) id="${branch#lot/}" ;;
        *) die "run this command from a lot/<id> branch (current: ${branch:-detached HEAD})." ;;
    esac
    title="$(lot_title "$id")"
    if [ -z "$title" ]; then
        warn "lot \"$id\" is not in $(milestones_file): no milestone, title \"Lot $id\"."
        title="Lot $id"
    fi
    slug="$(repo_slug)"

    require_clean
    require_gh
    fetch_origin
    git merge-base --is-ancestor origin/developement HEAD ||
        die "$branch does not contain the latest origin/developement. Run git merge origin/developement, check, then run the command again."
    range="origin/developement..HEAD"
    n="$(git rev-list --count --no-merges "$range")"
    [ "$n" -gt 0 ] || die "$branch has no commit that is not already in origin/developement."
    commits="$(git log --no-merges --reverse --format='%h %s' "$range")"
    breaking="$(breaking_commits "$range")"

    issues="$(milestone_open_issues "$title")"
    if [ -n "$except" ]; then
        issues="$(printf '%s\n' "$issues" | awk -v ex=",$except," '{ num = substr($1, 2); if (index(ex, "," num ",") == 0) print }')"
    fi
    closes="$(printf '%s\n' "$issues" | sed -n 's/^#\([0-9]*\) .*/Closes #\1/p')"

    milestone="$(milestone_exists "$title")"
    labels=""
    if [ -n "$breaking" ]; then
        if [ -n "$(label_exists breaking)" ]; then
            labels="breaking"
        else
            warn "the label \"breaking\" does not exist yet (scripts/dev project sync): pull request without label."
        fi
    fi
    pr_url="$(gh pr list --repo "$slug" --head "$branch" --base developement --state open \
        --json url --jq '.[0].url // empty' 2>/dev/null || true)"

    body="$DEV_TMPDIR/pr-body.md"
    {
        echo "## Summary"
        echo
        echo "$title."
        echo
        echo "Commits:"
        echo
        printf '%s\n' "$commits" | sed 's/^/- /'
        echo
        echo "## Linked issues"
        echo
        if [ -n "$closes" ]; then echo "$closes"; else echo "None."; fi
        echo
        echo "## Checklist"
        echo
        echo "- [x] Commit messages follow the convention (checked by the commit-msg hook)"
        echo "- [x] \`./gradlew build test\` and \`./gradlew runGameTestServer\` pass (run by \`scripts/dev lot finish\`)"
        echo "- [ ] Display changes checked in game with \`./gradlew runClient\`"
        echo "- [ ] New texts added to both language files (\`en_us.json\` and \`fr_fr.json\`)"
        if [ -n "$breaking" ]; then
            echo "- [x] **Breaks existing worlds**: commits marked \`!\`, label \`breaking\`, details below"
            echo
            echo "## Breaking changes"
            echo
            printf '%s\n' "$breaking" | sed 's/^/- /'
        else
            echo "- [ ] **Breaks existing worlds**"
        fi
    } >"$body"

    echo
    echo "Plan for $branch ($n commit(s)):"
    plan "run ./gradlew build test and ./gradlew runGameTestServer (log: build/dev-checks.log)" \
        "push $branch to origin (the pre-push hook is skipped: the same checks have just passed)"
    if [ -n "$pr_url" ]; then
        plan "pull request already open, updated by the push: $pr_url"
    else
        plan "open the pull request $branch -> developement \"$title\"${milestone:+, milestone \"$milestone\"}${labels:+, label \"$labels\"}"
    fi
    # Issues moved to In review on the board after the pull request (none without a board).
    review=""
    board_configured && review="$(printf '%s\n' "$closes" | sed -n 's/^Closes #//p' | paste -s -d ',' -)"
    if [ -n "$review" ]; then
        plan "move #$(printf '%s' "$review" | sed 's/,/, #/g') to In review on the board $(board_label)"
    fi
    [ -n "$milestone" ] || warn "no milestone \"$title\" on GitHub (scripts/dev project sync): pull request without milestone."
    [ -n "$closes" ] || warn "no open issue to close in the milestone \"$title\"."
    [ -z "$breaking" ] || warn "commits marked \"!\": this lot breaks existing worlds."
    case "$(dev_phase)" in
        freeze*) warn "feature freeze for $(dev_phase | cut -d ' ' -f 2): merging this lot now would bring features into the beta." ;;
    esac
    echo
    echo "Pull request description:"
    sed 's/^/  | /' "$body"
    echo

    run_checks build/dev-checks.log dev || die "checks failed: nothing was pushed."
    stop_if_dry_run

    echo
    if [ -n "$pr_url" ]; then verb="update"; else verb="open"; fi
    if ! confirm "Push $branch and $verb the pull request?"; then
        say "nothing was pushed."
        exit 0
    fi
    git push --no-verify --set-upstream origin "$branch"
    if [ -n "$pr_url" ]; then
        say "pull request updated: $pr_url"
    else
        # shellcheck disable=SC2046 # optional flags, each one word or a quoted value
        gh pr create --repo "$slug" --base developement --head "$branch" --title "$title" \
            --body-file "$body" ${milestone:+--milestone "$milestone"} ${labels:+--label "$labels"}
    fi
    if [ -n "$review" ] && board_ready_for_lot; then
        # The push is done: a board problem is only a warning.
        ( board_move "In review" "$review" ) ||
            warn "the board was not updated. Once the problem is fixed: scripts/dev project board --review $review"
    fi
    say "after the merge on GitHub (Create a merge commit): git switch developement && git pull --ff-only"
}
