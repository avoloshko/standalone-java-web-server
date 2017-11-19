#!/bin/sh

set -e

gradle clean build

tar -xzf build/distributions/webapp.tar webapp

export JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"

sh webapp/bin/webapp