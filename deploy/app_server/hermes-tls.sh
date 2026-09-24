#!/bin/sh
# Sourced from setenv.sh on every start: makes sure the HTTPS connector has
# a certificate, and lets the JVM trust it for its own loopback calls.
#
# The certificate lives in $TLS_DIR on the repository volume:
#   server.p12       PKCS#12 key store (alias "hermes"), read by server.xml
#   .password        its password (random, generated once)
# Replace it from the admin console (Main > HTTPS Certificate), or by
# putting PEM files in place before a start:
#   APP_TLS_CERT_FILE / APP_TLS_KEY_FILE   certificate (+ chain) and key
# Without either, a self-signed certificate for APP_TLS_HOSTNAME (default
# localhost) is generated on first start.

TLS_DIR=/hermes_home/repository/tls
KEYSTORE=$TLS_DIR/server.p12
PASSWORD_FILE=$TLS_DIR/.password
TRUSTSTORE=$CATALINA_BASE/conf/truststore.p12

mkdir -p "$TLS_DIR"
chmod 700 "$TLS_DIR"
if [ ! -s "$PASSWORD_FILE" ]; then
    (umask 077; head -c 24 /dev/urandom | base64 | tr -d '/+=\n' > "$PASSWORD_FILE")
fi
password=$(cat "$PASSWORD_FILE")

if [ -n "$APP_TLS_CERT_FILE" ] && [ -n "$APP_TLS_KEY_FILE" ]; then
    # PEM supplied at start: convert it into the key store
    if openssl pkcs12 -export -name hermes -in "$APP_TLS_CERT_FILE" -inkey "$APP_TLS_KEY_FILE" \
            -out "$KEYSTORE.new" -passout "file:$PASSWORD_FILE" 2>/dev/null; then
        mv "$KEYSTORE.new" "$KEYSTORE"
        echo "hermes-tls: installed the certificate from $APP_TLS_CERT_FILE" >&2
    else
        rm -f "$KEYSTORE.new"
        echo "hermes-tls: could not read $APP_TLS_CERT_FILE / $APP_TLS_KEY_FILE; keeping the current certificate" >&2
    fi
fi

if [ ! -s "$KEYSTORE" ]; then
    host=${APP_TLS_HOSTNAME:-localhost}
    keytool -genkeypair -alias hermes -keyalg RSA -keysize 2048 -validity 825 \
        -storetype PKCS12 -keystore "$KEYSTORE" -storepass "$password" \
        -dname "CN=$host, OU=Hermes, O=Hermes2, C=TH" \
        -ext "SAN=dns:$host,dns:localhost,ip:127.0.0.1" >/dev/null 2>&1
    echo "hermes-tls: generated a self-signed certificate for $host" >&2
fi
chmod 600 "$KEYSTORE"

# the JVM's default trust store plus this certificate (so Hermes can call
# itself over HTTPS, e.g. loopback tests, even when it is self-signed)
cp "$JAVA_HOME/lib/security/cacerts" "$TRUSTSTORE.new"
keytool -exportcert -alias hermes -keystore "$KEYSTORE" -storepass "$password" -rfc 2>/dev/null \
    | keytool -importcert -noprompt -alias hermes-tls -keystore "$TRUSTSTORE.new" \
        -storepass changeit >/dev/null 2>&1
mv "$TRUSTSTORE.new" "$TRUSTSTORE"
unset password

CATALINA_OPTS="$CATALINA_OPTS -Dhermes.tls.keystore=$KEYSTORE -Dhermes.tls.passwordFile=$PASSWORD_FILE"
CATALINA_OPTS="$CATALINA_OPTS -Djavax.net.ssl.trustStore=$TRUSTSTORE -Djavax.net.ssl.trustStorePassword=changeit"
