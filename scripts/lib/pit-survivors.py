"""Lists the PIT mutants that survived, grouped by file, so a caller reads the gaps without opening the HTML report."""
import sys
import xml.etree.ElementTree as ElementTree
from collections import defaultdict

mutationsPath = sys.argv[1]
survivorsByFile = defaultdict(list)
uncoveredCountByFile = defaultdict(int)
for mutation in ElementTree.parse(mutationsPath).getroot().findall("mutation"):
    sourceFile = mutation.findtext("sourceFile")
    status = mutation.get("status")
    if status == "SURVIVED":
        survivorsByFile[sourceFile].append(
            (mutation.findtext("lineNumber"), mutation.findtext("mutatedMethod"), mutation.findtext("description"))
        )
    elif status == "NO_COVERAGE":
        uncoveredCountByFile[sourceFile] += 1

print("survived mutants:")
for sourceFile, survivors in sorted(survivorsByFile.items(), key=lambda entry: -len(entry[1])):
    print(f"  {sourceFile}: {len(survivors)}")
    for lineNumber, mutatedMethod, description in survivors:
        print(f"    {lineNumber:>4} {mutatedMethod}: {description[:100]}")
if not survivorsByFile:
    print("  none")
print("no unit coverage (cucumber-only code shows here):",
      dict(sorted(uncoveredCountByFile.items(), key=lambda entry: -entry[1])[:8]) or "none")
