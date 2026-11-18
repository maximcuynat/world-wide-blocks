# Shared helpers for scripts/dev. Meant to be sourced; written for bash 3.2 (macOS) with set -euo pipefail.
#
# Nothing here names the mod or the repository: the GitHub repository comes from the git remote,
# the mod identity and version from gradle.properties.

DRY_RUN=0

say() {
    echo "dev: $*"
}

warn() {
    echo "dev: warning: $*" >&2
}

die() {
    echo "dev: $*" >&2
    exit 1
}

# Prints a block of lines with an indent: the plan shown before acting.
plan() {
    local line
    for line in "$@"; do
        echo "  - $line"
    done
}

# Asks a yes/no question on the terminal. The answer always comes from the terminal, never from a
# pipe: without a terminal, the answer is no.
confirm() {
    local answer=""
    printf '%s [y/N] ' "$1"
    if ! { read -r answer </dev/tty; } 2>/dev/null; then
        echo
        echo "dev: no terminal to confirm, answering no. Run this command in a real terminal." >&2
        answer=""
    fi
    case "$answer" in
        y | Y | yes | Yes | YES) return 0 ;;
        *) return 1 ;;
    esac
}

# Stops before any change in --dry-run mode.
stop_if_dry_run() {
    if [ "$DRY_RUN" -eq 1 ]; then
        echo
        say "dry run: nothing was changed."
        exit 0
    fi
}

# Value of a key in gradle.properties (first occurrence).
prop() {
    sed -n "s/^$1=//p" gradle.properties | head -n 1
}

# owner/repo of the GitHub repository, read from the raw URL of the origin remote.
repo_slug() {
    local url slug
    url="$(git config --get remote.origin.url || true)"
    [ -n "$url" ] || die "no origin remote: cannot find the GitHub repository."
    slug="$(printf '%s\n' "$url" | sed -n -E 's#^(https://|ssh://git@|git@)github\.com[:/]([^/]+/[^/]+)$#\2#p')"
    slug="${slug%.git}"
    [ -n "$slug" ] || die "the origin remote ($url) is not a GitHub repository."
    echo "$slug"
}

current_branch() {
    git symbolic-ref --quiet --short HEAD || true
}

require_clean() {
    [ -z "$(git status --porcelain)" ] ||
        die "the working tree is not clean. Commit or stash your changes (git stash -u), then run the command again."
}

require_branch_absent() {
    if git rev-parse -q --verify "refs/heads/$1" >/dev/null; then
        die "the branch $1 already exists locally."
    fi
    if git rev-parse -q --verify "refs/remotes/origin/$1" >/dev/null; then
        die "the branch $1 already exists on origin."
    fi
}

fetch_origin() {
    say "git fetch origin"
    git fetch --quiet --prune --tags origin || die "git fetch origin failed (network, or access to the repository)."
}

# Succeeds when gh is installed and logged in.
gh_ready() {
    command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1
}

require_gh() {
    command -v gh >/dev/null 2>&1 || die "gh (GitHub CLI) not found (macOS: brew install gh)."
    gh auth status >/dev/null 2>&1 || die "gh is not logged in to GitHub: run gh auth login."
}

require_cliff() {
    command -v git-cliff >/dev/null 2>&1 || die "git-cliff not found (macOS: brew install git-cliff)."
}

# git-cliff with the repository for the compare links, offline, without its INFO and WARN lines
# unless it fails.
cliff() {
    local err="$DEV_TMPDIR/cliff.err"
    if ! git-cliff --offline --github-repo "$(repo_slug)" "$@" 2>"$err"; then
        cat "$err" >&2
        return 1
    fi
}

signing_configured() {
    [ -n "$(git config --get user.signingkey || true)" ]
}

# Lines of scripts/project/milestones.tsv: kind, key, version, title, description (tab-separated).
milestones_file() {
    echo "scripts/project/milestones.tsv"
}

# Field <n> (1-based) of the milestone line of a given kind and key, empty if unknown.
milestone_field() {
    local kind="$1" key="$2" n="$3"
    awk -F '\t' -v kind="$kind" -v key="$key" -v n="$n" \
        '$0 !~ /^#/ && $1 == kind && $2 == key { print $n; exit }' "$(milestones_file)"
}

lot_title() {
    milestone_field lot "$1" 4
}

lot_version() {
    milestone_field lot "$1" 3
}

# Keys of the lots planned for a version, one per line.
lots_of_version() {
    awk -F '\t' -v v="$1" '$0 !~ /^#/ && $1 == "lot" && $3 == v { print $2 }' "$(milestones_file)"
}

version_title() {
    milestone_field version "$1" 4
}

# Open issues of a milestone (by title), one "#<number> <title>" per line. Empty if the milestone
# does not exist yet.
milestone_open_issues() {
    gh issue list --repo "$(repo_slug)" --milestone "$1" --state open --limit 200 \
        --json number,title --jq '.[] | "#\(.number) \(.title)"' 2>/dev/null || true
}

# Subjects of commits marked "!" or with a BREAKING CHANGE footer, in a range, one per line.
breaking_commits() {
    git log --no-merges --format='%H' "$1" | while read -r sha; do
        if git log -1 --format='%s' "$sha" | grep -q -E '^[a-z]+(\([^)]*\))?!:' ||
            git log -1 --format='%b' "$sha" | grep -q -E '^BREAKING[ -]CHANGE:'; then
            git log -1 --format='%h %s' "$sha"
        fi
    done
}
