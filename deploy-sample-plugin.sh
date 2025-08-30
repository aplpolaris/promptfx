#!/bin/bash
# Build and deploy sample plugin script

echo "Building PromptFx Sample Plugin..."

# Build the plugin
mvn clean package -pl promptfx-sample-plugin -q

if [ $? -eq 0 ]; then
    echo "Build successful!"
    
    # Copy to config directory
    echo "Deploying plugin to config directory..."
    mkdir -p promptfx/config/plugins
    cp promptfx-sample-plugin/target/promptfx-sample-plugin-*[!javadoc][!sources].jar promptfx/config/plugins/
    
    echo "Plugin deployed successfully!"
    echo "The plugin JAR is now in promptfx/config/ and will be loaded automatically when PromptFx starts."
    echo ""
    echo "To use the plugin:"
    echo "1. Start PromptFx"
    echo "2. Look for 'Hello World' under the 'Sample' category in the UI"
else
    echo "Build failed! Please check the Maven output above for errors."
    exit 1
fi