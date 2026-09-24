#!/bin/sh
# Sourced from setenv.sh on every Tomcat start. Users live in
# $TOMCAT_USERS on the repository volume and are managed from the admin
# console (Access > Users); this script only creates that file on first
# start, and can reset the administrator:
#
#   APP_ADMIN_USER      administrator's username (default: admin)
#   APP_ADMIN_PASSWORD  its initial password; if unset it is "admin" and
#                       must be changed at first login
#   APP_ADMIN_RESET     "true" to reset the administrator to the above on
#                       this start (e.g. forgotten password); other users
#                       are kept. It resets on EVERY start while set, so
#                       set it for one start only, then remove it.
#   APP_API_USER / APP_API_PASSWORD   the REST API user (default user
#                       apiuser; unset password generated once, kept in
#                       $CREDENTIALS_FILE)
#
# Passwords are stored as salted SHA-256 digests (server.xml's
# CredentialHandler), made with Tomcat's own digest.sh.

TOMCAT_USERS=/hermes_home/repository/tomcat-users.xml
CREDENTIALS_FILE=/hermes_home/repository/.credentials
DEFAULT_ADMIN_PASSWORD=admin

digest() {
    "$CATALINA_HOME/bin/digest.sh" -a SHA-256 -i 10000 -s 16 \
        -h org.apache.catalina.realm.MessageDigestCredentialHandler "$1" \
        | tail -n 1 | sed 's/.*://'
}

random_password() {
    head -c 24 /dev/urandom | base64 | tr -d '/+=\n' | cut -c1-24
}

xml_escape() {
    printf '%s' "$1" | sed -e 's/&/\&amp;/g' -e 's/</\&lt;/g' -e 's/>/\&gt;/g' -e 's/"/\&quot;/g'
}

admin_user_line() {
    user=${APP_ADMIN_USER:-admin}
    password=${APP_ADMIN_PASSWORD:-$DEFAULT_ADMIN_PASSWORD}
    roles="admin,user"
    # the well-known default must be replaced at first login
    if [ -z "$APP_ADMIN_PASSWORD" ]; then
        roles="$roles,password_change_required"
    fi
    echo "  <user username=\"$(xml_escape "$user")\" password=\"$(digest "$password")\" roles=\"$roles\"/>"
}

mkdir -p "$(dirname "$TOMCAT_USERS")"
umask 077

if [ ! -f "$TOMCAT_USERS" ]; then
    api_user=${APP_API_USER:-apiuser}
    api_password=${APP_API_PASSWORD:-$(sed -n 's/^API_PASSWORD=//p' "$CREDENTIALS_FILE" 2>/dev/null)}
    [ -z "$api_password" ] && api_password=$(random_password)
    {
        echo "API_USER=$api_user"
        echo "API_PASSWORD=$api_password"
    } > "$CREDENTIALS_FILE"
    cat > "$TOMCAT_USERS" <<EOF
<?xml version='1.0' encoding='utf-8'?>
<tomcat-users>
  <role rolename="admin"/>
  <role rolename="operator"/>
  <role rolename="viewer"/>
  <role rolename="api"/>
  <role rolename="user"/>
$(admin_user_line)
  <user username="$(xml_escape "$api_user")" password="$(digest "$api_password")" roles="api"/>
</tomcat-users>
EOF
    echo "admin-credentials: created $TOMCAT_USERS with administrator ${APP_ADMIN_USER:-admin}" >&2
elif [ "$APP_ADMIN_RESET" = "true" ]; then
    user=${APP_ADMIN_USER:-admin}
    grep -v "username=\"$(xml_escape "$user")\"" "$TOMCAT_USERS" \
        | sed "s#</tomcat-users>#$(admin_user_line | sed 's/[#&\\]/\\&/g')\n</tomcat-users>#" \
        > "$TOMCAT_USERS.new" && mv "$TOMCAT_USERS.new" "$TOMCAT_USERS"
    echo "admin-credentials: APP_ADMIN_RESET: administrator $user reset" >&2
fi

chmod 600 "$TOMCAT_USERS"
umask 022
