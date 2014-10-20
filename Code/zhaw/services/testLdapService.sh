#!/bin/sh
curl -X GET 'http://localhost:8888/zhaw-p2abc-webservices/ldap-issuance-service/attributeInfoCollection/*magic*/account' > in.xml
curl -X POST --header 'Content-Type: application/xml' -d @in.xml 'http://localhost:8888/zhaw-p2abc-webservices/ldap-issuance-service/genCredSpec/*magic*/' > credSpec.xml
#curl -X POST --header 'Content-Type: application/xml' -d @credSpec.xml 'http://localhost:8888/zhaw-p2abc-webservices/genIssuanceAttributes' > outAttribs.xml
curl -X POST --header 'Content-Type: application/xml' -d @ldapSimpleAuth.xml 'http://localhost:8888/zhaw-p2abc-webservices/ldap-issuance-service/testAuthentication'
