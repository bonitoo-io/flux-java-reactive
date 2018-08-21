#!/usr/bin/env bash
#
# The MIT License
# Copyright © 2018
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.
#

#
# script to start influxdb and compile influxdb-java with all tests.
#
set -e

DEFAULT_INFLUXDB_VERSION="1.6"
DEFAULT_MAVEN_JAVA_VERSION="3-jdk-8-slim"
DEFAULT_FLUX_VERSION="nightly"
DEFAULT_RUN_NIGHTLY_BINARY="false"

INFLUXDB_VERSION="${INFLUXDB_VERSION:-$DEFAULT_INFLUXDB_VERSION}"
MAVEN_JAVA_VERSION="${MAVEN_JAVA_VERSION:-$DEFAULT_MAVEN_JAVA_VERSION}"
FLUX_VERSION="${FLUX_VERSION:-$DEFAULT_FLUX_VERSION}"
RUN_NIGHTLY_BINARY="${RUN_NIGHTLY_BINARY:-$DEFAULT_RUN_NIGHTLY_BINARY}"

if [ ! "$RUN_NIGHTLY_BINARY" == "true" ]; then

    echo "Run tests on InfluxDB-${INFLUXDB_VERSION} with Flux-${FLUX_VERSION}"

    docker kill influxdb || true
    docker rm influxdb || true
    docker kill flux || true
    docker rm flux || true
    docker network remove influxdb || true

    #
    # Create network
    #
    docker network create influxdb
    INFLUXDB_IP=influxdb
    FLUX_IP=flux
    DOCKER_NET=influxdb

    #
    # InfluxDB
    #
    docker pull influxdb:${INFLUXDB_VERSION}-alpine || true
    docker run \
              --detach \
              --name influxdb \
              --net=influxdb \
              --publish 8086:8086 \
              --publish 8082:8082 \
              --publish 8089:8089/udp \
              --volume ${PWD}/config/influxdb.conf:/etc/influxdb/influxdb.conf \
          influxdb:${INFLUXDB_VERSION}-alpine

    #
    # Flux
    #
    docker pull quay.io/influxdb/flux:${FLUX_VERSION}

    # wait for InfluxDB
    sleep 3
    docker run --detach --net=influxdb --name flux --publish 8093:8093 quay.io/influxdb/flux:${FLUX_VERSION}
fi

if [ "$RUN_NIGHTLY_BINARY" == "true" ]; then

    echo "Run tests on InfluxDB nightly binary with Flux nightly binary"

    rm -rf ./influxdb-*
    rm -rf ./fluxd_nightly_*

    case "$OSTYPE" in
      darwin*)
        archive='darwin_amd64';
        conf='influxdb_mac';
         ;;
      linux*)
        archive="linux_amd64";
        conf='influxdb_travis';
    esac

    wget https://dl.influxdata.com/influxdb/nightlies/influxdb-nightly_${archive}.tar.gz -O influxdb-nightly.tar.gz
    tar zxvf influxdb-nightly.tar.gz
    mv `find . -name influxdb-1.7.*` influxdb-nightly

    wget https://dl.influxdata.com/flux/nightlies/fluxd_nightly_${archive}.tar.gz -O fluxd_nightly.tar.gz
    tar zxvf fluxd_nightly.tar.gz
    mv `find . -name fluxd_nightly_*` fluxd_nightly

    killall influxd || true
    killall fluxd || true

    ./influxdb-nightly/usr/bin/influxd -config ./config/${conf}.conf &>./influxdb-nightly.log &

    # Wait for start InfluxDB
    echo "Wait 5s to start InfluxDB"
    sleep 5

    ./fluxd_nightly/fluxd  &>./fluxd_nightly.log &

    # Wait for start Flux
    echo "Wait 5s to start Flux"
    sleep 5

    ifconfig | sed -En 's/127.0.0.1//;s/.*inet (addr:)?(([0-9]*\.){3}[0-9]*).*/\2/p'
    INFLUXDB_IP=`ifconfig | sed -En 's/127.0.0.1//;s/.*inet (addr:)?(([0-9]*\.){3}[0-9]*).*/\2/p' | grep 10`
    FLUX_IP=${INFLUXDB_IP}
    DOCKER_NET=host
    ps ux
fi

echo "INFLUXDB_IP: " ${INFLUXDB_IP} " FLUX_IP: " ${FLUX_IP}

docker run -it --rm \
       --volume ${PWD}:/usr/src/mymaven \
       --volume ${PWD}/.m2:/root/.m2 \
       --workdir /usr/src/mymaven \
       --net=${DOCKER_NET} \
       --env INFLUXDB_VERSION=${INFLUXDB_VERSION} \
       --env INFLUXDB_IP=${INFLUXDB_IP} \
       --env FLUX_IP=${FLUX_IP} \
       maven:${MAVEN_JAVA_VERSION} mvn clean install -U

docker kill influxdb || true
docker kill flux || true
