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

INFLUXDB_VERSION="1.6"
FLUX_VERSION="nightly"

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

mvn clean install

docker kill influxdb || true
docker kill flux || true
