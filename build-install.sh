#!/bin/bash
# Clean and install all modules in dependency order

set -e

echo "=== Clean installing promptkt ==="
(cd promptkt && mvn clean install)

echo "=== Clean installing promptfx ==="
(cd promptfx && mvn clean install)

echo "=== Clean installing promptex ==="
(cd promptex && mvn clean install)

echo "=== Clean installing promptrt ==="
(cd promptrt && mvn clean install)

echo "=== All modules installed successfully ==="
