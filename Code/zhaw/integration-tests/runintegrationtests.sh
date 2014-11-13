#!/bin/sh
cd ../services
./deploy.sh
cd ../integration-tests
mvn clean install
mvn exec:java -Dexec.mainClass="Test"
