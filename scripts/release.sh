#!/usr/bin/env bash
# Prépare et publie une version du mod, entièrement en local (pas de GitHub Actions).
#
# Usage :
#   scripts/release.sh --dry-run <version>   vérifie tout et montre ce qui serait fait, sans rien modifier
#   scripts/release.sh <version>             prépare la version sur developement :
#                                              vérifications, version, CHANGELOG.md, build et tests,
#                                              commit et tag signés, puis (après confirmation)
#                                              envoi et pull request vers main
#   scripts/release.sh --publish <version>   une fois la pull request fusionnée dans main :
#                                              release GitHub en brouillon avec le jar et les notes
#                                              (après confirmation) ; vous la publiez vous-même
#
# <version> : SemVer sans « v », par exemple 0.1.0-alpha.1, 0.1.0-beta.2, 0.1.0.
# Le canal découle du suffixe : -alpha.N = alpha, -beta.N = beta, sans suffixe = release.
# Détails : CONTRIBUTING.md, section « Publier une version ».
set -euo pipefail

usage() {
    sed -n '2,15p' "$0" | sed 's/^# \{0,1\}//'
    exit 2
}

fail() {
    echo "release : $*" >&2
    exit 1
}

info() {
    echo "release : $*"
}

confirm() {
    local answer
    printf '%s [o/N] ' "$1"
    # La réponse vient du terminal, jamais d'un tube : sans terminal, c'est non.
    if ! { read -r answer </dev/tty; } 2>/dev/null; then
        echo
        echo "release : pas de terminal pour confirmer, réponse non. Lancez ce script dans un vrai terminal." >&2
        answer=""
    fi
    case "$answer" in
        o | O | oui | Oui | OUI) return 0 ;;
        *) return 1 ;;
    esac
}

prop() {
    sed -n "s/^$1=//p" gradle.properties | head -n 1
}

mode="prepare"
case "${1:-}" in
    --dry-run) mode="dry-run"; shift ;;
    --publish) mode="publish"; shift ;;
    -h | --help | "") usage ;;
esac
[ $# -eq 1 ] || usage
version="$1"

if ! printf '%s' "$version" | grep -q -E '^[0-9]+\.[0-9]+\.[0-9]+(-(alpha|beta)\.[0-9]+)?$'; then
    fail "version « $version » invalide : attendu X.Y.Z, X.Y.Z-alpha.N ou X.Y.Z-beta.N (sans « v »)."
fi

case "$version" in
    *-alpha.*) channel="alpha" ;;
    *-beta.*) channel="beta" ;;
    *) channel="release" ;;
esac

cd "$(git rev-parse --show-toplevel)"

tag="v$version"
mod_id="$(prop mod_id)"
mod_name="$(prop mod_name)"
mc_version="$(prop minecraft_version)"
neo_version="$(prop neo_version)"
jar="build/libs/${mod_id}-${mc_version}-${version}.jar"
notes="$(mktemp "${TMPDIR:-/tmp}/release-notes.XXXXXX")"
trap 'rm -f "$notes"' EXIT

command -v git-cliff >/dev/null 2>&1 || fail "git-cliff est introuvable (macOS : brew install git-cliff)."

# git-cliff sans son journal (INFO, WARN sur les deux commits anciens hors convention), sauf en cas d'erreur.
cliff() {
    local err
    err="$(mktemp "${TMPDIR:-/tmp}/release-cliff.XXXXXX")"
    if ! git-cliff "$@" 2>"$err"; then
        cat "$err" >&2
        rm -f "$err"
        return 1
    fi
    rm -f "$err"
}

run_checks() {
    info "./gradlew build test"
    ./gradlew build test || return 1
    info "./gradlew runGameTestServer"
    ./gradlew runGameTestServer || return 1
}

write_notes() {
    # Notes de version : en-tête technique, puis les sections du changelog de cette version.
    {
        echo "**${mod_name} ${version}** : canal ${channel}, Minecraft ${mc_version}, NeoForge ${neo_version} ou plus récent."
        echo
        # Le titre « ## [version] - date » ferait doublon avec le titre de la release.
        sed -e '/^## \[/d' | cat -s
    } >"$notes"
}

check_signing() {
    if [ -z "$(git config --get user.signingkey || true)" ]; then
        return 1
    fi
}

