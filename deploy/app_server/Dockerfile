# Hermes application server: Apache Tomcat 9 on Java 21 (Ubuntu 24.04).
# Built from the repository root (docker-compose: context "..").

# ---- build ----------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY . /build
RUN mvn -B -q pre-clean \
 && mvn -B -q package -Dmaven.test.skip=true
# JDBC drivers: MySQL 8.x, PostgreSQL, Oracle (HERMES_DB_TYPE picks one at start)
RUN for a in com.mysql:mysql-connector-j:8.4.0 org.postgresql:postgresql:42.7.4 \
             com.oracle.database.jdbc:ojdbc11:23.6.0.24.10; do \
        mvn -B -q dependency:copy -Dartifact=$a -DoutputDirectory=/build/jdbc || exit 1; \
    done
RUN mkdir -p /hermes \
 && cd /hermes && jar xf /build/target/hermes2_bin.zip \
 && rm -rf plugins/corvus-as2-admin

COPY deploy/app_server/configure-plugins.sh /usr/local/bin/configure-plugins.sh
RUN sh /usr/local/bin/configure-plugins.sh /hermes

# ---- runtime --------------------------------------------------------------
FROM tomcat:9.0-jre21-temurin-noble
LABEL maintainer="patrick.yee@gmail.com"

# Log timestamps and message history dates follow the deployment's local
# time; deploy/db uses the same zone so MySQL's CURRENT_TIMESTAMP and the
# JVM's formatting agree.
ENV TZ=Asia/Bangkok
RUN apt-get update \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends tzdata openssl \
 && rm -rf /var/lib/apt/lists/* \
 && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Only the Hermes web application
RUN rm -rf $CATALINA_HOME/webapps/* $CATALINA_HOME/webapps.dist
COPY --from=build /hermes/webapps/corvus $CATALINA_HOME/webapps/corvus
COPY --from=build /build/jdbc/*.jar $CATALINA_HOME/webapps/corvus/WEB-INF/lib/

# Plugins are installed from this pristine copy at every start, with the
# database settings filled in (hermes-config.sh)
COPY --from=build /hermes/plugins /opt/hermes/plugins
COPY --from=build /hermes/sample /hermes_home/sample
COPY --from=build /hermes/sql /hermes_home/sql
RUN mkdir -p /hermes_home/logs /hermes_home/repository

COPY deploy/app_server/server.xml $CATALINA_HOME/conf/server.xml
COPY deploy/app_server/tomcat-users.xml $CATALINA_HOME/conf/tomcat-users.xml
COPY --chmod=750 deploy/app_server/setenv.sh deploy/app_server/hermes-config.sh \
     deploy/app_server/hermes-tls.sh deploy/app_server/admin-credentials.sh $CATALINA_HOME/bin/

EXPOSE 8080 8443
CMD ["catalina.sh", "run"]
