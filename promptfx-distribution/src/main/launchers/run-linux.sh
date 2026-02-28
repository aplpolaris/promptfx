#!/bin/bash

echo "---------------------------------------------------------------------"
echo "Starting PromptFx UI..."
echo "---------------------------------------------------------------------"
echo "This app requires Java17+ to run, and has been tested on JDK17."
echo "See README.md for help."
echo "---------------------------------------------------------------------"
java -version
echo "---------------------------------------------------------------------"

DIR="$(cd "$(dirname "$0")" && pwd)"
JVM_OPTS="--add-opens javafx.controls/javafx.scene.control.skin=ALL-UNNAMED --add-opens javafx.graphics/javafx.scene=ALL-UNNAMED"
java $JVM_OPTS -jar "$DIR/promptfx-@version@.jar"

echo "---------------------------------------------------------------------"
echo "PromptFx UI has exited."
echo "---------------------------------------------------------------------"
echo "Press any key to continue..."
read -n 1 -s