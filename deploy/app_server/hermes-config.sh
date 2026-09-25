#!/bin/sh
# Sourced from setenv.sh on every start (as root, before the JVM):
# installs the plugins from the image's pristine copy and fills in the
# database settings from the environment:
#
#   HERMES_DB_TYPE      mysql (default), postgres or oracle (23ai or later)
#   HERMES_DB_HOST      host (default: db)
#   HERMES_DB_PORT      port (default: the database's usual one)
#   HERMES_DB_USER      account, MySQL/PostgreSQL (default: hermes)
#   HERMES_DB_PASSWORD  its password (required)
#   HERMES_DB_SERVICE   Oracle service name (default: FREEPDB1)
#   HERMES_ORACLE_SCHEMA_PREFIX  Oracle: each Hermes database is a schema
#                       named <prefix><database> (default: hermes_), e.g.
#                       hermes_ebms, all with HERMES_DB_PASSWORD
#
# Starting from the pristine copy each time means changed settings take
# effect with a restart; nothing secret is stored in the image.

PLUGINS_TEMPLATE=/opt/hermes/plugins
PLUGINS=/hermes_home/plugins

type=${HERMES_DB_TYPE:-mysql}
host=${HERMES_DB_HOST:-db}
[ -z "$HERMES_DB_PASSWORD" ] && echo "hermes-config: HERMES_DB_PASSWORD is not set (see deploy/setup.sh)" >&2

EBMS_CONF=hk/hku/cecid/ebms/spa/conf
AS2_CONF=hk/hku/cecid/edi/as2/conf
ADMIN_CONF=hk/hku/cecid/piazza/corvus/core/main/admin/conf
DAO_API=hk/hku/cecid/hermes/api/conf/api.dao.xml
EBMS_PAGELET=hk.hku.cecid.ebms.admin.listener.MessageHistoryPageletAdaptor
AS2_PAGELET=hk.hku.cecid.edi.as2.admin.listener.MessageHistoryPageletAdaptor

case "$type" in
    mysql)
        driver=com.mysql.cj.jdbc.Driver
        url_template="jdbc:mysql://$host${HERMES_DB_PORT:+:$HERMES_DB_PORT}/%DB%?useSSL=false&allowPublicKeyRetrieval=true"
        validation="SELECT 1"
        DAO_EBMS=$EBMS_CONF/ebms.mysql.dao.xml DAO_AS2=$AS2_CONF/as2.dao.xml
        DAO_SFRM=sfrm.dao.xml DAO_ADMIN=$ADMIN_CONF/admin.dao.xml ;;
    postgres|postgresql)
        driver=org.postgresql.Driver
        url_template="jdbc:postgresql://$host${HERMES_DB_PORT:+:$HERMES_DB_PORT}/%DB%"
        validation="SELECT 1"
        DAO_EBMS=$EBMS_CONF/ebms.dao.xml DAO_AS2=$AS2_CONF/as2.dao.xml
        DAO_SFRM=sfrm.dao.xml DAO_ADMIN=$ADMIN_CONF/admin.dao.xml ;;
    oracle)
        driver=oracle.jdbc.OracleDriver
        url_template="jdbc:oracle:thin:@//$host:${HERMES_DB_PORT:-1521}/${HERMES_DB_SERVICE:-FREEPDB1}"
        validation="SELECT 1 FROM DUAL"
        DAO_EBMS=$EBMS_CONF/ebms.oracle.dao.xml DAO_AS2=$AS2_CONF/as2.oracle.dao.xml
        DAO_SFRM=sfrm.oracle.dao.xml DAO_ADMIN=$ADMIN_CONF/admin.oracle.dao.xml
        EBMS_PAGELET=hk.hku.cecid.ebms.admin.listener.MessageHistoryOraclePageletAdaptor
        AS2_PAGELET=hk.hku.cecid.edi.as2.admin.listener.MessageHistoryOraclePageletAdaptor ;;
    *)
        echo "hermes-config: unknown HERMES_DB_TYPE '$type' (mysql, postgres or oracle)" >&2
        type=mysql driver=com.mysql.cj.jdbc.Driver validation="SELECT 1"
        url_template="jdbc:mysql://$host/%DB%?useSSL=false&allowPublicKeyRetrieval=true"
        DAO_EBMS=$EBMS_CONF/ebms.mysql.dao.xml DAO_AS2=$AS2_CONF/as2.dao.xml
        DAO_SFRM=sfrm.dao.xml DAO_ADMIN=$ADMIN_CONF/admin.dao.xml ;;
