# scripts/dev project sync: labels, milestones and issues on GitHub from scripts/project/.
# Meant to be sourced.
#
# Can be run again at any time: what exists is left alone, except the color and description of
# labels and the description of milestones, which follow the data files. An issue is recognised by
# the hidden key at the end of its body (<!-- project-key: <group>/<file> -->): it is never edited
# again, whatever is changed on GitHub.
#
# Data:
#   scripts/project/labels.tsv       name, color, description, former names
#   scripts/project/milestones.tsv   kind, key, version, title, description
#   scripts/project/issues/<group>/<NN-slug>.md
#       Title: <title>
#       Labels: <label>, <label>
#       Milestone: lot <key> | version <key>      (optional)
#       <blank line>
#       <body, Markdown>

project_dir() {
    echo "scripts/project"
}

# Header field of an issue file.
issue_field() {
    sed -n "1,/^\$/s/^$2: //p" "$1" | head -n 1
}

# Title of the milestone named by "lot <key>" or "version <key>", empty if none.
issue_milestone_title() {
    local ref kind key
    ref="$(issue_field "$1" Milestone)"
    [ -n "$ref" ] || return 0
    kind="${ref%% *}"
    key="${ref#* }"
    milestone_field "$kind" "$key" 4
}

# Checks the data files before anything is sent: known labels, known milestones, titles.
project_validate() {
    local file label ref errors=0 labels
    labels="$(awk -F '\t' '$0 !~ /^#/ && NF { print $1 }' "$(project_dir)/labels.tsv")"
    for file in $(find "$(project_dir)/issues" -name '*.md' | sort); do
        if [ -z "$(issue_field "$file" Title)" ]; then
            echo "dev: $file: no \"Title:\" line." >&2
            errors=$((errors + 1))
        fi
        ref="$(issue_field "$file" Milestone)"
        if [ -n "$ref" ] && [ -z "$(issue_milestone_title "$file")" ]; then
            echo "dev: $file: unknown milestone \"$ref\" (expected \"lot <key>\" or \"version <key>\" of milestones.tsv)." >&2
            errors=$((errors + 1))
        fi
        issue_field "$file" Labels | tr ',' '\n' | sed 's/^ *//; s/ *$//; /^$/d' >"$DEV_TMPDIR/labels"
        while IFS= read -r label; do
            if ! printf '%s\n' "$labels" | grep -F -x -q -- "$label"; then
                echo "dev: $file: unknown label \"$label\" (not in labels.tsv)." >&2
                errors=$((errors + 1))
            fi
        done <"$DEV_TMPDIR/labels"
    done
    [ "$errors" -eq 0 ] || die "$errors error(s) in $(project_dir): nothing was sent."
}

