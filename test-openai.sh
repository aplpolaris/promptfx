#!/bin/bash
# Run all tests tagged "openai" across all modules

set -e

echo "=== Running openai-tagged tests in promptkt ==="
(cd promptkt && mvn test -Dgroups=openai)

echo "=== Running openai-tagged tests in promptfx ==="
(cd promptfx && mvn test -Dgroups=openai)

echo "=== Running openai-tagged tests in promptex ==="
(cd promptex && mvn test -Dgroups=openai)

echo "=== Running openai-tagged tests in promptrt ==="
(cd promptrt && mvn test -Dgroups=openai)

echo "=== All openai-tagged tests completed successfully ==="
