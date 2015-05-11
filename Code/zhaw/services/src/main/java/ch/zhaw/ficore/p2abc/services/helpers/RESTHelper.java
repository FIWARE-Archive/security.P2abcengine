package ch.zhaw.ficore.p2abc.services.helpers;

import java.io.StringReader;
import java.io.StringWriter;
import java.security.NoSuchAlgorithmException;

import javax.naming.NamingException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import ch.zhaw.ficore.p2abc.configuration.ServicesConfiguration;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;

public class RESTHelper {
    /**
     * Serializes an object using JAXB to a XML.
     * 
     * @param clazz
     *            Class of the object
     * @param obj
     *            the object
     * @return XML as string
     * @throws JAXBException
     *             when serialization fails.
     */
    @SuppressWarnings("rawtypes")
    public static String toXML(Class clazz, Object obj) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(clazz);
        javax.xml.bind.Marshaller m = context.createMarshaller();
        m.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT,
                Boolean.FALSE);
        StringWriter w = new StringWriter();
        m.marshal(obj, w);
        return w.toString();
    }

    /**
     * Deserializes XML to an object.
     * 
     * @param clazz
     *            Class of the object
     * @param xml
     *            the input data
     * @return the object
     * @throws JAXBException
     *             when deseralization fails.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Object fromXML(Class clazz, String xml) throws JAXBException {
        System.out.println("--- 1");
        System.out.println(xml);
        System.out.println("2 ---");
        return JAXB.unmarshal(new StringReader(xml), clazz);
    }

    /**
     * Performs a post request and returns the result as an object.
     * 
     * @param url
     *            URL
     * @param xml
     *            XML data to send (will be sent as application/xml).
     * @param clazz
     *            class of the object to be returned (needed for
     *            deserialization)
     * @return the object
     * @throws ClientHandlerException
     *             When a connection error occurs.
     * @throws UniformInterfaceException
     *             When something else went wrong.
     * @throws JAXBException
     *             When either serialization or deserialization fails.
     */
    @SuppressWarnings("rawtypes")
    public static Object postRequest(String url, String xml, Class clazz)
            throws ClientHandlerException, UniformInterfaceException,
            JAXBException, NamingException {
        Client client = getSSLClient();
        client.addFilter(new HTTPBasicAuthFilter(ServicesConfiguration.getRestAuthUser(), ServicesConfiguration.getRestAuthPassword()));

        WebResource webResource = client.resource(url);

        ClientResponse response = webResource.type("application/xml").post(
                ClientResponse.class, xml);

        if (response.getStatus() != 200)
            throw new RESTException("postRequest failed for: " + url
                    + " got " + response.getStatus() + "|"
                    + response.getEntity(String.class), response.getStatus());

        return fromXML(clazz, response.getEntity(String.class));
    }

    public static Object postRequest(String url, String xml)
            throws ClientHandlerException, UniformInterfaceException,
            JAXBException, NamingException {
        Client client = getSSLClient();
        client.addFilter(new HTTPBasicAuthFilter(ServicesConfiguration.getRestAuthUser(), ServicesConfiguration.getRestAuthPassword()));

        WebResource webResource = client.resource(url);

        ClientResponse response = webResource.type("application/xml").post(
                ClientResponse.class, xml);

        if (response.getStatus() != 200)
            throw new RESTException("postRequest failed for: " + url
                    + " got " + response.getStatus() + "|"
                    + response.getEntity(String.class), response.getStatus());

        return response.getEntity(String.class);
    }

    @SuppressWarnings("rawtypes")
    public static Object postRequest(String url, Class clazz)
            throws ClientHandlerException, UniformInterfaceException,
            JAXBException, NamingException {
        Client client = getSSLClient();
        client.addFilter(new HTTPBasicAuthFilter(ServicesConfiguration.getRestAuthUser(), ServicesConfiguration.getRestAuthPassword()));

        WebResource webResource = client.resource(url);

        ClientResponse response = webResource.type("application/xml").post(
                ClientResponse.class);

        if (response.getStatus() != 200)
            throw new RESTException("postRequest failed for: " + url
                    + " got " + response.getStatus() + "|"
                    + response.getEntity(String.class), response.getStatus());

        return fromXML(clazz, response.getEntity(String.class));
    }

    public static Object postRequest(String url) throws ClientHandlerException,
            UniformInterfaceException, JAXBException, NamingException {
        Client client = getSSLClient();
        client.addFilter(new HTTPBasicAuthFilter(ServicesConfiguration.getRestAuthUser(), ServicesConfiguration.getRestAuthPassword()));

        WebResource webResource = client.resource(url);

        ClientResponse response = webResource.type("application/xml").post(
                ClientResponse.class);

        if (response.getStatus() != 200)
            throw new RESTException("postRequest failed for: " + url
                    + " got " + response.getStatus() + "|"
                    + response.getEntity(String.class), response.getStatus());

        return response.getEntity(String.class);
    }

    public static Object postRequest(String url,
            MultivaluedMap<String, String> params)
            throws ClientHandlerException, UniformInterfaceException,
            JAXBException, NamingException {
        Client client = getSSLClient();
        client.addFilter(new HTTPBasicAuthFilter(ServicesConfiguration.getRestAuthUser(), ServicesConfiguration.getRestAuthPassword()));

        WebResource webResource = client.resource(url);

        ClientResponse response = webResource.type(
                MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(
                ClientResponse.class, params);

        if (response.getStatus() != 200)
            throw new RESTException("postRequest failed for: " + url
                    + " got " + response.getStatus() + "|"
                    + response.getEntity(String.class), response.getStatus());

        return response.getEntity(String.class);
    }
    
    public static String postRequest(String url,
            MultivaluedMap<String, String> params, String user, String password)
            throws ClientHandlerException, UniformInterfaceException,
            JAXBException, NamingException {
        Client client = getSSLClient();
        client.addFilter(new HTTPBasicAuthFilter(user, password));

        WebResource webResource = client.resource(url);

        ClientResponse response = webResource.type(
                MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(
                ClientResponse.class, params);

        if (response.getStatus() != 200)
            throw new RESTException("postRequest failed for: " + url
                    + " got " + response.getStatus() + "|"
                    + response.getEntity(String.class), response.getStatus());

        return response.getEntity(String.class);
    }

    @SuppressWarnings("rawtypes")
    public static Object putRequest(String url, String xml, Class clazz)
            throws ClientHandlerException, UniformInterfaceException,
            JAXBException, NamingException {
        Client client = getSSLClient();
        client.addFilter(new HTTPBasicAuthFilter(ServicesConfiguration.getRestAuthUser(), ServicesConfiguration.getRestAuthPassword()));

        WebResource webResource = client.resource(url);

        ClientResponse response = webResource.type("application/xml").put(
                ClientResponse.class, xml);

        if (response.getStatus() != 200)
            throw new RESTException("putRequest failed for: " + url
                    + " got " + response.getStatus() + "|"
                    + response.getEntity(String.class), response.getStatus());

        return fromXML(clazz, response.getEntity(String.class));
    }

    public static Object putRequest(String url, String xml)
            throws ClientHandlerException, UniformInterfaceException,
            JAXBException, NamingException {
        Client client = getSSLClient();
        client.addFilter(new HTTPBasicAuthFilter(ServicesConfiguration.getRestAuthUser(), ServicesConfiguration.getRestAuthPassword()));

        WebResource webResource = client.resource(url);

        ClientResponse response = webResource.type("application/xml").put(
                ClientResponse.class, xml);

        if (response.getStatus() != 200)
            throw new RESTException("putRequest failed for: " + url
                    + " got " + response.getStatus() + "|"
                    + response.getEntity(String.class), response.getStatus());

        return response.getEntity(String.class);
    }

    public static Object putRequest(String url,
            MultivaluedMap<String, String> params)
            throws ClientHandlerException, UniformInterfaceException,
            JAXBException, NamingException {
        Client client = getSSLClient();
        client.addFilter(new HTTPBasicAuthFilter(ServicesConfiguration.getRestAuthUser(), ServicesConfiguration.getRestAuthPassword()));

        WebResource webResource = client.resource(url);

        ClientResponse response = webResource.type(
                MediaType.APPLICATION_FORM_URLENCODED_TYPE).put(
                ClientResponse.class, params);

        if (response.getStatus() != 200)
            throw new RESTException("putRequest failed for: " + url
                    + " got " + response.getStatus() + "|"
                    + response.getEntity(String.class), response.getStatus());

        return response.getEntity(String.class);
    }

    @SuppressWarnings("rawtypes")
    public static Object getRequest(String url, Class clazz)
            throws ClientHandlerException, UniformInterfaceException,
            JAXBException, NamingException {
        Client client = getSSLClient();
        client.addFilter(new HTTPBasicAuthFilter(ServicesConfiguration.getRestAuthUser(), ServicesConfiguration.getRestAuthPassword()));

        WebResource webResource = client.resource(url);

        ClientResponse response = webResource.get(ClientResponse.class);

        if (response.getStatus() != 200)
            throw new RESTException("getRequest failed for: " + url
                    + " got " + response.getStatus() + "|"
                    + response.getEntity(String.class), response.getStatus());

        return fromXML(clazz, response.getEntity(String.class));
    }

    public static Object getRequest(String url) throws ClientHandlerException,
            UniformInterfaceException, JAXBException, NamingException {
        Client client = getSSLClient();
        client.addFilter(new HTTPBasicAuthFilter(ServicesConfiguration.getRestAuthUser(), ServicesConfiguration.getRestAuthPassword()));

        WebResource webResource = client.resource(url);

        ClientResponse response = webResource.get(ClientResponse.class);

        if (response.getStatus() != 200)
            throw new RESTException("getRequest failed for: " + url
                    + " got " + response.getStatus() + "|"
                    + response.getEntity(String.class), response.getStatus());

        return response.getEntity(String.class);
    }
    
    /**
     * Performs a GET request without automatically adding
     * <tt>client.addFilter(new HTTPBasicAuthFilter(ServicesConfiguration.getRestAuthUser(), ServicesConfiguration.getRestAuthPassword()));</tt>
     * 
     * @param url
     * @return
     * @throws ClientHandlerException
     * @throws UniformInterfaceException
     * @throws JAXBException
     * @throws NamingException
     */
    public static Object getRequestUnauth(String url) throws ClientHandlerException,
		    UniformInterfaceException, JAXBException, NamingException {
		Client client = getSSLClient();
		
		WebResource webResource = client.resource(url);
		
		ClientResponse response = webResource.get(ClientResponse.class);
		
		if (response.getStatus() != 200)
		    throw new RESTException("getRequest failed for: " + url
		            + " got " + response.getStatus() + "|"
		            + response.getEntity(String.class), response.getStatus());
		
		return response.getEntity(String.class);
}

    public static Object deleteRequest(String url)
            throws ClientHandlerException, UniformInterfaceException,
            JAXBException, NamingException {
        Client client = getSSLClient();
        client.addFilter(new HTTPBasicAuthFilter(ServicesConfiguration.getRestAuthUser(), ServicesConfiguration.getRestAuthPassword()));

        WebResource webResource = client.resource(url);

        ClientResponse response = webResource.type("application/xml").delete(
                ClientResponse.class);

        if (response.getStatus() != 200)
            throw new RESTException("deleteRequest failed for: " + url
                    + " got " + response.getStatus() + "|"
                    + response.getEntity(String.class), response.getStatus());

        return response.getEntity(String.class);
    }

    public static Object deleteRequest(String url,
            MultivaluedMap<String, String> params)
            throws ClientHandlerException, UniformInterfaceException,
            JAXBException, NamingException {
        Client client = getSSLClient();
        client.addFilter(new HTTPBasicAuthFilter(ServicesConfiguration.getRestAuthUser(), ServicesConfiguration.getRestAuthPassword()));

        WebResource webResource = client.resource(url);

        ClientResponse response = webResource.type(
                MediaType.APPLICATION_FORM_URLENCODED_TYPE).delete(
                ClientResponse.class, params);

        if (response.getStatus() != 200)
            throw new RESTException("deleteRequest failed for: " + url
                    + " got " + response.getStatus() + "|"
                    + response.getEntity(String.class), response.getStatus());

        return response.getEntity(String.class);
    }
    
    public static Client getSSLClient() {
    	ClientConfig config = new DefaultClientConfig();
    	
    	config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(getHostnameVerifier(), getSSLContext()));
    	
    	return Client.create(config);
    }
    
    private static HostnameVerifier getHostnameVerifier() {
    	return new HostnameVerifier() {

    		public boolean verify(String hostname,
    				javax.net.ssl.SSLSession sslSession) {
    			return true;
    		}
    	};
    }

    private static SSLContext getSSLContext() { /* this should trust anything... */
    	javax.net.ssl.TrustManager x509 = new javax.net.ssl.X509TrustManager() {

    		public void checkClientTrusted(
    				java.security.cert.X509Certificate[] arg0, String arg1)
    						throws java.security.cert.CertificateException {
    			return;
    		}

    		public void checkServerTrusted(
    				java.security.cert.X509Certificate[] arg0, String arg1)
    						throws java.security.cert.CertificateException {
    			return;
    		}

    		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
    			return null;
    		}
    	};
    	
    	SSLContext ctx = null;
    	try {
    		ctx = SSLContext.getInstance("SSL");
    		ctx.init(null, new javax.net.ssl.TrustManager[] { x509 }, null);
    	} 
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    	return ctx;
    }
}