cmd_project_sync() {
    [ $# -eq 0 ] || die "usage: scripts/dev project sync"
    local slug existing_labels existing_ms existing_keys actions file key title labels ms body
    local name color desc former former_name old cur_color cur_desc number cur_ms_desc state n_actions
    local kind key version a b c d label
    require_gh
    slug="$(repo_slug)"
    project_validate

    say "reading labels, milestones and issues of $slug"
    existing_labels="$(gh label list --repo "$slug" --limit 500 --json name,color,description \
        --jq '.[] | [.name, (.color | ascii_downcase), (.description // "")] | @tsv')"
    existing_ms="$(gh api "repos/$slug/milestones?state=all&per_page=100" --paginate \
        --jq '.[] | [(.number | tostring), .title, (.description // ""), .state] | @tsv')"
    existing_keys="$(gh issue list --repo "$slug" --state all --limit 1000 --json number,body \
        --jq '.[] | select(.body | test("<!-- project-key: [^ ]+ -->")) | "\(.body | capture("<!-- project-key: (?<k>[^ ]+) -->").k) #\(.number)"')"

    # Actions, one per line: <kind><TAB><arguments...>, applied in order after confirmation.
    actions="$DEV_TMPDIR/actions"
    : >"$actions"
    echo
    echo "Labels:"
    while IFS="$(printf '\t')" read -r name color desc former; do
        case "$name" in '#'* | '') continue ;; esac
        cur_color="$(printf '%s\n' "$existing_labels" | awk -F '\t' -v n="$name" '$1 == n { print $2; exit }')"
        if printf '%s\n' "$existing_labels" | cut -f 1 | grep -F -x -q -- "$name"; then
            cur_desc="$(printf '%s\n' "$existing_labels" | awk -F '\t' -v n="$name" '$1 == n { print $3; exit }')"
            if [ "$cur_color" = "$color" ] && [ "$cur_desc" = "$desc" ]; then
                echo "  = $name"
            else
                echo "  ~ $name: color or description updated"
                printf 'label-update\t%s\t%s\t%s\n' "$name" "$color" "$desc" >>"$actions"
            fi
            continue
        fi
        old=""
        for former_name in $(printf '%s' "$former" | tr ',' ' '); do
            if printf '%s\n' "$existing_labels" | cut -f 1 | grep -F -x -q -- "$former_name"; then
                old="$former_name"
                break
            fi
        done
        if [ -n "$old" ]; then
            echo "  ~ $old renamed to \"$name\""
            printf 'label-rename\t%s\t%s\t%s\t%s\n' "$old" "$name" "$color" "$desc" >>"$actions"
        else
            echo "  + $name"
            printf 'label-create\t%s\t%s\t%s\n' "$name" "$color" "$desc" >>"$actions"
        fi
    done <"$(project_dir)/labels.tsv"

    echo
    echo "Milestones:"
    while IFS="$(printf '\t')" read -r kind key version title desc; do
        case "$kind" in '#'* | '') continue ;; esac
        number="$(printf '%s\n' "$existing_ms" | awk -F '\t' -v t="$title" '$2 == t { print $1; exit }')"
        if [ -z "$number" ]; then
            echo "  + $title"
            printf 'milestone-create\t%s\t%s\n' "$title" "$desc" >>"$actions"
            continue
        fi
        cur_ms_desc="$(printf '%s\n' "$existing_ms" | awk -F '\t' -v t="$title" '$2 == t { print $3; exit }')"
        state="$(printf '%s\n' "$existing_ms" | awk -F '\t' -v t="$title" '$2 == t { print $4; exit }')"
        if [ "$cur_ms_desc" = "$desc" ]; then
            echo "  = $title${state:+ ($state)}"
        else
            echo "  ~ $title: description updated"
            printf 'milestone-update\t%s\t%s\n' "$number" "$desc" >>"$actions"
        fi
    done <"$(milestones_file)"

    echo
    echo "Issues:"
    for file in $(find "$(project_dir)/issues" -name '*.md' | sort); do
        key="${file#"$(project_dir)/issues/"}"
        key="${key%.md}"
        title="$(issue_field "$file" Title)"
        number="$(printf '%s\n' "$existing_keys" | awk -v k="$key" '$1 == k { print $2; exit }')"
        if [ -n "$number" ]; then
            echo "  = $number $title"
        else
            ms="$(issue_milestone_title "$file")"
            labels="$(issue_field "$file" Labels)"
            echo "  + $title${labels:+ [$labels]}${ms:+ ($ms)}"
            printf 'issue-create\t%s\n' "$file" >>"$actions"
        fi
    done

    n_actions="$(wc -l <"$actions" | tr -d ' ')"
    echo
    if [ "$n_actions" -eq 0 ]; then
        say "everything is up to date on $slug."
        return 0
    fi
    stop_if_dry_run
    if ! confirm "Apply these $n_actions change(s) on GitHub ($slug)?"; then
        say "nothing was sent."
        exit 0
    fi

    # The actions are read on descriptor 3: gh must not swallow them through its standard input.
    while IFS="$(printf '\t')" read -r kind a b c d <&3; do
        case "$kind" in
            label-create) gh label create "$a" --repo "$slug" --color "$b" --description "$c" ;;
            label-update) gh label create "$a" --repo "$slug" --color "$b" --description "$c" --force ;;
            label-rename) gh label edit "$a" --repo "$slug" --name "$b" --color "$c" --description "$d" ;;
            milestone-create)
                gh api --method POST "repos/$slug/milestones" -f title="$a" -f description="$b" >/dev/null
                ;;
            milestone-update)
                gh api --method PATCH "repos/$slug/milestones/$a" -f description="$b" >/dev/null
                ;;
            issue-create)
                key="${a#"$(project_dir)/issues/"}"
                key="${key%.md}"
                body="$DEV_TMPDIR/issue-body.md"
                {
                    sed '1,/^$/d' "$a"
                    echo
                    echo "<!-- project-key: $key -->"
                } >"$body"
                set -- --repo "$slug" --title "$(issue_field "$a" Title)" --body-file "$body"
                ms="$(issue_milestone_title "$a")"
                [ -z "$ms" ] || set -- "$@" --milestone "$ms"
                issue_field "$a" Labels | tr ',' '\n' | sed 's/^ *//; s/ *$//; /^$/d' >"$DEV_TMPDIR/labels"
                while IFS= read -r label; do
                    set -- "$@" --label "$label"
                done <"$DEV_TMPDIR/labels"
                gh issue create "$@" >/dev/null
                ;;
        esac
        say "done: $kind $a"
    done 3<"$actions" </dev/null
    say "$n_actions change(s) applied on $slug."
}
