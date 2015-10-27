#!/bin/sh

rm idemix*

echo "Starting apacheds default"
sudo /etc/init.d/apacheds-2.0.0-M17-default start

echo "Building components"
cd ../components
mvn clean install -DskipTests
cd ../services

echo "Building Services (profile)"
mvn clean
mvn -P ldap-ui-service install -DskipTests
~/web/web2/bin/shutdown.sh
rm -rf ~/web/web2/webapps/zhaw-p2abc-webservices
rm -f ~/web/web2/webapps/*.war
cp -f ./target/zhaw-p2abc-webservices.war ~/web/web2/webapps/zhaw-p2abc-webservices.war
cp -rf ./target/zhaw-p2abc-webservices ~/web/web2/webapps/
~/web/web2/bin/startup.sh
