FROM ubuntu:16.04
MAINTAINER patrick.yee@gmail.com

# Update System
RUN apt-get update && apt-get install -y --no-install-recommends \
	openjdk-8-jdk-headless \
        tomcat8 \
	maven \
	unzip \
	libmysql-java
ENV JAVA_HOME /usr/lib/jvm/java-1.8.0-openjdk-amd64
RUN echo "export JAVA_HOME=$JAVA_HOME" >> /etc/profile

# Build Hermes
RUN mkdir -p /build
COPY . /build
RUN cd /build && \
	mvn pre-clean && \
	mvn compile && \
	mvn package -Dmaven.test.skip=true

# Set up Tomcat
COPY deploy/app_server/tomcat-users.xml /etc/tomcat8/tomcat-users.xml
RUN chown root:tomcat8 /etc/tomcat8/tomcat-users.xml
RUN chmod 640 /etc/tomcat8/tomcat-users.xml

# Set up HTTPS: generate a self-signed keystore for the Tomcat connector, and
# also trust that same cert in the JVM's own truststore so Hermes' outbound
# HTTP client (used for AS2/EbMS delivery, e.g. in loopback testing) accepts
# the self-signed certificate on outbound HTTPS requests back to itself.
RUN mkdir -p /etc/tomcat8/ssl && \
	keytool -genkeypair -alias corvus -keyalg RSA -keysize 2048 -validity 3650 \
		-keystore /etc/tomcat8/ssl/keystore.jks -storepass corvusssl -keypass corvusssl \
		-dname "CN=localhost, OU=Corvus, O=Hermes2, L=HK, ST=HK, C=HK" \
		-ext "SAN=dns:localhost,ip:127.0.0.1" && \
	keytool -exportcert -alias corvus -keystore /etc/tomcat8/ssl/keystore.jks -storepass corvusssl \
		-file /etc/tomcat8/ssl/corvus.cer && \
	keytool -importcert -noprompt -alias corvus \
		-keystore "$JAVA_HOME/jre/lib/security/cacerts" -storepass changeit \
		-file /etc/tomcat8/ssl/corvus.cer && \
	chown -R tomcat8:tomcat8 /etc/tomcat8/ssl && \
	chmod 640 /etc/tomcat8/ssl/keystore.jks

COPY deploy/app_server/server.xml /var/lib/tomcat8/conf/server.xml
RUN chown tomcat8:tomcat8 /var/lib/tomcat8/conf/server.xml

# server.xml's Connector ports are ${catalina.http.port}/${catalina.https.port}
# system properties, not hardcoded numbers, so the listening ports can be
# changed per-container via APP_HTTP_PORT/APP_HTTPS_PORT without an image
# rebuild. catalina.sh sources bin/setenv.sh automatically if present.
COPY deploy/app_server/setenv.sh /var/lib/tomcat8/bin/setenv.sh
RUN chown tomcat8:tomcat8 /var/lib/tomcat8/bin/setenv.sh && \
	chmod 750 /var/lib/tomcat8/bin/setenv.sh

# Tomcat's own scratch/temp directory (used e.g. by commons-fileupload to
# buffer multipart uploads to disk) isn't created by this stripped-down
# package install.
RUN mkdir -p /var/lib/tomcat8/temp && chown tomcat8:tomcat8 /var/lib/tomcat8/temp

# Deploy Hermes
RUN rm -rf /hermes_home
RUN mkdir -p /hermes_home/logs
RUN mkdir -p /tmp/h
RUN cd /tmp/h && unzip /build/target/hermes2_bin.zip
RUN rm -rf /tmp/h/plugins/corvus-as2-admin

# corvus-as2plus shares the same @as2...@ placeholder tokens as corvus-as2, but
# needs its own database (its DAO schema differs from as2's). Substitute its
# tokens first, scoped to its own plugin directory, before the global as2
# substitution below runs on everything else.
RUN find /tmp/h/plugins/corvus-as2plus -name *.xml -exec sed -i 's/@as2DriverClass@/com\.mysql\.jdbc\.Driver/g' {} \;
RUN find /tmp/h/plugins/corvus-as2plus -name *.xml -exec sed -i 's/@as2ConnStr@/jdbc:mysql:\/\/db\/as2plus?useSSL=false/g' {} \;
RUN find /tmp/h/plugins/corvus-as2plus -name *.xml -exec sed -i 's/@as2user@/corvus/g' {} \;
RUN find /tmp/h/plugins/corvus-as2plus -name *.xml -exec sed -i 's/@as2pw@/corvus/g' {} \;
RUN find /tmp/h/plugins/corvus-as2plus -name *.xml -exec sed -i 's/@as2ValidationQuery@/SELECT now\(\)/g' {} \;
RUN find /tmp/h/plugins/corvus-as2plus -name *.xml -exec sed -i 's/@as2DAOFile@/hk\/hku\/cecid\/edi\/as2\/conf\/as2.dao.xml/g' {} \;

