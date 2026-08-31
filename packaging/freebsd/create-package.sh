#!/bin/sh

set -eu

usage() {
    echo "Usage: $0 DISTRIBUTION_ARCHIVE OUTPUT_DIRECTORY" >&2
    exit 2
}

if [ "$#" -ne 2 ]; then
    usage
fi

archive=$1
output_dir=$2

if [ ! -f "$archive" ]; then
    echo "Distribution archive not found: $archive" >&2
    exit 1
fi
if ! command -v pkg >/dev/null 2>&1; then
    echo "The FreeBSD pkg command is required." >&2
    exit 1
fi

archive_root=$(tar -tzf "$archive" | head -n 1 | cut -d/ -f1)
if [ -z "$archive_root" ] || [ "$archive_root" = "." ]; then
    echo "Distribution archive has an invalid layout." >&2
    exit 1
fi

package_version=${JCDDNS_PACKAGE_VERSION:-0.1.0}
stage_dir=$(mktemp -d "${TMPDIR:-/tmp}/jcloudflareddns-package.XXXXXXXX")
trap 'rm -rf "$stage_dir"' EXIT HUP INT TERM

mkdir -p "$stage_dir/root/usr/local/share" "$stage_dir/root/usr/local/bin" "$output_dir"
tar -xzf "$archive" -C "$stage_dir/root/usr/local/share"
ln -s "/usr/local/share/$archive_root/bin/jcloudflareddns" \
    "$stage_dir/root/usr/local/bin/jcloudflareddns"

find "$stage_dir/root/usr/local" \( -type f -o -type l \) -print \
    | sed "s#^$stage_dir/root/usr/local/##" > "$stage_dir/plist"

cat > "$stage_dir/manifest.ucl" <<EOF
name: jcloudflareddns
version: "$package_version"
origin: local/jcloudflareddns
comment: "Secure, lightweight Cloudflare Dynamic DNS client"
maintainer: "Jenny Cabrera Varona <jcabrerav@proactiveidea.com>"
www: "https://github.com/jCodeOps/jCloudflareDDNS"
prefix: /usr/local
desc: "A plain Java one-shot Cloudflare Dynamic DNS client. Requires Java 25."
EOF

pkg create --no-clobber --format txz --root-dir "$stage_dir/root" \
    --out-dir "$output_dir" --manifest "$stage_dir/manifest.ucl" \
    --plist "$stage_dir/plist"
