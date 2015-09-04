#!/bin/sh

#urn%3Afiware%3Aprivacy%3Auserdata%3Aissuer-params

#Stop script if an error occurs.
set -e
# Setup System Parameters.
echo "Setup System Parameters"
curl --user both:tomcat -X POST --header 'Content-Type: text/xml' 'http://localhost:12001/zhaw-p2abc-webservices/issuance/protected/setupSystemParameters/?securityLevel=80&cryptoMechanism=urn:abc4trust:1.0:algorithm:idemix' > ./out/systemparameters.xml

# Store credential specification at user.
# This method is not specified in H2.2.
echo "Store credential specification at user"
curl -X PUT --header 'Content-Type: text/xml' -d @./gen/credSpec.xml 'http://localhost:12001/zhaw-p2abc-webservices/user/credentialSpecification/store/urn%3Afiware%3Aprivacy%3Auserdata' > ./out/storeCredentialSpecificationAtUserResponce.xml
cat ./out/storeCredentialSpecificationAtUserResponce.xml

# Store credential specification at verifier.
# This method is not specified in H2.2.
echo "Store credential specification at verifier"
curl --user both:tomcat -X PUT --header 'Content-Type: text/xml' -d @./gen/credSpec.xml 'http://localhost:12001/zhaw-p2abc-webservices/verification/protected/credentialSpecification/store/urn%3Afiware%3Aprivacy%3Auserdata' > ./out/storeCredentialSpecificationAtVerifierResponce.xml
cat ./out/storeCredentialSpecificationAtVerifierResponce.xml


# Store System parameters at User.
# This method is not specified in H2.2.
echo "Store System parameters at User"
curl -X PUT --header 'Content-Type: text/xml' -d @./out/systemparameters.xml 'http://localhost:12001/zhaw-p2abc-webservices/user/systemParameters/store' > ./out/storeSystemParametersResponceAtUser.xml

# Store System parameters at verifier.
# This method is not specified in H2.2.
echo "Store System parameters at Verifier"
curl --user both:tomcat -X PUT --header 'Content-Type: text/xml' -d @./out/systemparameters.xml 'http://localhost:12001/zhaw-p2abc-webservices/verification/protected/systemParameters/store' > ./out/storeSystemParametersResponceAtVerifier.xml

# Setup issuer parameters.
echo "Setup issuer parameters"
curl --user both:tomcat -X POST --header 'Content-Type: text/xml' -d @./issuerParametersInput.xml 'http://localhost:12001/zhaw-p2abc-webservices/issuance/protected/setupIssuerParameters/' > ./out/issuerParameters.xml


# Store Issuer Parameters at user.
# This method is not specified in H2.2.
echo "Store Issuer Parameters at user"
curl -X PUT --header 'Content-Type: text/xml' -d @./out/issuerParameters.xml 'http://localhost:12001/zhaw-p2abc-webservices/user/issuerParameters/store/urn%3Afiware%3Aprivacy%3Auserdata%3Aissuer-params' > ./out/storeIssuerParametersAtUser.xml

# Store Issuer Parameters at verifier.
# This method is not specified in H2.2.
echo "Store Issuer Parameters at verifier"
curl --user both:tomcat -X PUT --header 'Content-Type: text/xml' -d @./out/issuerParameters.xml 'http://localhost:12001/zhaw-p2abc-webservices/verification/protected/issuerParameters/store/urn%3Afiware%3Aprivacy%3Auserdata%3Aissuer-params' > ./out/storeIssuerParametersAtVerifier.xml


# Init issuance protocol (first step for the issuer).
#echo "Init issuance protocol"
#curl -X POST --header 'Content-Type: text/xml' -d @./outIssuanceRequest.xml 'http://localhost:12001/zhaw-p2abc-webservices/issuance/initIssuanceProtocol/*magic*' > ./out/issuanceMessageAndBoolean.xml

echo 'IssuanceRequest (by the user)'
curl -X POST --header 'Content-Type: application/xml' -d @issuanceRequest.xml 'http://localhost:12001/zhaw-p2abc-webservices/issuance/issuanceRequest' > ./out/issuanceMessageAndBoolean.xml

# Extract issuance message.
curl -X POST --header 'Content-Type: text/xml' -d @./out/issuanceMessageAndBoolean.xml 'http://localhost:12001/zhaw-p2abc-webservices/user/extractIssuanceMessage/' > ./out/firstIssuanceMessage.xml

# First issuance protocol step (first step for the user).
echo "First issuance protocol step for the user"
curl -X POST --header 'Content-Type: text/xml' -d @./out/firstIssuanceMessage.xml 'http://localhost:12001/zhaw-p2abc-webservices/user/issuanceProtocolStep/' > ./out/issuanceReturn.xml

# Setup uiIssuanceReturn.xml.
UiContext=`cat ./out/issuanceReturn.xml | sed 's/^.*<uiContext>//' | sed 's/<\/uiContext>.*//'`
# echo ${UiContext}
cat ./uiIssuanceReturn.xml | sed "s#REPLACE-THIS-CONTEXT#${UiContext}#" > ./out/uiIssuanceReturn.xml

