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

validate_archive() {
    if ! archive_listing=$(tar -tzf "$archive"); then
        echo "Distribution archive could not be read." >&2
        exit 1
    fi
    archive_root=$(printf '%s\n' "$archive_listing" | awk -F/ 'NF { print $1; exit }')
    case "$archive_root" in
        jcloudflareddns-*) ;;
        *)
            echo "Distribution archive has an unsafe or invalid layout." >&2
            exit 1
            ;;
    esac
    if ! printf '%s\n' "$archive_listing" | awk -v root="$archive_root" '
        NF == 0 { next }
        $0 ~ /^\// || $0 ~ /(^|\/)\.\.(\/|$)/ { invalid = 1; exit }
        $0 != root && index($0, root "/") != 1 { invalid = 1; exit }
        { seen = 1 }
        END { if (invalid || !seen) exit 1 }
    '; then
        echo "Distribution archive has an unsafe or invalid layout." >&2
        exit 1
    fi
    if ! printf '%s\n' "$archive_listing" | grep -F -x "$archive_root/bin/jcloudflareddns" >/dev/null; then
        echo "Distribution archive has an unsafe or invalid layout." >&2
        exit 1
    fi
}

validate_archive

package_version=${JCDDNS_PACKAGE_VERSION:-0.1.0}
case "$package_version" in
    ''|*[!A-Za-z0-9.+_-]*)
        echo "Package version contains unsupported characters." >&2
        exit 1
        ;;
esac
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
