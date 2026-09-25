#!/bin/sh
# Build time: fills the @token@ placeholders of the Hermes plugin
# descriptors in $1. Everything that depends on the database - its type
# (MySQL, PostgreSQL, Oracle), where it is and the account used - is left
# as a __HERMES_DB_*__ placeholder that hermes-config.sh fills at each
# start, so one image serves every database and holds no credential.
set -eu
dir=$1

# each Hermes database: token prefix, database name, DAO descriptor key
set_db() {
    files=$1 prefix=$2 db=$3 dao=$4
    find $files -name '*.xml' -exec sed -i \
        -e "s/@${prefix}DriverClass@/__HERMES_DB_DRIVER__/g" \
        -e "s/@${prefix}ConnStr@/__HERMES_DB_URL_${db}__/g" \
        -e "s/@${prefix}user@/__HERMES_DB_USER_${db}__/g" \
        -e "s/@${prefix}pw@/__HERMES_DB_PASSWORD__/g" \
        -e "s/@${prefix}ValidationQuery@/__HERMES_DB_VALIDATION__/g" \
        -e "s#@${prefix}DAOFile@#__HERMES_DAO_${dao}__#g" {} \;
}

# as2plus shares as2's tokens but has its own database: do it first, scoped
# to its plugin, before the global as2 substitution
set_db "$dir/plugins/corvus-as2plus" as2 as2plus AS2

find "$dir" -name '*.xml' -exec sed -i \
    -e 's#@h2\.home@#/hermes_home#g' \
    -e 's/@as2PageletAdaptor@/__HERMES_AS2_PAGELET_ADAPTOR__/g' \
    -e 's/@ebmsPageletAdaptor@/__HERMES_EBMS_PAGELET_ADAPTOR__/g' {} \;

set_db "$dir" as2 as2 AS2
set_db "$dir" ebms ebms EBMS
set_db "$dir" sfrm sfrm SFRM
set_db "$dir" api apikeys API
set_db "$dir" listenerPorts listenerports ADMIN
