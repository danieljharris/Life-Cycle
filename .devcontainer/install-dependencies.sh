#!/bin/bash

# install sdkman and java 21 (idempotent)
SDKMAN_DIR="/usr/local/sdkman"
if [ ! -s "$SDKMAN_DIR/bin/sdkman-init.sh" ]; then
    curl -s "https://get.sdkman.io" | bash
fi
source "$SDKMAN_DIR/bin/sdkman-init.sh"

if [ ! -d "$SDKMAN_DIR/candidates/java/21.0.2-ms" ]; then
    yes | sdk install java 21.0.2-ms
fi
sdk default java 21.0.2-ms

# Install Gradle for IDE Ctrl+Click support (actual builds use Bazel)
if [ ! -d "$SDKMAN_DIR/candidates/gradle/8.5" ]; then
    yes | sdk install gradle 8.5
fi
sdk default gradle 8.5

# install buildifier
curl -L https://github.com/bazelbuild/buildtools/releases/download/v8.2.1/buildifier-linux-arm64 \
    -o /usr/local/bin/buildifier

# make buildifier executable
chmod +x /usr/local/bin/buildifier

# setup server
chmod +x ./.devcontainer/setup_server.sh && \
 ./.devcontainer/setup_server.sh