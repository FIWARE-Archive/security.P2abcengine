import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import static org.junit.Assert.*;
import java.io.*;

public class Test {

    private static String userServiceURL = "http://localhost:8888/zhaw-p2abc-webservices/user/";
    private static String verificationServiceURL = "http://localhost:8888/zhaw-p2abc-webservices/verification/";
    private static String issuanceServiceURL = "http://localhost:8888/zhaw-p2abc-webservices/issuance/";
    private static String credSpecName = "test";
    private static String credSpecURI = "urn%3Aprivacy%3Afiware%3Atest";
    private static String issuanceURI = "urn%3Afiware%3Aprivacy%3Aissuance%3Aidemix";
    
    private static String magic = "*magic*";

    /**
     * Performs the whole more or less adopted from the ancient tutorial.
     * 
     * Please don't change the order of the calls as it is relevant!
     * To run this test you have to:
     *  - have the services running (on tomcat)
     *  - have write access to the location where the STORAGE resides
     *  - the STORAGE must be EMPTIED/CLEARED before running this.
     *  - the configuration needs to be set-up to use FAKE identity and FAKE
     *    attribute source (because we check against hardcoded values used in the Fake*Providers.
     *    (Please don't change the values in Fake*Providers without reflecting the changes here
     *    and vice versa)). 
     *    
     * Notes:
     * This integration test tests the whole flow from setup to generation of CredentialSpecification
     * to IssuanceRequest to Verification. However, this test does not check any intermediate results
     * (other than ensuring that the webservices responded with the correct status code) because this test assumes
     * that if the final Verification process succeeds, the test was successful. In other words: This test will
     * obtain a Credential from the Issuance service and verifies the obtained Credential against a
     * PresentationPolicy at the Verification service. 
     */
    public static void main(String[] args) {
        System.out.println("hi there");

        /* Test if all three services are running by calling /status/
         * on each service and expecting a 200 response.
         */
        testUserStatus();
        testIssuanceStatus();
        testVerificationStatus();
        
        /* 
         * Ok, if we are here all services are at least running 
         */
        
        /* Test authentication */
        testAuthentication(readTextFile("simpleAuth.xml"));
        
        /* Get an attributeInfoCollection and convert it to a credentialSpecification */
        String attributeInfoCollection = testAttributeInfoCollection();
        String credSpec = testGenCredSpec(attributeInfoCollection);
        
        /* Store/Get credentialSpecification at issuer*/
        testStoreCredSpecAtIssuer(credSpec);
        testGetCredSpecFromIssuer();
        
        /* Store/Get queryRule at issuer */
        testStoreQueryRuleAtIssuer(readTextFile("queryRule.xml"));
        testGetQueryRuleFromIssuer();
        
        /* Store/Get IssuancePolicy at issuer */
        testStoreIssuancePolicyAtIssuer(readTextFile("issuancePolicy.xml"));
        testGetIssuancePolicyFromIssuer();
        
        /*
         * Ok, if we are here the first phase of setup is done. 
         */
        
        /* Generate the SystemParameters */
        String systemParameters = testSetupSystemParametersIssuer();
        
        /* Store CredentialSpecification at User and Verifier */
        testStoreCredSpecAtUser(credSpec);
        testStoreCredSpecAtVerifier(credSpec);

        System.out.println("I'm done!");
    }
    
    public static String readTextFile(String path) {
        try {
            ClassLoader cl = Test.class.getClassLoader();
            File f = new File(cl.getResource(path).getFile());
            BufferedReader br = new BufferedReader(new FileReader(f));
            String lines = "";
            String line = "";
            while((line = br.readLine()) != null)
                lines += line + "\n";
            br.close();
            return lines;
        }
        catch(Exception e) {
            throw new RuntimeException("readTextFile("+path+") failed!");
        }
    }
    
    public static void testStoreCredSpecAtUser(String credSpec) {
        Client client = Client.create();

        WebResource webResource = client
                .resource(userServiceURL + "storeCredentialSpecification/" + credSpecURI);

        
        ClientResponse response = webResource.type("application/xml")
                        .put(ClientResponse.class, credSpec);
        assertTrue(response.getStatus() == 200);
    }
    
    public static void testStoreCredSpecAtVerifier(String credSpec) {
        Client client = Client.create();

        WebResource webResource = client
                .resource(verificationServiceURL + "storeCredentialSpecification/" + magic + "/" + credSpecURI);

        
        ClientResponse response = webResource.type("application/xml")
                        .put(ClientResponse.class, credSpec);
        assertTrue(response.getStatus() == 200);
    }
    
