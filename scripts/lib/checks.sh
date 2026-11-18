# Build and tests, shared by the pre-push hook and scripts/dev. Meant to be sourced.
#
# run_checks <log file> <prefix>
#   Runs ./gradlew build test, then ./gradlew runGameTestServer, from the current directory.
#   Gradle's output goes to <log file> only. On failure, prints the last lines of the log
#   (CHECKS_TAIL_LINES, 40 by default) and returns 1.

run_checks() {
    local log="$1" prefix="$2" step
    mkdir -p "$(dirname "$log")"
    : >"$log"
    echo "$prefix: Gradle output goes to $log (tail -f to follow it)."
    for step in "build test" "runGameTestServer"; do
        echo "$prefix: ./gradlew $step"
        printf '\n### ./gradlew %s\n\n' "$step" >>"$log"
        # step is split on purpose: "build test" is two Gradle tasks.
        # shellcheck disable=SC2086
        if ! ./gradlew $step >>"$log" 2>&1; then
            {
                echo "$prefix: ./gradlew $step failed. Last lines of $log:"
                echo
                tail -n "${CHECKS_TAIL_LINES:-40}" "$log" | sed 's/^/    /'
                echo
                if [ "$step" = "runGameTestServer" ]; then
                    echo "$prefix: game test details are in run-gametest/logs/latest.log."
                fi
            } >&2
            return 1
        fi
    done
    echo "$prefix: build, unit tests and game tests passed."
}
