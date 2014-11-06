#!/bin/sh
cp -f servicesConfiguration.xml /tmp/servicesConfiguration.xml
cd ../services
./deploy.sh
cd ../integration-tests
mvn clean install
mvn exec:java -Dexec.mainClass="Test"
