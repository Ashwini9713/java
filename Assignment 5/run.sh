#!/bin/bash
# Usage: ./run.sh <ClassName> [args]      e.g.  ./run.sh B1_PassStoreTest      ./run.sh B3_InsertBenchmark 10000
# DB programs need lib/mysql-connector-j-*.jar (see README) and a running MySQL with setup.sql applied.
cd "$(dirname "$0")"; CP="out:$(ls lib/*.jar 2>/dev/null | tr '\n' ':')"
java -cp "$CP" "$@"
