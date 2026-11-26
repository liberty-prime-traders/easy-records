#!/usr/bin/env python3
"""Compare unit and integration line coverage per source file.

Reads the two JaCoCo XML reports and sorts every instrumented line into one bucket:
  both      covered by unit and cucumber  (candidate duplication)
  unit      covered by unit only
  cucumber  covered by cucumber only
  neither   covered by nothing            (the gap to close)

Usage: coverage-overlap.py <unit jacoco.xml> <integration jacoco.xml> [--lines]
"""
import sys
import xml.etree.ElementTree as ElementTree


def covered_and_missed_lines(report_path):
    covered = {}
    missed = {}
    root = ElementTree.parse(report_path).getroot()
    for package in root.findall("package"):
        for source_file in package.findall("sourcefile"):
            file_key = package.get("name") + "/" + source_file.get("name")
            covered[file_key] = set()
            missed[file_key] = set()
            for line in source_file.findall("line"):
                line_number = int(line.get("nr"))
                if int(line.get("ci")) > 0:
                    covered[file_key].add(line_number)
                else:
                    missed[file_key].add(line_number)
    return covered, missed


def collapse_to_ranges(line_numbers):
    ranges = []
    for line_number in sorted(line_numbers):
        if ranges and line_number == ranges[-1][1] + 1:
            ranges[-1][1] = line_number
        else:
            ranges.append([line_number, line_number])
    return ",".join(str(start) if start == end else f"{start}-{end}" for start, end in ranges)


def main():
    arguments = [argument for argument in sys.argv[1:] if not argument.startswith("--")]
    show_lines = "--lines" in sys.argv
    if len(arguments) != 2:
        sys.exit(__doc__)

    unit_covered, unit_missed = covered_and_missed_lines(arguments[0])
    integration_covered, integration_missed = covered_and_missed_lines(arguments[1])
    file_keys = sorted(set(unit_covered) | set(integration_covered))

    rows = []
    for file_key in file_keys:
        all_lines = (
            unit_covered.get(file_key, set()) | unit_missed.get(file_key, set())
            | integration_covered.get(file_key, set()) | integration_missed.get(file_key, set())
        )
        unit_lines = unit_covered.get(file_key, set())
        integration_lines = integration_covered.get(file_key, set())
        both = unit_lines & integration_lines
        unit_only = unit_lines - integration_lines
        cucumber_only = integration_lines - unit_lines
        neither = all_lines - unit_lines - integration_lines
        rows.append((file_key, len(all_lines), len(both), len(unit_only), len(cucumber_only), neither))

    print(f"{'file':<62}{'lines':>6}{'both':>6}{'unit':>6}{'cuke':>6}{'none':>6}{'cov%':>6}")
    totals = [0, 0, 0, 0, 0]
    for file_key, line_count, both, unit_only, cucumber_only, neither in rows:
        if line_count == 0:
            continue
        covered_percent = 100 * (line_count - len(neither)) // line_count
        print(f"{file_key[-61:]:<62}{line_count:>6}{both:>6}{unit_only:>6}{cucumber_only:>6}{len(neither):>6}"
              f"{covered_percent:>5}%")
        for index, value in enumerate((line_count, both, unit_only, cucumber_only, len(neither))):
            totals[index] += value
        if show_lines and neither:
            print(f"    uncovered lines: {collapse_to_ranges(neither)}")

    line_count, both, unit_only, cucumber_only, neither = totals
    if line_count:
        print(f"{'TOTAL':<62}{line_count:>6}{both:>6}{unit_only:>6}{cucumber_only:>6}{neither:>6}"
              f"{100 * (line_count - neither) // line_count:>5}%")
        covered_lines = line_count - neither
        if covered_lines:
            print(f"\noverlap: {100 * both // covered_lines}% of covered lines are hit by both suites; "
                  f"{100 * unit_only // covered_lines}% unit only, {100 * cucumber_only // covered_lines}% cucumber only.")


if __name__ == "__main__":
    main()
