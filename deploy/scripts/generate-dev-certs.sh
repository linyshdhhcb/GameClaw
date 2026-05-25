#!/bin/bash
set -e
KEYSTORE_DIR="app/src/main/resources/keystore"
mkdir -p "$KEYSTORE_DIR"
keytool -genkeypair \
  -alias gameclaw-dev \
  -keyalg RSA \
  -keysize 2048 \
  -validity 365 \
  -storetype PKCS12 \
  -keystore "$KEYSTORE_DIR/gameclaw-dev.p12" \
  -storepass gameclaw \
  -dname "CN=gameclaw-dev, OU=Dev, O=GameClaw, L=Shanghai, ST=Shanghai, C=CN" \
  -ext SAN=DNS:localhost,IP:127.0.0.1
echo "Dev keystore created at $KEYSTORE_DIR/gameclaw-dev.p12"
