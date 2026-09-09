#!/usr/bin/env bash
set -euo pipefail
mkdir -p out
find src/main/java -name '*.java' -print0 | xargs -0 javac -d out
java -cp out com.bloodlink.Main
