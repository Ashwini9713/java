#!/bin/bash
# Demonstrates A4.2: old file + new class, same vs different serialVersionUID.
set -e
ROOT="$(cd "$(dirname "$0")" && pwd)"; W="$(mktemp -d)"
mkdir -p "$W/v1" "$W/v2same" "$W/v2diff"
javac -d "$W/v1" "$ROOT/partA/A4/v1/Resident.java"
(cd "$W" && java -cp v1 Resident)                        # writes resident.ser
javac -d "$W/v2same" "$ROOT/partA/A4/v2/Resident.java"
sed 's/serialVersionUID = 1L/serialVersionUID = 2L/' "$ROOT/partA/A4/v2/Resident.java" > "$W/Resident.java"
javac -d "$W/v2diff" "$W/Resident.java"
echo "--- v2 with SAME serialVersionUID (1L):";      (cd "$W" && java -cp v2same Resident)
echo "--- v2 with DIFFERENT serialVersionUID (2L):"; (cd "$W" && java -cp v2diff Resident)
