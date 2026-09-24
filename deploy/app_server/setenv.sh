#!/bin/sh
# Sourced automatically by catalina.sh on startup. server.xml's Connector
# ports reference ${catalina.http.port}/${catalina.https.port}; these must
# always resolve to something, so default to the previous hardcoded values
# (8080/8443) when APP_HTTP_PORT/APP_HTTPS_PORT aren't set in the container
# environment.
CATALINA_OPTS="$CATALINA_OPTS -Dcatalina.http.port=${APP_HTTP_PORT:-8080}"
CATALINA_OPTS="$CATALINA_OPTS -Dcatalina.https.port=${APP_HTTPS_PORT:-8443}"
# The admin console (and its sign-in page) redirect plain-HTTP requests to
# HTTPS on APP_ADMIN_HTTPS_PORT: the port browsers reach, which differs from
# APP_HTTPS_PORT when Docker maps it (e.g. 18443 -> 8443).
CATALINA_OPTS="$CATALINA_OPTS -Dhermes.admin.requireHttps=${APP_ADMIN_REQUIRE_HTTPS:-true}"
CATALINA_OPTS="$CATALINA_OPTS -Dhermes.admin.httpsPort=${APP_ADMIN_HTTPS_PORT:-${APP_HTTPS_PORT:-8443}}"
export CATALINA_OPTS

# Users live on the repository volume and are managed in the admin console;
# this creates them on first start (admin/admin) or resets the administrator.
. "$CATALINA_BASE/bin/admin-credentials.sh"
