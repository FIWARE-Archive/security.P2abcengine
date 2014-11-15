#!/bin/sh

echo 'Downloading AttributeInfoCollection'
curl -X GET 'http://localhost:8888/zhaw-p2abc-webservices/issuance/attributeInfoCollection/*magic*/userdata' > ./gen/attrInfoCol.xml

echo 'Generating CredentialSpecification'
curl -X POST --header 'Content-Type: application/xml' -d @./gen/attrInfoCol.xml 'http://localhost:8888/zhaw-p2abc-webservices/issuance/genCredSpec/*magic*/' > ./gen/credSpec.xml

# urn:abc4trust:credspec:ldap:abcPerson == urn%3Aabc4trust%3Acredspec%3Aldap%3AabcPerson

echo 'Storing CredentialSpecification at Issuer'

curl -X PUT --header 'Content-Type: application/xml' -d @./gen/credSpec.xml 'http://localhost:8888/zhaw-p2abc-webservices/issuance/storeCredentialSpecification/*magic*/urn%3Afiware%3Aprivacy%3Acredspec%3Auserdata'

echo 'Store QueryRule at Issuer'

curl -X PUT --header 'Content-Type: application/xml' -d @queryRule.xml 'http://localhost:8888/zhaw-p2abc-webservices/issuance/storeQueryRule/*magic*/urn%3Afiware%3Aprivacy%3Acredspec%3Auserdata'

echo 'Store IssuancePolicy at Issuer'

curl -X PUT --header 'Content-Type: application/xml' -d @issuancePolicy.xml 'http://localhost:8888/zhaw-p2abc-webservices/issuance/storeIssuancePolicy/*magic*/urn%3Afiware%3Aprivacy%3Acredspec%3Auserdata'
