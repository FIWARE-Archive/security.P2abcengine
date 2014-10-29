#!/bin/sh

echo 'Downloading AttributeInfoCollection'
curl -X GET 'http://localhost:8888/zhaw-p2abc-webservices/ldap-issuance-service/attributeInfoCollection/*magic*/person' > attrInfoCol.xml

echo 'Generating CredentialSpecification'
curl -X POST --header 'Content-Type: application/xml' -d @attrInfoCol.xml 'http://localhost:8888/zhaw-p2abc-webservices/ldap-issuance-service/genCredSpec/*magic*/' > credSpec.xml

# urn:abc4trust:credspec:ldap:abcPerson == urn%3Aabc4trust%3Acredspec%3Aldap%3AabcPerson

echo 'Storing CredentialSpecification at Issuer'

curl -X PUT --header 'Content-Type: application/xml' -d @credSpec.xml 'http://localhost:8888/zhaw-p2abc-webservices/ldap-issuance-service/storeCredentialSpecification/*magic*/urn%3Aabc4trust%3Acredspec%3Aldap%3Aperson'

echo 'Store QueryRule at Issuer'

curl -X PUT --header 'Content-Type: application/xml' -d @queryRule.xml 'http://localhost:8888/zhaw-p2abc-webservices/ldap-issuance-service/storeQueryRule/*magic*/urn%3Aabc4trust%3Acredspec%3Aldap%3Aperson'

echo 'Store IssuancePolicy at Issuer'

curl -X PUT --header 'Content-Type: application/xml' -d @issuancePolicy.xml 'http://localhost:8888/zhaw-p2abc-webservices/ldap-issuance-service/storeIssuancePolicy/*magic*/urn%3Aabc4trust%3Acredspec%3Aldap%3Aperson'

echo 'IssuanceRequest'

curl -X POST --header 'Content-Type: application/xml' -d @issuanceRequest.xml 'http://localhost:8888/zhaw-p2abc-webservices/ldap-issuance-service/issuanceRequest' > outIssuanceRequest.xml
