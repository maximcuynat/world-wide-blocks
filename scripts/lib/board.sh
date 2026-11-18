# scripts/dev project board, and the board steps of lot start and lot finish. Meant to be sourced.
#
# Keeps the Status field of the GitHub Projects board named in scripts/project/board.conf in step
# with the issues of the repository. Can be run again at any time: a status set by hand is never
# overwritten, except Done for a closed issue. Rules, first one that applies:
#   closed issue                              -> Done
#   --start <n>                               -> In Progress
#   --review <n>                              -> In review
#   label "type: idea", no status or Todo     -> Ideas (the label decides)
#   no status                                 -> Todo
# Open issues missing from the board are added first. The option "No status" counts as no status.
#
# Everything goes through the GraphQL API (gh api graphql), as described in "Using the API to
# manage Projects" in the GitHub documentation. Every gh result is checked explicitly: these
# functions also run in subshells where set -e does not apply.

# Options of the Status field, in the wanted order. Missing ones are created; other existing
# options are kept after them, in their current order.
BOARD_STATUSES="Ideas
Todo
In Progress
In review
Done"
# Option counted as "no status": GitHub's board template creates a real option with this name.
BOARD_NO_STATUS="No status"
BOARD_IDEA_LABEL="type: idea"

board_conf() {
    echo "$(project_dir)/board.conf"
}

board_conf_value() {
    [ -f "$(board_conf)" ] || return 0
    sed -n "s/^$1=//p" "$(board_conf)" | head -n 1
}

# Succeeds when board.conf names an owner and a board number.
board_configured() {
    [ -n "$(board_conf_value owner)" ] && printf '%s\n' "$(board_conf_value number)" | grep -q -E '^[0-9]+$'
}

board_require_config() {
    board_configured || die "no board configured: $(board_conf) needs owner=<login> and number=<board number>."
}

board_label() {
    echo "$(board_conf_value owner)/$(board_conf_value number)"
}

# Runs gh api graphql with the given arguments; on failure, prints gh's error and a hint about
# the token scope when that is the cause, then returns 1.
board_gql() {
    local err="$DEV_TMPDIR/board-gh.err"
    if gh api graphql "$@" 2>"$err"; then
        return 0
    fi
    sed 's/^/dev: gh: /' "$err" >&2
    if grep -q -i 'scope' "$err"; then
        echo "dev: gh needs the \"project\" scope: run gh auth refresh -s project (in a real terminal)." >&2
    fi
    return 1
}

BOARD_Q_PROJECT='query BoardProject($owner: String!, $number: Int!) {
  repositoryOwner(login: $owner) {
    ... on ProjectV2Owner {
      projectV2(number: $number) {
        id
        title
        field(name: "Status") {
          ... on ProjectV2SingleSelectField { id name options { id name color description } }
        }
      }
    }
  }
}'

BOARD_Q_ISSUES='query BoardIssues($owner: String!, $name: String!, $endCursor: String) {
  repository(owner: $owner, name: $name) {
    issues(first: 100, after: $endCursor, states: [OPEN, CLOSED]) {
      pageInfo { hasNextPage endCursor }
      nodes {
        id
        number
        state
        title
        labels(first: 50) { nodes { name } }
        projectItems(first: 20) {
          nodes {
            id
            project { id }
            fieldValueByName(name: "Status") { ... on ProjectV2ItemFieldSingleSelectValue { name } }
          }
        }
      }
    }
  }
}'

BOARD_M_OPTIONS='mutation BoardOptions($field: ID!, $options: [ProjectV2SingleSelectFieldOptionInput!]!) {
  updateProjectV2Field(input: {fieldId: $field, singleSelectOptions: $options}) {
    projectV2Field { ... on ProjectV2SingleSelectField { options { id name } } }
  }
}'

BOARD_M_ADD='mutation BoardAdd($project: ID!, $content: ID!) {
  addProjectV2ItemById(input: {projectId: $project, contentId: $content}) { item { id } }
}'

BOARD_M_STATUS='mutation BoardStatus($project: ID!, $item: ID!, $field: ID!, $option: String!) {
  updateProjectV2ItemFieldValue(input: {projectId: $project, itemId: $item, fieldId: $field, value: {singleSelectOptionId: $option}}) {
    projectV2Item { id }
  }
}'

