#!/bin/bash
# Build and deploy sample plugins script

echo "Building PromptFx Sample Plugins..."

# Build both plugins
mvn clean package -pl promptfx-sample-plugin,promptfx-sample-textplugin -q

if [ $? -eq 0 ]; then
    echo "Build successful!"
    
    # Copy to config directory
    echo "Deploying plugins to config directory..."
    mkdir -p promptfx/config/plugins
    cp promptfx-sample-plugin/target/promptfx-sample-plugin-*[!javadoc][!sources].jar promptfx/config/plugins/
    cp promptfx-sample-textplugin/target/promptfx-sample-textplugin-*[!javadoc][!sources].jar promptfx/config/plugins/
    
    echo "Plugins deployed successfully!"
    echo "The plugin JARs are now in promptfx/config/plugins/ and will be loaded automatically when PromptFx starts."
    echo ""
    echo "To use the plugins:"
    echo "1. Start PromptFx"
    echo "2. NavigableWorkspaceView plugin: Look for 'Hello World' under the 'Custom' tab in the UI (Sample Plugin category)"
    echo "3. TextPlugin: Look for 'SampleText' models in any model selection dropdown"
else
    echo "Build failed! Please check the Maven output above for errors."
    exit 1
fi