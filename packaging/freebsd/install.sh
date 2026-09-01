#!/bin/sh

set -eu

usage() {
    echo "Usage: $0 ARCHIVE [PREFIX]" >&2
    exit 2
}

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
    usage
fi

archive=$1
prefix=${2:-/usr/local}

if [ "$(id -u)" -ne 0 ]; then
    echo "This installer must be run as root." >&2
    exit 1
fi

if [ ! -f "$archive" ]; then
    echo "Distribution archive not found: $archive" >&2
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

share_dir="$prefix/share"
install_dir="$share_dir/$archive_root"
bin_dir="$prefix/bin"
launcher="$bin_dir/jcloudflareddns"
config_dir="$prefix/etc/jcloudflareddns"

if [ -e "$install_dir" ] || [ -L "$install_dir" ]; then
    echo "Installation already exists: $install_dir" >&2
    exit 1
fi
if [ -e "$launcher" ] || [ -L "$launcher" ]; then
    echo "Launcher already exists: $launcher" >&2
    exit 1
fi

install -d -m 0755 "$share_dir" "$bin_dir"
install -d -m 0750 "$config_dir"
tar -xzf "$archive" -C "$share_dir"
ln -s "$install_dir/bin/jcloudflareddns" "$launcher"

echo "Installed jCloudflareDDNS to $install_dir"
echo "Launcher: $launcher"
echo "Configuration directory: $config_dir"