# Reads the board and its Status field into $DEV_TMPDIR/board/project.json:
# {"id", "title", "field": {"id", "name", "options": [{"id", "name", "color", "description"}]}}
board_read_project() {
    local out="$DEV_TMPDIR/board/project.json"
    mkdir -p "$DEV_TMPDIR/board"
    board_gql -f query="$BOARD_Q_PROJECT" -f owner="$(board_conf_value owner)" \
        -F number="$(board_conf_value number)" --jq '.data.repositoryOwner.projectV2' >"$out" ||
        die "cannot read the board $(board_label) ($(board_conf))."
    jq -e '.id' "$out" >/dev/null 2>&1 || die "board $(board_label) not found ($(board_conf))."
    jq -e '.field.id and (.field.options | type == "array")' "$out" >/dev/null 2>&1 ||
        die "the board $(board_label) has no single-select field named \"Status\"."
}

# Id of the Status option with the given name, empty if none.
board_option_id() {
    jq -r --arg n "$1" 'first(.field.options[] | select(.name == $n) | .id) // empty' "$DEV_TMPDIR/board/project.json"
}

# Wanted options missing from the Status field, one per line.
board_missing_options() {
    local name
    printf '%s\n' "$BOARD_STATUSES" | while IFS= read -r name; do
        [ -n "$(board_option_id "$name")" ] || echo "$name"
    done
}

