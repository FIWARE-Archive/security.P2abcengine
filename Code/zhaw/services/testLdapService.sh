#!/bin/sh
curl -X GET 'http://localhost:8888/zhaw-p2abc-webservices/ldap-issuance-service/attributeInfoCollection/*magic*/account' > in.xml
curl -X POST --header 'Content-Type: application/xml' -d @in.xml 'http://localhost:8888/zhaw-p2abc-webservices/ldap-issuance-service/genCredSpec/*magic*/' > credSpec.xml
curl -X PUT --header 'Content-Type: application/xml' -d @credSpec.xml 'http://localhost:8888/zhaw-p2abc-webservices/ldap-issuance-service/storeCredentialSpecification/*magic*/foo'
curl -X GET 'http://localhost:8888/zhaw-p2abc-webservices/ldap-issuance-service/getCredentialSpecification/*magic*/foo' > credSpecGet.xml
#curl -X POST --header 'Content-Type: application/xml' -d @credSpec.xml 'http://localhost:8888/zhaw-p2abc-webservices/genIssuanceAttributes' > outAttribs.xml
curl -X POST --header 'Content-Type: application/xml' -d @ldapSimpleAuth.xml 'http://localhost:8888/zhaw-p2abc-webservices/ldap-issuance-service/testAuthentication'
curl -X PUT --header 'Content-Type: application/xml' -d @queryRule.xml 'http://localhost:8888/zhaw-p2abc-webservices/ldap-issuance-service/storeQueryRule/*magic*/foo'
curl -X GET 'http://localhost:8888/zhaw-p2abc-webservices/ldap-issuance-service/getQueryRule/*magic*/foo' > outQueryRule.xml
