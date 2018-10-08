#!/bin/bash

set -x
set -e

###################################################################################################################
# launch docker server
###################################################################################################################

mysql=( mysql --protocol=tcp -uperf -h127.0.0.1 --port=3305 -p'!Password0')
export COMPOSE_FILE=.travis/docker-compose.yml
docker-compose -f ${COMPOSE_FILE} up -d

###################################################################################################################
# wait for docker initialisation
###################################################################################################################

for i in {60..0}; do
    if echo 'SELECT 1' | "${mysql[@]}" &> /dev/null; then
        break
    fi
    echo 'data server still not active'
    sleep 1
done


if [ "$i" = 0 ]; then
    if [ -n "COMPOSE_FILE" ] ; then
        docker-compose -f ${COMPOSE_FILE} logs
    fi

    echo 'SELECT 1' | "${mysql[@]}"
    echo >&2 'data server init process failed.'
    exit 1
fi



###################################################################################################################
# run test suite
###################################################################################################################

echo "Running coveralls for JDK version: $TRAVIS_JDK_VERSION"

mvn clean install
java -jar target/benchmarks.jar -Dport=3305 -Dhost=mariadb.example.com
