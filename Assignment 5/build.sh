#!/bin/bash
# Compile everything into ./out  (needs JDK 11+; MySQL driver only needed at RUN time for DB programs)
cd "$(dirname "$0")"; mkdir -p out
javac -d out DbConfig.java partA/*.java partB/*.java && echo "Compiled OK"
