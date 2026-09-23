FROM mysql:5.7
MAINTAINER patrick.yee@gmail.com

RUN mkdir -p /build
COPY h2o-installer/sql/mysql_ebms.sql /build/ebms.sql
COPY h2o-installer/sql/mysql_as2.sql /build/as2.sql
COPY h2o-installer/sql/mysql_as2plus.sql /build/as2plus.sql
COPY h2o-installer/sql/mysql_sfrm.sql /build/sfrm.sql
COPY h2o-installer/sql/mysql_apikeys.sql /build/apikeys.sql
COPY deploy/db/load.sql /docker-entrypoint-initdb.d/load.sql