# Reads the issues of the repository into $DEV_TMPDIR/board/issues.tsv, one per line:
# number, state (OPEN or CLOSED), idea or -, issue id, board item id or -, status or -, title.
board_read_issues() {
    local slug pid
    slug="$(repo_slug)"
    pid="$(jq -r '.id' "$DEV_TMPDIR/board/project.json")"
    board_gql --paginate -f query="$BOARD_Q_ISSUES" -f owner="${slug%%/*}" -f name="${slug#*/}" \
        --jq '.data.repository.issues.nodes[]
            | (first(.projectItems.nodes[] | select(.project.id == "'"$pid"'")) // null) as $item
            | [(.number | tostring), .state,
               (if any(.labels.nodes[]; .name == "'"$BOARD_IDEA_LABEL"'") then "idea" else "-" end),
               .id, ($item.id // "-"), ($item.fieldValueByName.name // "-"), .title]
            | @tsv' >"$DEV_TMPDIR/board/issues.tsv" ||
        die "cannot read the issues of $slug."
}

# Field <n> of the issue line of a number, empty if the issue is unknown.
board_issue_field() {
    awk -F '\t' -v num="$1" -v n="$2" '$1 == num { print $n; exit }' "$DEV_TMPDIR/board/issues.tsv"
}

# Succeeds when the comma-separated list $2 contains the number $1.
board_in_list() {
    case ",$2," in *",$1,"*) return 0 ;; *) return 1 ;; esac
}

# Checks the numbers given to --start and --review: issues of the repository, not in both lists.
board_check_numbers() {
    local start="$1" review="$2" num
    for num in $(printf '%s,%s' "$start" "$review" | tr ',' ' '); do
        [ -n "$(board_issue_field "$num" 1)" ] || die "#$num is not an issue of $(repo_slug)."
        if board_in_list "$num" "$start" && board_in_list "$num" "$review"; then
            die "#$num is given to both --start and --review."
        fi
    done
}

# Writes the planned changes to $DEV_TMPDIR/board/actions and shows them. Mode "full" applies every
# rule; mode "moves" only moves the issues of --start and --review (lot start, lot finish).
# Actions, one per line: options | add<TAB>number<TAB>issue id | status<TAB>number<TAB>status
board_plan() {
    local mode="$1" start="$2" review="$3" actions="$DEV_TMPDIR/board/actions"
    local num state idea issue_id item_id status title current target missing n_same=0
    : >"$actions"

    if [ "$mode" = full ]; then
        missing="$(board_missing_options)"
        echo "Status options of the board \"$(jq -r '.title' "$DEV_TMPDIR/board/project.json")\" ($(board_label)):"
        jq -r '.field.options[].name' "$DEV_TMPDIR/board/project.json" | sed 's/^/  = /'
        if [ -n "$missing" ]; then
            printf '%s\n' "$missing" | sed 's/^/  + /'
            echo "  order after the change: $(board_options_payload | jq -r 'map(.name) | join(", ")')"
            echo options >>"$actions"
        fi
        echo
    fi

    echo "Issues:"
    while IFS="$(printf '\t')" read -r num state idea issue_id item_id status title; do
        current="$status"
        [ "$current" != "$BOARD_NO_STATUS" ] || current="-"
        target=""
        if [ "$state" = CLOSED ]; then
            if board_in_list "$num" "$start,$review"; then
                echo "  ! #$num $title: closed, left to Done"
            fi
            # A closed issue that is not on the board stays off it.
            [ "$item_id" != "-" ] || continue
            [ "$mode" = full ] && target="Done"
        elif board_in_list "$num" "$start"; then
            target="In Progress"
        elif board_in_list "$num" "$review"; then
            target="In review"
        elif [ "$mode" = moves ]; then
            continue
        elif [ "$idea" = idea ] && { [ "$current" = "-" ] || [ "$current" = Todo ]; }; then
            target="Ideas"
        elif [ "$current" = "-" ]; then
            target="Todo"
        fi
        [ -n "$target" ] || target="$status"
        if [ "$item_id" = "-" ]; then
            echo "  + #$num $title: added to the board, $target"
            printf 'add\t%s\t%s\n' "$num" "$issue_id" >>"$actions"
            printf 'status\t%s\t%s\n' "$num" "$target" >>"$actions"
        elif [ "$target" != "$status" ]; then
            [ "$status" != "-" ] || status="no status"
            echo "  ~ #$num $title: $status -> $target"
            printf 'status\t%s\t%s\n' "$num" "$target" >>"$actions"
        else
            n_same=$((n_same + 1))
        fi
    done <"$DEV_TMPDIR/board/issues.tsv"
    [ "$mode" = moves ] || echo "  = $n_same issue(s) already in the right column"
}

# Options sent to updateProjectV2Field: the wanted ones in order (existing ones with their id,
# color and description), then the other existing options. The API replaces the whole list and
# clears the values of items whose option is not sent again with its id.
board_options_payload() {
    printf '%s\n' "$BOARD_STATUSES" | jq -R . | jq -s --slurpfile p "$DEV_TMPDIR/board/project.json" '
        . as $wanted
        | ($p[0].field.options) as $cur
        | {"Ideas": ["BLUE", "Ideas, not planned yet"], "In review": ["ORANGE", "Pull request open, waiting for the merge"]} as $new
        | [ $wanted[] as $n
            | (first($cur[] | select(.name == $n))
               // {name: $n, color: ($new[$n][0] // "GRAY"), description: ($new[$n][1] // "")}) ]
          + [ $cur[] | select(.name as $n | $wanted | index($n) | not) ]
        | map({name, color, description} + (if .id then {id} else {} end))'
}

# Creates the missing options, keeping every existing option with its id.
board_apply_options() {
    local payload="$DEV_TMPDIR/board/options.json" body="$DEV_TMPDIR/board/options-body.json" lost
    board_options_payload >"$payload"
    lost="$(jq -r --slurpfile o "$payload" '[.field.options[] | select((.id // "") as $id | $id == "" or ([$o[0][].id] | index($id) | not)) | .name] | join(", ")' \
        "$DEV_TMPDIR/board/project.json")"
    [ -z "$lost" ] || die "the existing option(s) $lost would not be sent again with their id: nothing was changed."
    jq -n --arg q "$BOARD_M_OPTIONS" --arg f "$(jq -r '.field.id' "$DEV_TMPDIR/board/project.json")" \
        --slurpfile o "$payload" '{query: $q, variables: {field: $f, options: $o[0]}}' >"$body"
    board_gql --method POST --input "$body" \
        --jq '.data.updateProjectV2Field.projectV2Field.options | map(.name) | join(", ")' >"$DEV_TMPDIR/board/options.out" ||
        die "the Status options could not be updated: nothing else was changed."
    say "done: Status options are now $(cat "$DEV_TMPDIR/board/options.out")"
    board_read_project
    [ -z "$(board_missing_options)" ] || die "Status options still missing after the update: $(board_missing_options | paste -s -d ',' -)."
}

# Applies $DEV_TMPDIR/board/actions, in order. Returns 1 at the first failure.
board_apply() {
    local kind num arg pid fid item option n=0
    if grep -q -x options "$DEV_TMPDIR/board/actions"; then
        board_apply_options
    fi
    pid="$(jq -r '.id' "$DEV_TMPDIR/board/project.json")"
    fid="$(jq -r '.field.id' "$DEV_TMPDIR/board/project.json")"
    : >"$DEV_TMPDIR/board/added.tsv"
    # The actions are read on descriptor 3: gh must not swallow them through its standard input.
    while IFS="$(printf '\t')" read -r kind num arg <&3; do
        case "$kind" in
            add)
                item="$(board_gql -f query="$BOARD_M_ADD" -f project="$pid" -f content="$arg" \
                    --jq '.data.addProjectV2ItemById.item.id')" && [ -n "$item" ] || {
                    echo "dev: #$num could not be added to the board." >&2
                    return 1
                }
                printf '%s\t%s\n' "$num" "$item" >>"$DEV_TMPDIR/board/added.tsv"
                say "done: #$num added to the board"
                ;;
            status)
                item="$(board_issue_field "$num" 5)"
                [ "$item" != "-" ] || item="$(awk -F '\t' -v n="$num" '$1 == n { print $2; exit }' "$DEV_TMPDIR/board/added.tsv")"
                option="$(board_option_id "$arg")"
                if [ -z "$option" ]; then
                    echo "dev: no option \"$arg\" in the Status field (scripts/dev project board creates it)." >&2
                    return 1
                fi
                board_gql -f query="$BOARD_M_STATUS" -f project="$pid" -f item="$item" -f field="$fid" \
                    -f option="$option" --jq '.data.updateProjectV2ItemFieldValue.projectV2Item.id' >/dev/null || {
                    echo "dev: #$num could not be moved to $arg." >&2
                    return 1
                }
                say "done: #$num -> $arg"
                ;;
            *) continue ;;
        esac
        n=$((n + 1))
    done 3<"$DEV_TMPDIR/board/actions" </dev/null
    say "$n change(s) applied on the board $(board_label)."
}

