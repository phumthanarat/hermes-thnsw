#!/bin/sh
# Sourced automatically by catalina.sh on startup. server.xml's Connector
# ports reference ${catalina.http.port}/${catalina.https.port}; these must
# always resolve to something, so default to the previous hardcoded values
# (8080/8443) when APP_HTTP_PORT/APP_HTTPS_PORT aren't set in the container
# environment.
CATALINA_OPTS="$CATALINA_OPTS -Dcatalina.http.port=${APP_HTTP_PORT:-8080}"
CATALINA_OPTS="$CATALINA_OPTS -Dcatalina.https.port=${APP_HTTPS_PORT:-8443}"
export CATALINA_OPTS

# No default password ships in the image: tomcat-users.xml is written from
# APP_ADMIN_*/APP_API_* (or generated credentials) on every start.
. "$CATALINA_BASE/bin/admin-credentials.sh"
