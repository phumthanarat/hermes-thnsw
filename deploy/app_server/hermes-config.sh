#!/bin/sh
# Sourced from setenv.sh on every start (as root, before the JVM):
# installs the plugins from the image's pristine copy and fills in where the
# database is and the account to use, from the environment:
#
#   HERMES_DB_HOST      MySQL host (default: db)
#   HERMES_DB_USER      account (default: hermes)
#   HERMES_DB_PASSWORD  its password (required)
#
# Starting from the pristine copy each time means a changed password takes
# effect with a restart; nothing secret is stored in the image.

PLUGINS_TEMPLATE=/opt/hermes/plugins
PLUGINS=/hermes_home/plugins

if [ -z "$HERMES_DB_PASSWORD" ]; then
    echo "hermes-config: HERMES_DB_PASSWORD is not set (see deploy/setup.sh)" >&2
fi

rm -rf "$PLUGINS"
cp -a "$PLUGINS_TEMPLATE" "$PLUGINS"

# XML-escape the values, then substitute them literally (perl reads them from
# the environment, so no character in a password needs shell/sed escaping)
HERMES_DB_HOST_XML=$(printf '%s' "${HERMES_DB_HOST:-db}" | perl -pe 's/&/&amp;/g; s/</&lt;/g; s/>/&gt;/g; s/"/&quot;/g')
HERMES_DB_USER_XML=$(printf '%s' "${HERMES_DB_USER:-hermes}" | perl -pe 's/&/&amp;/g; s/</&lt;/g; s/>/&gt;/g; s/"/&quot;/g')
HERMES_DB_PASSWORD_XML=$(printf '%s' "$HERMES_DB_PASSWORD" | perl -pe 's/&/&amp;/g; s/</&lt;/g; s/>/&gt;/g; s/"/&quot;/g')
export HERMES_DB_HOST_XML HERMES_DB_USER_XML HERMES_DB_PASSWORD_XML
grep -rl --include='*.xml' '__HERMES_DB_' "$PLUGINS" | xargs -r perl -pi -e '
    s/__HERMES_DB_HOST__/$ENV{HERMES_DB_HOST_XML}/g;
    s/__HERMES_DB_USER__/$ENV{HERMES_DB_USER_XML}/g;
    s/__HERMES_DB_PASSWORD__/$ENV{HERMES_DB_PASSWORD_XML}/g;'
unset HERMES_DB_HOST_XML HERMES_DB_USER_XML HERMES_DB_PASSWORD_XML
chmod -R go-rwx "$PLUGINS"
