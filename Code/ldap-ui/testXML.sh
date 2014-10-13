#!/bin/sh
curl -X GET 'http://localhost:8888/ldap-ui-service/schemaDump?oc=abcPerson' > in.xml
curl -X POST --header 'Content-Type: application/xml' -d @in.xml 'http://localhost:8888/ldap-ui-service/genCredSpec' > out.xml
