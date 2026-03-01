#!/bin/bash
# Compile and test all modules in dependency order

set -e

echo "=== Compiling and testing promptkt ==="
(cd promptkt && mvn compile test)

echo "=== Compiling and testing promptfx ==="
(cd promptfx && mvn compile test)

echo "=== Compiling and testing promptex ==="
(cd promptex && mvn compile test)

echo "=== Compiling and testing promptrt ==="
(cd promptrt && mvn compile test)

echo "=== All modules compiled and tested successfully ==="
