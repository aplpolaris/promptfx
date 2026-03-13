#!/bin/bash
# Run all tests tagged "gemini" across all modules

set -e

echo "=== Running gemini-tagged tests in promptkt ==="
(cd promptkt && mvn test -Dgroups=gemini)

echo "=== Running gemini-tagged tests in promptfx ==="
(cd promptfx && mvn test -Dgroups=gemini)

echo "=== Running gemini-tagged tests in promptex ==="
(cd promptex && mvn test -Dgroups=gemini)

echo "=== Running gemini-tagged tests in promptrt ==="
(cd promptrt && mvn test -Dgroups=gemini)

echo "=== All gemini-tagged tests completed successfully ==="