    public static String testSetupSystemParametersIssuer() {
        String uri = "setupSystemParameters/" + magic + "/?securityLevel=80&cryptoMechanism=urn:abc4trust:1.0:algorithm:idemix";
        
        Client client = Client.create();

        WebResource webResource = client
                .resource(issuanceServiceURL + uri);

        
        ClientResponse response = webResource.type("application/xml")
                        .post(ClientResponse.class);
        assertTrue(response.getStatus() == 200);
        
        return response.getEntity(String.class);
    }
    
    public static String testGetIssuancePolicyFromIssuer() {
        Client client = Client.create();

        WebResource webResource = client
                .resource(issuanceServiceURL + "getIssuancePolicy/" + magic + "/" + credSpecURI);

        ClientResponse response = webResource.get(ClientResponse.class);

        assertTrue(response.getStatus() == 200);
        
        return response.getEntity(String.class);
    }
    
    public static void testStoreIssuancePolicyAtIssuer(String ip) {
        Client client = Client.create();

        WebResource webResource = client
                .resource(issuanceServiceURL + "storeIssuancePolicy/" + magic + "/" + credSpecURI);

        
        ClientResponse response = webResource.type("application/xml")
                        .put(ClientResponse.class, ip);
        assertTrue(response.getStatus() == 200);
    }
    
    public static String testGetQueryRuleFromIssuer() {
        Client client = Client.create();

        WebResource webResource = client
                .resource(issuanceServiceURL + "getQueryRule/" + magic + "/" + credSpecURI);

        ClientResponse response = webResource.get(ClientResponse.class);

        assertTrue(response.getStatus() == 200);
        
        return response.getEntity(String.class);
    }
    
    public static void testStoreQueryRuleAtIssuer(String queryRule) {
        Client client = Client.create();

        WebResource webResource = client
                .resource(issuanceServiceURL + "storeQueryRule/" + magic + "/" + credSpecURI);

        
        ClientResponse response = webResource.type("application/xml")
                        .put(ClientResponse.class, queryRule);
        assertTrue(response.getStatus() == 200);
    }
    
    public static String testGetCredSpecFromIssuer() {
        Client client = Client.create();

        WebResource webResource = client
                .resource(issuanceServiceURL + "getCredentialSpecification/" + magic + "/" + credSpecURI);

        ClientResponse response = webResource.get(ClientResponse.class);

        assertTrue(response.getStatus() == 200);
        
        return response.getEntity(String.class);
    }
    
    public static void testStoreCredSpecAtIssuer(String credSpec) {
        Client client = Client.create();

        WebResource webResource = client
                .resource(issuanceServiceURL + "storeCredentialSpecification/" + magic + "/" + credSpecURI);

        
        ClientResponse response = webResource.type("application/xml")
                        .put(ClientResponse.class, credSpec);
        assertTrue(response.getStatus() == 200);
    }
    
    public static String testAuthentication(String authRequest) {
        Client client = Client.create();

        WebResource webResource = client
                .resource(issuanceServiceURL + "testAuthentication");

        ClientResponse response = webResource.type("application/xml")
                .post(ClientResponse.class, authRequest);

        assertTrue(response.getStatus() == 200);
        
        return response.getEntity(String.class);
    }
    
    public static String testGenCredSpec(String attributeInfoCollection) {
        Client client = Client.create();

        WebResource webResource = client
                .resource(issuanceServiceURL + "genCredSpec/" + magic + "/");

        
        ClientResponse response = webResource.type("application/xml")
                        .post(ClientResponse.class, attributeInfoCollection);
        
        assertTrue(response.getStatus() == 200);
        
        return response.getEntity(String.class);
    }
    
    public static String testAttributeInfoCollection() {
        Client client = Client.create();

        WebResource webResource = client
                .resource(issuanceServiceURL + "attributeInfoCollection/" + magic + "/" + credSpecName);

        ClientResponse response = webResource.get(ClientResponse.class);

        assertTrue(response.getStatus() == 200);
        
        return response.getEntity(String.class);
    }

    public static void testUserStatus() {
        Client client = Client.create();

        WebResource webResource = client
                .resource(userServiceURL + "status");

        ClientResponse response = webResource.get(ClientResponse.class);

        assertTrue(response.getStatus() == 200);
    }
    
    public static void testIssuanceStatus() {
        Client client = Client.create();

        WebResource webResource = client
                .resource(issuanceServiceURL + "status");

        ClientResponse response = webResource.get(ClientResponse.class);

        assertTrue(response.getStatus() == 200);       
    }
    
    public static void testVerificationStatus() {
        Client client = Client.create();

        WebResource webResource = client
                .resource(verificationServiceURL + "status");

        ClientResponse response = webResource.get(ClientResponse.class);

        assertTrue(response.getStatus() == 200); 
    }
    
}
