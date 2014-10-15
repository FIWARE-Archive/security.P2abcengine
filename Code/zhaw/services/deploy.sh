#!/bin/sh

mvn clean
mvn -P ldap-ui-service install
~/web/web1/bin/shutdown.sh
rm -rf ~/web/web1/webapps/zhaw-p2abc-webservices
rm -f ~/web/web1/webapps/*.war
cp -f ./target/zhaw-p2abc-webservices.war ~/web/web1/webapps/zhaw-p2abc-webservices.war
cp -rf ./target/zhaw-p2abc-webservices ~/web/web1/webapps/
cp -f ./src-web/ldapui.html ~/web/web1/webapps/ROOT/ldapui.html
cp -f ldapServiceConfig.xml /etc/abc4trust/ldapServiceConfig.xml
~/web/web1/bin/startup.sh
