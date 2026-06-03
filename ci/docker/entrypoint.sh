#!/bin/bash

java -version
echo "================================"
echo -e "JVM runtime arguments:\n${RUN_ARGS}"
echo "================================"
echo -e "CAS properties:\n${CAS_PROPERTIES}"
echo "================================"
echo "Launching CAS server in Docker container..."
echo "================================"
exec java ${RUN_ARGS} \
  -Dlog.console.stacktraces=true \
  -jar cas.war \
  --server.port=${SERVER_PORT} \
  --spring.profiles.active=none \
  --cas.audit.slf4j.use-single-line=true \
  ${CAS_PROPERTIES}
