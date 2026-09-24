#!/bin/sh
# Build time: fills the @token@ placeholders of the Hermes plugin
# descriptors in $1 for a MySQL deployment. Where the database lives and
# the account used are left as __HERMES_DB_HOST__ / __HERMES_DB_USER__ /
# __HERMES_DB_PASSWORD__, filled at each start by hermes-config.sh, so no
# credential is baked into the image.
set -eu
dir=$1

# JDBC URLs are XML attribute values: "&" must be "&amp;" (and escaped for sed)
jdbc() {
    echo "jdbc:mysql:\/\/__HERMES_DB_HOST__\/$1?useSSL=false\&amp;allowPublicKeyRetrieval=true"
}

# each database: token prefix, database name, DAO descriptor
set_db() {
    files=$1 prefix=$2 db=$3 dao=$4
    find $files -name '*.xml' -exec sed -i \
        -e "s/@${prefix}DriverClass@/com.mysql.cj.jdbc.Driver/g" \
        -e "s/@${prefix}ConnStr@/$(jdbc "$db")/g" \
        -e "s/@${prefix}user@/__HERMES_DB_USER__/g" \
        -e "s/@${prefix}pw@/__HERMES_DB_PASSWORD__/g" \
        -e "s/@${prefix}ValidationQuery@/SELECT now()/g" \
        -e "s#@${prefix}DAOFile@#${dao}#g" {} \;
}

# as2plus shares as2's tokens but has its own database: do it first, scoped
# to its plugin, before the global as2 substitution
set_db "$dir/plugins/corvus-as2plus" as2 as2plus hk/hku/cecid/edi/as2/conf/as2.dao.xml

find "$dir" -name '*.xml' -exec sed -i \
    -e 's#@h2\.home@#/hermes_home#g' \
    -e 's/@as2PageletAdaptor@/hk.hku.cecid.edi.as2.admin.listener.MessageHistoryPageletAdaptor/g' \
    -e 's/@ebmsPageletAdaptor@/hk.hku.cecid.ebms.admin.listener.MessageHistoryPageletAdaptor/g' {} \;

set_db "$dir" as2 as2 hk/hku/cecid/edi/as2/conf/as2.dao.xml
set_db "$dir" ebms ebms hk/hku/cecid/ebms/spa/conf/ebms.mysql.dao.xml
set_db "$dir" sfrm sfrm sfrm.dao.xml
set_db "$dir" api apikeys hk/hku/cecid/hermes/api/conf/api.dao.xml
set_db "$dir" listenerPorts listenerports hk/hku/cecid/piazza/corvus/core/main/admin/conf/admin.dao.xml
