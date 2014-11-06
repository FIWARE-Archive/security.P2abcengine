import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import static org.junit.Assert.*;

public class Test {

    private static String userServiceURL = "http://localhost:8888/zhaw-p2abc-webservices/user/";
    private static String verificationServiceURL = "http://localhost:8888/zhaw-p2abc-webservices/verification/";
    private static String issuanceServiceURL = "http://localhost:8888/zhaw-p2abc-webservices/issuance/";
    
    private static String magic = "*magic*";

    public static void main(String[] args) {
        System.out.println("hi there");

        /* Test if all three services are running by calling /status/
         * on each service and expecting a 200 response.
         */
        testUserStatus();
        testIssuanceStatus();
        testVerificationStatus();
        testAttributeInfoCollection();

        System.out.println("I'm done!");
    }
    
    public static void testAttributeInfoCollection() {
        Client client = Client.create();

        WebResource webResource = client
                .resource(issuanceServiceURL + "attributeInfoCollection/" + magic + "/test");

        ClientResponse response = webResource.get(ClientResponse.class);

        assertTrue(response.getStatus() == 200);
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
