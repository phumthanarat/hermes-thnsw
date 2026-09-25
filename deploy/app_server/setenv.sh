#!/bin/sh
# Sourced automatically by catalina.sh on startup (as root, before the JVM).
# server.xml's Connector ports reference ${catalina.http.port}/
# ${catalina.https.port}; these must always resolve to something, so default
# to 8080/8443 when APP_HTTP_PORT/APP_HTTPS_PORT aren't set.
CATALINA_OPTS="$CATALINA_OPTS -Dcatalina.http.port=${APP_HTTP_PORT:-8080}"
CATALINA_OPTS="$CATALINA_OPTS -Dcatalina.https.port=${APP_HTTPS_PORT:-8443}"

# The admin console (and its sign-in page) redirect plain-HTTP requests to
# HTTPS on APP_ADMIN_HTTPS_PORT: the port browsers reach, which differs from
# APP_HTTPS_PORT when Docker maps it (e.g. 18443 -> 8443).
CATALINA_OPTS="$CATALINA_OPTS -Dhermes.admin.requireHttps=${APP_ADMIN_REQUIRE_HTTPS:-true}"
CATALINA_OPTS="$CATALINA_OPTS -Dhermes.admin.httpsPort=${APP_ADMIN_HTTPS_PORT:-${APP_HTTPS_PORT:-8443}}"

# SAAJ 1.3 (SOAP) uses the JDK's internal Xerces, closed off since Java 9
for pkg in dom jaxp util parsers xni xni.parser impl; do
    CATALINA_OPTS="$CATALINA_OPTS --add-exports java.xml/com.sun.org.apache.xerces.internal.$pkg=ALL-UNNAMED"
done
CATALINA_OPTS="$CATALINA_OPTS --add-opens java.base/java.net=ALL-UNNAMED -Djava.net.preferIPv4Stack=true"

# Plugins and their database settings, the HTTPS certificate, and the
# console users (created on first start, admin/admin).
# Tomcat's own tools (digest.sh, used by admin-credentials.sh) source this
# file again: do the Hermes setup once, for the server start only.
if [ -z "$HERMES_SETENV_DONE" ]; then
    HERMES_SETENV_DONE=1
    export HERMES_SETENV_DONE
    . "${CATALINA_BASE:-$CATALINA_HOME}/bin/hermes-config.sh"
    . "${CATALINA_BASE:-$CATALINA_HOME}/bin/hermes-tls.sh"
    . "${CATALINA_BASE:-$CATALINA_HOME}/bin/admin-credentials.sh"
fi
export CATALINA_OPTS