# ---------------------------------------------------------------------------------------------
# Publication : release GitHub en brouillon, après la fusion de la pull request dans main.
# ---------------------------------------------------------------------------------------------
if [ "$mode" = "publish" ]; then
    command -v gh >/dev/null 2>&1 || fail "gh (GitHub CLI) est introuvable (macOS : brew install gh)."
    gh auth status >/dev/null 2>&1 || fail "gh n'est pas connecté à GitHub : lancez gh auth login."

    git rev-parse -q --verify "refs/tags/$tag" >/dev/null || fail "le tag $tag n'existe pas en local."
    info "git fetch origin"
    git fetch --quiet --tags origin
    [ -n "$(git ls-remote --tags origin "refs/tags/$tag")" ] || fail "le tag $tag n'est pas sur origin."

    tag_commit="$(git rev-parse "$tag^{commit}")"
    git merge-base --is-ancestor "$tag_commit" origin/main ||
        fail "$tag n'est pas encore dans origin/main : fusionnez d'abord la pull request (commit de fusion)."

    [ -z "$(git status --porcelain)" ] || fail "l'arbre de travail n'est pas propre."
    [ "$(git rev-parse HEAD)" = "$tag_commit" ] ||
        fail "HEAD n'est pas sur $tag : lancez git switch --detach $tag, puis relancez."
    [ "$(prop mod_version)" = "$version" ] || fail "gradle.properties ne contient pas mod_version=$version."

    info "./gradlew build (jar construit depuis le tag)"
    ./gradlew build
    [ -f "$jar" ] || fail "jar introuvable : $jar"

    cliff --current --strip all | write_notes

    echo
    echo "Release à créer en brouillon : $tag (canal $channel)"
    echo "  Fichier : $jar"
    echo "  Notes :"
    sed 's/^/    /' "$notes"
    echo

    prerelease_flag=""
    [ "$channel" = "release" ] || prerelease_flag="--prerelease"

    if ! confirm "Créer la release GitHub $tag en brouillon ?"; then
        info "rien n'a été envoyé."
        exit 0
    fi

    # shellcheck disable=SC2086 # prerelease_flag est vide ou un seul mot
    gh release create "$tag" "$jar" --verify-tag --draft $prerelease_flag \
        --title "${mod_name} ${version} (Minecraft ${mc_version})" \
        --notes-file "$notes"

    info "brouillon créé. Vérifiez-le sur GitHub (onglet Releases), puis publiez-le vous-même."
    info "Revenir sur developement : git switch developement"
    exit 0
fi

# ---------------------------------------------------------------------------------------------
# Préparation (et essai à blanc) : sur developement.
# ---------------------------------------------------------------------------------------------
branch="$(git symbolic-ref --quiet --short HEAD || true)"
[ "$branch" = "developement" ] || fail "à lancer depuis la branche developement (actuelle : ${branch:-HEAD détachée})."
[ -z "$(git status --porcelain)" ] || fail "l'arbre de travail n'est pas propre."

info "git fetch origin"
git fetch --quiet --tags origin
behind="$(git rev-list --count HEAD..origin/developement)"
[ "$behind" -eq 0 ] || fail "developement a $behind commit(s) de retard sur origin/developement : faites git pull."

git rev-parse -q --verify "refs/tags/$tag" >/dev/null && fail "le tag $tag existe déjà en local."
[ -z "$(git ls-remote --tags origin "refs/tags/$tag")" ] || fail "le tag $tag existe déjà sur origin."

if [ "$mode" = "dry-run" ]; then
    echo
    echo "Essai à blanc de $tag (canal $channel), rien ne sera modifié."
    echo "  Version actuelle dans gradle.properties : $(prop mod_version)"
    echo "  Fichier qui sera produit : $jar"
    if check_signing; then
        echo "  Signature : user.signingkey configuré (format : $(git config --get gpg.format || echo openpgp))."
    else
        echo "  ATTENTION : aucune clé de signature (user.signingkey). La vraie release refusera de continuer."
        echo "              Procédure : docs/securite-depot.md, section « Signer ses commits et ses tags »."
    fi
    if command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then
        echo "  GitHub CLI : connecté."
    else
        echo "  ATTENTION : gh absent ou non connecté (gh auth login), nécessaire pour la pull request et la release."
    fi
    echo
    echo "Notes de version prévues :"
    cliff --unreleased --tag "$tag" --strip all | write_notes
    sed 's/^/    /' "$notes"
    echo
    run_checks || fail "vérifications en échec."
    echo
    info "essai à blanc réussi. Pour de vrai : scripts/release.sh $version"
    exit 0
fi

check_signing || fail "aucune clé de signature (git config user.signingkey) : le commit et le tag doivent être signés. Voir docs/securite-depot.md."

info "version $version dans gradle.properties"
awk -v v="$version" '/^mod_version=/ { print "mod_version=" v; next } { print }' gradle.properties >gradle.properties.tmp
mv gradle.properties.tmp gradle.properties

info "CHANGELOG.md"
cliff --tag "$tag" -o CHANGELOG.md

restore() {
    git checkout -- gradle.properties CHANGELOG.md
}

if ! run_checks; then
    restore
    fail "vérifications en échec, gradle.properties et CHANGELOG.md remis en l'état."
fi
if [ ! -f "$jar" ]; then
    restore
    fail "jar introuvable : $jar"
fi

git add gradle.properties CHANGELOG.md
git commit -S -m "chore(release): $tag"
git tag -s "$tag" -m "${mod_name} ${version} (Minecraft ${mc_version})"
info "commit et tag $tag signés créés en local."

cliff --latest --strip all | write_notes

echo
echo "Prêt à envoyer :"
echo "  - branche developement ($(git rev-parse --short HEAD)) et tag $tag vers origin"
echo "  - pull request developement -> main « Release $tag »"
echo

if ! confirm "Envoyer et ouvrir la pull request ?"; then
    echo
    info "rien n'a été envoyé. Pour annuler la préparation :"
    echo "    git tag -d $tag && git reset --hard HEAD~1"
    exit 0
fi

# Le build et les tests viennent de passer sur ce HEAD exact : le hook avant push ne les relance pas.
git push --atomic --no-verify origin developement "$tag"

if command -v gh >/dev/null 2>&1; then
    gh pr create --base main --head developement --title "Release $tag" --body-file "$notes" ||
        info "pull request non créée (déjà ouverte ?). À faire sur GitHub : developement -> main."
else
    info "gh introuvable : ouvrez la pull request developement -> main sur GitHub."
fi

echo
info "étapes suivantes :"
echo "  1. Sur GitHub, fusionnez la pull request avec « Create a merge commit » (ni squash ni rebase)."
echo "  2. scripts/release.sh --publish $version"
