# Versions of the mod: SemVer X.Y.Z, X.Y.Z-alpha.N or X.Y.Z-beta.N, tags v<version>.
# Meant to be sourced; written for bash 3.2.

version_valid() {
    printf '%s' "$1" | grep -q -E '^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-(alpha|beta)\.[1-9][0-9]*)?$'
}

version_channel() {
    case "$1" in
        *-alpha.*) echo alpha ;;
        *-beta.*) echo beta ;;
        *) echo release ;;
    esac
}

# X.Y.Z part of a version.
version_core() {
    echo "${1%%-*}"
}

# Sort key: X Y Z channel-rank N, padded so that a plain string sort orders versions.
# alpha < beta < release for the same X.Y.Z.
version_key() {
    local core="${1%%-*}" rank n
    case "$1" in
        *-alpha.*) rank=0; n="${1##*.}" ;;
        *-beta.*) rank=1; n="${1##*.}" ;;
        *) rank=2; n=0 ;;
    esac
    printf '%06d%06d%06d%d%06d\n' $(echo "$core" | tr '.' ' ') "$rank" "$n"
}

# Succeeds when version $1 is strictly greater than version $2.
version_gt() {
    [ "$(version_key "$1")" \> "$(version_key "$2")" ]
}

# Versions of the local tags, sorted from oldest to newest, one per line.
tagged_versions() {
    local tag
    git tag -l 'v*' | while read -r tag; do
        if version_valid "${tag#v}"; then
            echo "$(version_key "${tag#v}") ${tag#v}"
        fi
    done | sort | cut -d ' ' -f 2
}

# Newest tagged version, empty if there is none. With a prefix (X.Y.), only versions starting with it.
latest_version() {
    tagged_versions | awk -v p="${1:-}" 'index($0, p) == 1' | tail -n 1
}

# Suggested next versions after the latest tag, one "<version> (<reason>)" per line.
next_versions() {
    local latest core n x y z
    latest="$(latest_version)"
    if [ -z "$latest" ]; then
        echo "$(prop mod_version) (first release, from gradle.properties)"
        return
    fi
    core="$(version_core "$latest")"
    x="${core%%.*}"; y="${core#*.}"; y="${y%%.*}"; z="${core##*.}"
    case "$(version_channel "$latest")" in
        alpha)
            n="${latest##*.}"
            echo "$core-alpha.$((n + 1)) (end of the next lot)"
            echo "$core-beta.1 (feature freeze)"
            ;;
        beta)
            n="${latest##*.}"
            echo "$core-beta.$((n + 1)) (more fixes)"
            echo "$core (stable release)"
            ;;
        release)
            echo "$x.$((y + 1)).0-alpha.1 (end of the first lot of the next version)"
            echo "$x.$y.$((z + 1)) (hotfix: scripts/dev hotfix start $x.$y.$((z + 1)))"
            ;;
    esac
}

# Phase of development: "freeze <X.Y.Z>" while the newest tag is a beta, "open" otherwise.
dev_phase() {
    local latest
    latest="$(latest_version)"
    if [ -n "$latest" ] && [ "$(version_channel "$latest")" = beta ]; then
        echo "freeze $(version_core "$latest")"
    else
        echo open
    fi
}
