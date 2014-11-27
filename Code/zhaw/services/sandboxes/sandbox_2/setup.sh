#!/bin/sh

echo 'Downloading AttributeInfoCollection'
curl --user both:tomcat -X GET 'http://localhost:8888/zhaw-p2abc-webservices/issuance/protected/attributeInfoCollection/userdata' > ./gen/attrInfoCol.xml

echo 'Generating CredentialSpecification'
curl --user both:tomcat -X POST --header 'Content-Type: application/xml' -d @./gen/attrInfoCol.xml 'http://localhost:8888/zhaw-p2abc-webservices/issuance/protected/generateCredentialSpecification/' > ./gen/credSpec.xml

# urn:abc4trust:credspec:ldap:abcPerson == urn%3Aabc4trust%3Acredspec%3Aldap%3AabcPerson

#echo 'Storing CredentialSpecification at Issuer'

#curl --user both:tomcat -X PUT --header 'Content-Type: application/xml' -d @./gen/credSpec.xml 'http://localhost:8888/zhaw-p2abc-webservices/issuance/protected/storeCredentialSpecification/urn%3Afiware%3Aprivacy%3Acredspec%3Auserdata'

echo 'Store QueryRule at Issuer'

curl --user both:tomcat -X PUT --header 'Content-Type: application/xml' -d @queryRule.xml 'http://localhost:8888/zhaw-p2abc-webservices/issuance/protected/queryRule/store/urn%3Afiware%3Aprivacy%3Auserdata'

echo 'Store IssuancePolicy at Issuer'

curl --user both:tomcat -X PUT --header 'Content-Type: application/xml' -d @issuancePolicy.xml 'http://localhost:8888/zhaw-p2abc-webservices/issuance/protected/issuancePolicy/store/urn%3Afiware%3Aprivacy%3Auserdata'

echo "Setup System Parameters"
curl --user both:tomcat -X POST --header 'Content-Type: text/xml' 'http://localhost:8888/zhaw-p2abc-webservices/issuance/protected/setupSystemParameters/?securityLevel=80&cryptoMechanism=urn:abc4trust:1.0:algorithm:idemix' > ./out/systemparameters.xml

echo "Store System parameters at User"
curl -X PUT --header 'Content-Type: text/xml' -d @./out/systemparameters.xml 'http://localhost:8888/zhaw-p2abc-webservices/user/storeSystemParameters/'
