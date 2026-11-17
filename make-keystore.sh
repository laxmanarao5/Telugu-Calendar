#!/bin/sh
# Creates a signing key once (inside Docker) and prints its SHA-1.
# Android installs an update over the app only if it is signed with the SAME key, so keep keystore/ safe and backed up.
# Passwords are random and stored only in keystore/keystore.properties (git-ignored).
#   docker compose run --rm android sh make-keystore.sh
set -e
mkdir -p keystore
if [ ! -f keystore/telugu-calendar.keystore ]; then
  PASS=$(head -c 24 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 20)
  keytool -genkeypair -v -keystore keystore/telugu-calendar.keystore -alias telugucalendar \
    -keyalg RSA -keysize 2048 -validity 10000 -storepass "$PASS" -keypass "$PASS" \
    -dname "CN=Telugu Calendar, C=IN" > /dev/null 2>&1
  printf 'storeFile=keystore/telugu-calendar.keystore\nstorePassword=%s\nkeyAlias=telugucalendar\nkeyPassword=%s\n' "$PASS" "$PASS" > keystore/keystore.properties
  echo "Created keystore/telugu-calendar.keystore. Back this folder up."
fi
PASS=$(grep '^storePassword=' keystore/keystore.properties | cut -d= -f2)
echo "SHA-1:"
keytool -list -v -keystore keystore/telugu-calendar.keystore -alias telugucalendar -storepass "$PASS" 2>/dev/null | grep 'SHA1:' | sed 's/^ *//'