board_action_count() {
    wc -l <"$DEV_TMPDIR/board/actions" | tr -d ' '
}

cmd_project_board() {
    local start="" review="" n
    while [ $# -gt 0 ]; do
        case "$1" in
            --start | --review)
                [ $# -ge 2 ] && printf '%s\n' "$2" | grep -q -E '^[0-9]+(,[0-9]+)*$' ||
                    die "$1 needs issue numbers separated by commas, for example $1 12,14"
                if [ "$1" = --start ]; then start="${start:+$start,}$2"; else review="${review:+$review,}$2"; fi
                shift 2
                ;;
            *) die "usage: scripts/dev project board [--start <n>,<n>...] [--review <n>,<n>...]" ;;
        esac
    done
    require_gh
    board_require_config
    say "reading the board $(board_label) and the issues of $(repo_slug)"
    board_read_project
    board_read_issues
    board_check_numbers "$start" "$review"
    echo
    board_plan full "$start" "$review"
    n="$(board_action_count)"
    echo
    if [ "$n" -eq 0 ]; then
        say "the board $(board_label) is up to date."
        return 0
    fi
    stop_if_dry_run
    if ! confirm "Apply these changes to the board $(board_label)?"; then
        say "nothing was sent."
        exit 0
    fi
    board_apply || die "stopped: the changes listed above as done were applied, the others were not."
}

# Moves open issues to a status, for lot start and lot finish: $1 status, $2 comma-separated
# numbers. Adds them to the board if needed; nothing else changes. Run it in a subshell: it may die.
board_move() {
    local status="$1" numbers="$2" num
    board_read_project
    if [ -z "$(board_option_id "$status")" ]; then
        die "no option \"$status\" in the Status field of the board $(board_label): run scripts/dev project board."
    fi
    board_read_issues
    for num in $(printf '%s' "$numbers" | tr ',' ' '); do
        [ -n "$(board_issue_field "$num" 1)" ] || die "#$num is not an issue of $(repo_slug)."
    done
    if [ "$status" = "In Progress" ]; then
        board_plan moves "$numbers" ""
    else
        board_plan moves "" "$numbers"
    fi
    [ "$(board_action_count)" -eq 0 ] || board_apply
}

# Succeeds when the board steps of lot start and lot finish can run; otherwise says why.
board_ready_for_lot() {
    if ! board_configured; then
        warn "no board configured ($(board_conf)): the board is not updated."
        return 1
    fi
    if ! gh_ready; then
        warn "gh missing or not logged in: the board is not updated."
        return 1
    fi
}
