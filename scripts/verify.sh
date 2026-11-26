#!/usr/bin/env bash
set -uo pipefail

# Usage: ./scripts/verify.sh <domain> [tier] [options]
#        ./scripts/verify.sh <dotted.package> [options]
#   domain  the domain folder name, e.g. account or sale_session
#   tier    platform | organizations | locations; needed only when the domain name appears in more than one
# Options:
#   --features <path[,path...]>  cucumber features to run; default is the .feature files named after the domain
#   --mutate                     also run PIT mutation testing against the unit tests (minutes)
#   --unit-only                  skip the cucumber run and reuse its last coverage data
#   --cucumber-only              skip the unit run and reuse its last coverage data
#
# Runs the unit tests and the cucumber features for one package with coverage on, then prints a verdict, any failing
# tests and a per-file table of which suite covers each line, with the uncovered ranges. Gradle's log goes to
# build/verify.log; the HTML reports are under build/reports/coverage/{unit,integration,merged}/html/index.html.
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT" || exit 1

BASE_PACKAGE="app.myliberty.easy_records"
SOURCE_ROOT="src/main/kotlin/$(echo "$BASE_PACKAGE" | tr . /)"
FEATURE_ROOT="src/cucumberTest/resources/features"
TIERS="platform organizations locations"

fail() { echo "verify: $1" >&2; exit 2; }

TARGET=""
TIER=""
FEATURE_PATHS=""
RUN_MUTATION=0
RUN_UNIT=1
RUN_CUCUMBER=1
while [[ $# -gt 0 ]]; do
  case "$1" in
    --features) FEATURE_PATHS="${2:?--features needs a value}"; shift 2 ;;
    --mutate) RUN_MUTATION=1; shift ;;
    --unit-only) RUN_CUCUMBER=0; shift ;;
    --cucumber-only) RUN_UNIT=0; shift ;;
    -h|--help) sed -n 3,17p "$0"; exit 0 ;;
    -*) fail "unknown option $1" ;;
    *)
      if [[ -z "$TARGET" ]]; then TARGET="$1"
      elif [[ " $TIERS " == *" $1 "* ]]; then TIER="$1"
      else fail "unexpected argument '$1'"
      fi
      shift ;;
  esac
done
[[ -n "$TARGET" ]] || fail "usage: verify.sh <domain> [tier] [--features ...] [--mutate] [--unit-only] [--cucumber-only]"

if [[ "$TARGET" == *.* ]]; then
  PACKAGE_NAME="$TARGET"
else
  SEARCH_TIERS="${TIER:-$TIERS}"
  MATCHES=()
  for TIER_NAME in $SEARCH_TIERS; do
    while IFS= read -r MATCH; do
      [[ -n "$MATCH" ]] && MATCHES+=("$MATCH")
    done < <(find "$SOURCE_ROOT/$TIER_NAME/business" -type d -name "$TARGET" -not -path "*/api/*" 2>/dev/null)
  done
  [[ ${#MATCHES[@]} -gt 0 ]] || fail "no domain folder named '$TARGET' under ${SEARCH_TIERS}"
  [[ ${#MATCHES[@]} -eq 1 ]] || fail "'$TARGET' is ambiguous, pass a tier: ${MATCHES[*]}"
  PACKAGE_NAME="$(echo "${MATCHES[0]}" | sed "s#^src/main/kotlin/##; s#/#.#g")"
  if [[ -z "$FEATURE_PATHS" ]]; then
    TIER_OF_MATCH="$(echo "$PACKAGE_NAME" | cut -d. -f4)"
    DASHED="${TARGET//_/-}"
    FEATURE_PATHS="$(find "$FEATURE_ROOT/$TIER_OF_MATCH" -type f \( -name "${TARGET}*.feature" -o -name "${DASHED}*.feature" \) 2>/dev/null | sort | paste -sd, -)"
  fi
fi

echo "package:  $PACKAGE_NAME"
echo "features: ${FEATURE_PATHS:-none found, the integration side runs the whole suite}"

PACKAGE_ARGUMENT="-PcoveragePackage=$PACKAGE_NAME"
CUCUMBER_ARGUMENTS=()
if [[ -n "$FEATURE_PATHS" ]]; then
  CUCUMBER_ARGUMENTS+=("-Dcucumber.features=$FEATURE_PATHS")
fi

# The agent appends to an existing exec file, so stale data from an earlier or wider run would leak into this one.
[[ $RUN_UNIT -eq 1 ]] && rm -f build/jacoco/test.exec
[[ $RUN_CUCUMBER -eq 1 ]] && rm -f build/jacoco/cucumberCoverageTest.exec

mkdir -p build
: > build/verify.log
# A failing test must not stop the reports: coverage of a red run still shows the gaps. The exit code is kept.
VERIFY_STATUS=0
[[ $RUN_UNIT -eq 1 ]] && { ./gradlew test "$PACKAGE_ARGUMENT" >> build/verify.log 2>&1 || VERIFY_STATUS=1; }
[[ $RUN_CUCUMBER -eq 1 ]] && {
  ./gradlew cucumberCoverageTest "$PACKAGE_ARGUMENT" ${CUCUMBER_ARGUMENTS[@]+"${CUCUMBER_ARGUMENTS[@]}"} >> build/verify.log 2>&1 || VERIFY_STATUS=1
}
./gradlew unitCoverageReport integrationCoverageReport mergedCoverageReport "$PACKAGE_ARGUMENT" >> build/verify.log 2>&1 || VERIFY_STATUS=1

echo "exit status: $VERIFY_STATUS (full log: build/verify.log)"
grep -E "BUILD (SUCCESSFUL|FAILED)|tests completed| FAILED$" build/verify.log | sort -u | head -20
echo
python3 -I scripts/lib/coverage-overlap.py \
  build/reports/coverage/unit/jacoco.xml \
  build/reports/coverage/integration/jacoco.xml \
  --lines

if [[ $RUN_MUTATION -eq 1 ]]; then
  echo
  ./gradlew pitest "$PACKAGE_ARGUMENT" > build/pit.log 2>&1 || VERIFY_STATUS=1
  grep -E "Generated [0-9]+ mutations|Test strength|BUILD (SUCCESSFUL|FAILED)" build/pit.log | tail -3
  python3 -I scripts/lib/pit-survivors.py build/reports/pitest/mutations.xml
  echo "mutation report: build/reports/pitest/index.html (log: build/pit.log)"
fi
exit "$VERIFY_STATUS"