RUN find /tmp/h -name *.xml -exec sed -i 's/@h2\.home@/\/hermes_home/g' {} \;
RUN find /tmp/h -name *.xml -exec sed -i 's/@as2PageletAdaptor@/hk\.hku\.cecid\.edi\.as2\.admin\.listener\.MessageHistoryPageletAdaptor/g' {} \;
RUN find /tmp/h -name *.xml -exec sed -i 's/@as2DriverClass@/com\.mysql\.jdbc\.Driver/g' {} \;
RUN find /tmp/h -name *.xml -exec sed -i 's/@as2ConnStr@/jdbc:mysql:\/\/db\/as2?useSSL=false/g' {} \;
RUN find /tmp/h -name *.xml -exec sed -i 's/@as2user@/corvus/g' {} \;
RUN find /tmp/h -name *.xml -exec sed -i 's/@as2pw@/corvus/g' {} \;
RUN find /tmp/h -name *.xml -exec sed -i 's/@as2ValidationQuery@/SELECT now\(\)/g' {} \;
RUN find /tmp/h -name *.xml -exec sed -i 's/@as2DAOFile@/hk\/hku\/cecid\/edi\/as2\/conf\/as2.dao.xml/g' {} \;
RUN find /tmp/h -name *.xml -exec sed -i 's/@ebmsPageletAdaptor@/hk\.hku\.cecid\.ebms\.admin\.listener\.MessageHistoryPageletAdaptor/g' {} \;
RUN find /tmp/h -name *.xml -exec sed -i 's/@ebmsDriverClass@/com\.mysql\.jdbc\.Driver/g' {} \;
RUN find /tmp/h -name *.xml -exec sed -i 's/@ebmsConnStr@/jdbc:mysql:\/\/db\/ebms?useSSL=false/g' {} \;
RUN find /tmp/h -name *.xml -exec sed -i 's/@ebmsuser@/corvus/g' {} \;
RUN find /tmp/h -name *.xml -exec sed -i 's/@ebmspw@/corvus/g' {} \;
RUN find /tmp/h -name *.xml -exec sed -i 's/@ebmsValidationQuery@/SELECT now\(\)/g' {} \;
RUN find /tmp/h -name *.xml -exec sed -i 's/@ebmsDAOFile@/hk\/hku\/cecid\/ebms\/spa\/conf\/ebms.mysql.dao.xml/g' {} \;
RUN find /tmp/h -name *.xml -exec sed -i 's/@sfrmDriverClass@/com\.mysql\.jdbc\.Driver/g' {} \;
RUN find /tmp/h -name *.xml -exec sed -i 's/@sfrmConnStr@/jdbc:mysql:\/\/db\/sfrm?useSSL=false/g' {} \;
RUN find /tmp/h -name *.xml -exec sed -i 's/@sfrmuser@/corvus/g' {} \;
RUN find /tmp/h -name *.xml -exec sed -i 's/@sfrmpw@/corvus/g' {} \;
RUN find /tmp/h -name *.xml -exec sed -i 's/@sfrmValidationQuery@/SELECT now\(\)/g' {} \;
RUN find /tmp/h -name *.xml -exec sed -i 's/@sfrmDAOFile@/sfrm.dao.xml/g' {} \;
RUN find /tmp/h -name *.xml -exec sed -i 's/@apiDriverClass@/com\.mysql\.jdbc\.Driver/g' {} \;
RUN find /tmp/h -name *.xml -exec sed -i 's/@apiConnStr@/jdbc:mysql:\/\/db\/apikeys?useSSL=false/g' {} \;
RUN find /tmp/h -name *.xml -exec sed -i 's/@apiuser@/corvus/g' {} \;
RUN find /tmp/h -name *.xml -exec sed -i 's/@apipw@/corvus/g' {} \;
RUN find /tmp/h -name *.xml -exec sed -i 's/@apiValidationQuery@/SELECT now\(\)/g' {} \;
RUN find /tmp/h -name *.xml -exec sed -i 's/@apiDAOFile@/hk\/hku\/cecid\/hermes\/api\/conf\/api.dao.xml/g' {} \;
RUN mv /tmp/h/plugins /hermes_home
RUN mv /tmp/h/sample /hermes_home
RUN mv /tmp/h/sql /hermes_home
RUN cp /usr/share/java/mysql.jar /tmp/h/webapps/corvus/WEB-INF/lib/
RUN rm -rf /var/lib/tomcat8/webapps/corvus
RUN mv /tmp/h/webapps/corvus /var/lib/tomcat8/webapps
RUN chown -R tomcat8:tomcat8 /var/lib/tomcat8/webapps/corvus
RUN chown -R tomcat8:tomcat8 /hermes_home
RUN rm -rf /tmp/h
RUN rm -rf /build

ENV CATALINA_BASE /var/lib/tomcat8
EXPOSE 8080 8443

# CMD service tomcat8 start && tail -f /var/log/tomcat8/catalina.out
CMD ["/usr/share/tomcat8/bin/catalina.sh", "run"]
