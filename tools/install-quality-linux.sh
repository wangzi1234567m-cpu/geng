#!/usr/bin/env bash
set -euo pipefail
tool_dir="${1:-.tools/quality/linux}"
mkdir -p "$tool_dir"
curl --fail --silent --show-error --location https://github.com/gitleaks/gitleaks/releases/download/v8.30.1/gitleaks_8.30.1_linux_x64.tar.gz -o "$tool_dir/gitleaks.tar.gz"
echo "551f6fc83ea457d62a0d98237cbad105af8d557003051f41f3e7ca7b3f2470eb  $tool_dir/gitleaks.tar.gz" | sha256sum --check
tar -xzf "$tool_dir/gitleaks.tar.gz" -C "$tool_dir" gitleaks
curl --fail --silent --show-error --location https://github.com/google/osv-scanner/releases/download/v2.7.0/osv-scanner_linux_amd64 -o "$tool_dir/osv-scanner"
echo "160c6253e2922e9e0a6a9771c5c4c40a4b3c6090b34a3cdd5e80c96cc2e4218b  $tool_dir/osv-scanner" | sha256sum --check
chmod +x "$tool_dir/gitleaks" "$tool_dir/osv-scanner"
curl --fail --silent --show-error --location https://github.com/rhysd/actionlint/releases/download/v1.7.12/actionlint_1.7.12_linux_amd64.tar.gz -o "$tool_dir/actionlint.tar.gz"
echo "8aca8db96f1b94770f1b0d72b6dddcb1ebb8123cb3712530b08cc387b349a3d8  $tool_dir/actionlint.tar.gz" | sha256sum --check
tar -xzf "$tool_dir/actionlint.tar.gz" -C "$tool_dir" actionlint
