FROM mysql:8.4
LABEL maintainer="patrick.yee@gmail.com"

# Matches deploy/app_server/Dockerfile's TZ -- MySQL's own CURRENT_TIMESTAMP/
# NOW() follow the OS clock (time_zone defaults to 'SYSTEM'), so both sides
# need to agree or a timestamp written here and one formatted by the app's
# JVM would silently disagree by the zone offset.
ENV TZ=Asia/Bangkok
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone || true

RUN mkdir -p /build
COPY h2o-installer/sql/mysql_ebms.sql /build/ebms.sql
COPY h2o-installer/sql/mysql_as2.sql /build/as2.sql
COPY h2o-installer/sql/mysql_as2plus.sql /build/as2plus.sql
COPY h2o-installer/sql/mysql_sfrm.sql /build/sfrm.sql
COPY h2o-installer/sql/mysql_apikeys.sql /build/apikeys.sql
COPY h2o-installer/sql/mysql_listenerports.sql /build/listenerports.sql
COPY --chmod=755 deploy/db/hermes-init.sh /docker-entrypoint-initdb.d/10-hermes-init.sh
COPY h2o-installer/sql/upgrade/mysql_upgrade.sql /build/upgrade.sql
# run by the db-upgrade service (docker-compose) against an existing database
COPY --chmod=755 deploy/db/hermes-upgrade.sh /usr/local/bin/hermes-upgrade.sh
COPY --chmod=755 deploy/db/hermes-backup.sh /usr/local/bin/hermes-backup.sh