# First issuance protocol step - UI (first step for the user).
echo "Second issuance protocol step (first step for the user)"
curl -X POST --header 'Content-Type: text/xml' -d @./out/uiIssuanceReturn.xml 'http://localhost:12001/zhaw-p2abc-webservices/user/issuanceProtocolStepUi/' > ./out/secondIssuanceMessage.xml


# Second issuance protocol step (second step for the issuer).
echo "Second issuance protocol step (second step for the issuer)"
curl -X POST --header 'Content-Type: text/xml' -d @./out/secondIssuanceMessage.xml 'http://localhost:12001/zhaw-p2abc-webservices/issuance/issuanceProtocolStep/' > ./out/thirdIssuanceMessageAndBoolean.xml

# Extract issuance message.
curl -X POST --header 'Content-Type: text/xml' -d @./out/thirdIssuanceMessageAndBoolean.xml 'http://localhost:12001/zhaw-p2abc-webservices/user/extractIssuanceMessage/' > ./out/thirdIssuanceMessage.xml

# Third issuance protocol step (second step for the user).
echo "Third issuance protocol step (second step for the user)"
curl -X POST --header 'Content-Type: text/xml' -d @./out/thirdIssuanceMessage.xml 'http://localhost:12001/zhaw-p2abc-webservices/user/issuanceProtocolStep/' > ./out/fourthIssuanceMessageAndBoolean.xml

# Create presentation policy alternatives.
# This method is not specified in H2.2.
echo "Create presentation policy alternatives"
curl -X POST --header 'Content-Type: text/xml' -d @./presentationPolicyAlternatives.xml 'http://localhost:12001/zhaw-p2abc-webservices/verification/createPresentationPolicy/' > ./out/presentationPolicyAlternatives.xml

# Create presentation UI return.
# This method is not specified in H2.2.
echo "Create presentation UI return"
curl -X POST --header 'Content-Type: text/xml' -d @./out/presentationPolicyAlternatives.xml 'http://localhost:12001/zhaw-p2abc-webservices/user/createPresentationToken/' > ./out/presentationReturn.xml

# Setup uiPresentationReturn.xml.
UiContext=`cat ./out/presentationReturn.xml | sed 's/^.*<uiContext>//' | sed 's/<\/uiContext>.*//'`
# echo ${UiContext}
cat ./uiPresentationReturn.xml | sed "s#REPLACE-THIS-CONTEXT#${UiContext}#" > ./out/uiPresentationReturn.xml
  
# Create presentation token.
# This method is not specified in H2.2.
echo "Create presentation token"
curl -X POST --header 'Content-Type: text/xml' -d @./out/uiPresentationReturn.xml 'http://localhost:12001/zhaw-p2abc-webservices/user/createPresentationTokenUi/' > ./out/presentationToken.xml

# Setup presentationPolicyAlternativesAndPresentationToken.xml.
presentationPolicy=`cat ./out/presentationPolicyAlternatives.xml | sed 's/^.*<PresentationPolicyAlternatives xmlns="http:\/\/abc4trust.eu\/wp2\/abcschemav1.0" Version="1.0">//' | sed 's/<\/PresentationPolicyAlternatives>.*//'`
presentationToken=`cat ./out/presentationToken.xml | sed 's/^.*<PresentationToken xmlns="http:\/\/abc4trust.eu\/wp2\/abcschemav1.0" Version="1.0">//' | sed 's/<\/PresentationToken>.*//'`
# echo "${presentationPolicy}"
# echo "${presentationToken}"
echo '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' > ./out/presentationPolicyAlternativesAndPresentationToken.xml
echo '<PresentationPolicyAlternativesAndPresentationToken xmlns="http://abc4trust.eu/wp2/abcschemav1.0" Version="1.0"> <PresentationPolicyAlternatives>' >> ./out/presentationPolicyAlternativesAndPresentationToken.xml
echo "${presentationPolicy}" >> ./out/presentationPolicyAlternativesAndPresentationToken.xml
echo '</PresentationPolicyAlternatives>' >> ./out/presentationPolicyAlternativesAndPresentationToken.xml
echo '<PresentationToken>' >> ./out/presentationPolicyAlternativesAndPresentationToken.xml
echo "${presentationToken}" >> ./out/presentationPolicyAlternativesAndPresentationToken.xml
echo '</PresentationToken>' >> ./out/presentationPolicyAlternativesAndPresentationToken.xml
echo '</PresentationPolicyAlternativesAndPresentationToken>' >> ./out/presentationPolicyAlternativesAndPresentationToken.xml
  
# Verify presentation token against presentation policy.
echo "Verify presentation token against presentation policy"
# This method is not specified in H2.2.
curl -X POST --header 'Content-Type: text/xml' -d @./out/presentationPolicyAlternativesAndPresentationToken.xml 'http://localhost:12001/zhaw-p2abc-webservices/verification/verifyTokenAgainstPolicy/' > ./out/presentationTokenDescription.xml


