#!/bin/zsh

ERR_USER=1;
ERR_DEPENDENCIES=2;
ERR_BUILD=3;
ERR_UNKNOWN=256;

RUNNABLE="./target/prometheus-http-sd-runner";

[ "$(id -u)" != 0 ] && echo "Script must be executed as root or a super user."; && exit $ERR_USER;

[ -e "$(which mvn)" ] || echo "Dependency not found: 'mvn':"; exit $ERR_DEPENDENCIES;

if [ ! -e "$RUNNABLE" ];
then
  mvn -P native package;
  [ $? -ne 0 ] || [ ! -e "$RUNNABLE" ] && echo "Error building prometheus-http-sd server."; && exit $ERR_BUILD;
fi;

[ ! -e "/opt/bin" ] && mkdir -p "/opt/bin";
ln -s "$RUNNABLE" /opt/bin/prometheus-http-sd;