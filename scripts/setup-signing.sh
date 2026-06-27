#!/usr/bin/env bash
# scripts/setup-signing.sh
#
# One-command signing setup for Auto Unstack.
#
# Generates a release keystore and stores all four signing secrets directly
# in the GitHub repository using the GitHub CLI (gh).  After this runs once,
# every Release APK workflow invocation will produce a properly signed APK.
#
# Requirements:
#   - gh CLI installed and authenticated  (https://cli.github.com)
#   - keytool available (included with any JDK; Android Studio ships one)
#
# Usage:
#   ./scripts/setup-signing.sh [owner/repo]
#
# owner/repo defaults to the repository detected from the current directory.

set -euo pipefail

ALIAS="auto-unstack"
KEYSTORE_FILE="$(mktemp --suffix=.jks)"

cleanup() { rm -f "${KEYSTORE_FILE}"; }
trap cleanup EXIT

# ---------------------------------------------------------------------------
# Resolve repository
# ---------------------------------------------------------------------------
REPO="${1:-}"
if [ -z "${REPO}" ]; then
  REPO="$(gh repo view --json nameWithOwner -q .nameWithOwner 2>/dev/null || true)"
fi
if [ -z "${REPO}" ]; then
  echo "Error: could not detect repository. Run from the repo directory or pass owner/repo." >&2
  exit 1
fi

# ---------------------------------------------------------------------------
# Check dependencies
# ---------------------------------------------------------------------------
for cmd in keytool openssl gh; do
  if ! command -v "${cmd}" &>/dev/null; then
    echo "Error: '${cmd}' is required but not found in PATH." >&2
    exit 1
  fi
done

# ---------------------------------------------------------------------------
# Generate secure random passwords
# ---------------------------------------------------------------------------
KEYSTORE_PASSWORD="$(openssl rand -base64 32 | tr -dc 'A-Za-z0-9' | head -c 32)"
KEY_PASSWORD="$(openssl rand -base64 32 | tr -dc 'A-Za-z0-9' | head -c 32)"

# ---------------------------------------------------------------------------
# Generate keystore
# ---------------------------------------------------------------------------
echo "Generating keystore..."
keytool -genkeypair \
  -storetype PKCS12 \
  -keystore "${KEYSTORE_FILE}" \
  -keyalg RSA -keysize 2048 -validity 36500 \
  -alias "${ALIAS}" \
  -storepass "${KEYSTORE_PASSWORD}" \
  -keypass "${KEY_PASSWORD}" \
  -dname "CN=Auto Unstack, O=Auto Unstack, C=US" \
  -noprompt \
  2>/dev/null

# ---------------------------------------------------------------------------
# Push all four secrets to GitHub
# ---------------------------------------------------------------------------
echo "Uploading signing secrets to ${REPO}..."

# base64 without line-breaks (works on both Linux and macOS)
base64 < "${KEYSTORE_FILE}" | tr -d '\n' \
  | gh secret set KEYSTORE_BASE64 --repo "${REPO}"
printf '%s' "${KEYSTORE_PASSWORD}" | gh secret set KEYSTORE_PASSWORD --repo "${REPO}"
printf '%s' "${ALIAS}"             | gh secret set KEY_ALIAS          --repo "${REPO}"
printf '%s' "${KEY_PASSWORD}"      | gh secret set KEY_PASSWORD       --repo "${REPO}"

# ---------------------------------------------------------------------------
# Done
# ---------------------------------------------------------------------------
echo ""
echo "✓ Signing secrets configured for ${REPO}."
echo "  Future releases will be automatically signed with your release keystore."
echo ""
echo "  NOTE: The keystore is stored only in GitHub Secrets."
echo "  To obtain a local copy for backup, re-run this script and copy"
echo "  \${KEYSTORE_FILE} before it is deleted, or generate one manually:"
echo "    keytool -genkeypair -storetype PKCS12 -keystore my-key.jks \\"
echo "      -keyalg RSA -keysize 2048 -validity 36500 -alias auto-unstack"