esac

rm -rf "$PLUGINS"
cp -a "$PLUGINS_TEMPLATE" "$PLUGINS"

# perl reads every value from the environment and XML-escapes it, so no
# character in a password or URL needs shell/sed escaping
HC_TYPE=$type HC_DRIVER=$driver HC_URL=$url_template HC_VALIDATION=$validation
HC_USER=${HERMES_DB_USER:-hermes} HC_PREFIX=${HERMES_ORACLE_SCHEMA_PREFIX:-hermes_}
HC_PASSWORD=$HERMES_DB_PASSWORD HC_EBMS_PAGELET=$EBMS_PAGELET HC_AS2_PAGELET=$AS2_PAGELET
HC_DAO_EBMS=$DAO_EBMS HC_DAO_AS2=$DAO_AS2 HC_DAO_SFRM=$DAO_SFRM HC_DAO_API=$DAO_API HC_DAO_ADMIN=$DAO_ADMIN
# exported: perl runs at the end of the pipeline, not as its first command
export HC_TYPE HC_DRIVER HC_URL HC_VALIDATION HC_USER HC_PREFIX HC_PASSWORD HC_EBMS_PAGELET \
    HC_AS2_PAGELET HC_DAO_EBMS HC_DAO_AS2 HC_DAO_SFRM HC_DAO_API HC_DAO_ADMIN
grep -rl --include='*.xml' '__HERMES_' "$PLUGINS" | xargs -r perl -pi -e '
    sub x { my $v = shift; $v =~ s/&/&amp;/g; $v =~ s/</&lt;/g; $v =~ s/>/&gt;/g; $v =~ s/"/&quot;/g; $v }
    s{__HERMES_DB_URL_(\w+)__}{ my ($db, $u) = ($1, $ENV{HC_URL}); $u =~ s/%DB%/$db/; x($u) }ge;
    s{__HERMES_DB_USER_(\w+)__}{ my $db = $1; x($ENV{HC_TYPE} eq "oracle" ? $ENV{HC_PREFIX}.$db : $ENV{HC_USER}) }ge;
    s{__HERMES_DB_PASSWORD__}{x($ENV{HC_PASSWORD})}ge;
    s{__HERMES_DB_DRIVER__}{x($ENV{HC_DRIVER})}ge;
    s{__HERMES_DB_VALIDATION__}{x($ENV{HC_VALIDATION})}ge;
    s{__HERMES_DAO_(\w+)__}{x($ENV{"HC_DAO_".$1})}ge;
    s{__HERMES_EBMS_PAGELET_ADAPTOR__}{x($ENV{HC_EBMS_PAGELET})}ge;
    s{__HERMES_AS2_PAGELET_ADAPTOR__}{x($ENV{HC_AS2_PAGELET})}ge;'
unset HC_TYPE HC_DRIVER HC_URL HC_VALIDATION HC_USER HC_PREFIX HC_PASSWORD HC_EBMS_PAGELET \
    HC_AS2_PAGELET HC_DAO_EBMS HC_DAO_AS2 HC_DAO_SFRM HC_DAO_API HC_DAO_ADMIN
chmod -R go-rwx "$PLUGINS"
if grep -rq --include='*.xml' -e '__HERMES_' -e 'name="driver" value=""' "$PLUGINS"; then
    echo "hermes-config: ERROR: plugin database settings left unfilled in $PLUGINS" >&2
fi
echo "hermes-config: plugins configured for $type at $host" >&2
