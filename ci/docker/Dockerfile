ARG JAVA_VERSION=25.0.3
FROM azul/zulu-openjdk:${JAVA_VERSION}

ARG CAS_WAR

ENV RUN_ARGS=""
ENV CAS_PROPERTIES=""
ENV SERVER_PORT="8443"

RUN cd / \
    && mkdir -p /etc/cas/config \
    && mkdir -p /etc/cas/services \
    && mkdir -p cas-server

COPY ${CAS_WAR} cas-server/cas.war
COPY ci/docker/entrypoint.sh cas-server/
RUN chmod +x /cas-server/entrypoint.sh

EXPOSE 8080 $SERVER_PORT 5701

ENV PATH=$PATH:$JAVA_HOME/bin:.
WORKDIR cas-server
ENTRYPOINT ["/cas-server/entrypoint.sh"]
