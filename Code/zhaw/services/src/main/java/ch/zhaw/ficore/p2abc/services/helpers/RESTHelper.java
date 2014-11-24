package ch.zhaw.ficore.p2abc.services.helpers;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

public class RESTHelper {
    
    private static String authUser = "both";
    private static String authPw = "tomcat";
    
    /**
     * Serializes an object using JAXB to a XML.
     * 
     * @param clazz
     *            Class of the object
     * @param obj
     *            the object
     * @return XML as string
     * @throws JAXBException
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
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Object fromXML(Class clazz, String xml) throws JAXBException {
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
     * @throws UniformInterfaceException
     * @throws JAXBException
     */
    @SuppressWarnings("rawtypes")
    public static Object postRequest(String url, String xml, Class clazz)
            throws ClientHandlerException, UniformInterfaceException,
            JAXBException {
        Client client = new Client();
        client.addFilter(new HTTPBasicAuthFilter(authUser, authPw));

        WebResource webResource = client.resource(url);

        ClientResponse response = webResource.type("application/xml").post(
                ClientResponse.class, xml);

        if (response.getStatus() != 200)
            throw new RuntimeException("postRequest failed for: " + url
                    + " got " + response.getStatus() + "|" + response.getEntity(String.class));

        return fromXML(clazz, response.getEntity(String.class));
    }
    
    public static Object putRequest(String url, String xml, Class clazz)
            throws ClientHandlerException, UniformInterfaceException,
            JAXBException {
        Client client = new Client();
        client.addFilter(new HTTPBasicAuthFilter(authUser, authPw));

        WebResource webResource = client.resource(url);

        ClientResponse response = webResource.type("application/xml").put(
                ClientResponse.class, xml);

        if (response.getStatus() != 200)
            throw new RuntimeException("postRequest failed for: " + url
                    + " got " + response.getStatus() + "|" + response.getEntity(String.class));

        return fromXML(clazz, response.getEntity(String.class));
    }
    
    @SuppressWarnings("rawtypes")
    public static Object getRequest(String url, Class clazz)
            throws ClientHandlerException, UniformInterfaceException,
            JAXBException {
        Client client = new Client();
        client.addFilter(new HTTPBasicAuthFilter(authUser, authPw));

        WebResource webResource = client.resource(url);

        ClientResponse response = webResource.get(
                ClientResponse.class);

        if (response.getStatus() != 200)
            throw new RuntimeException("getRequest failed for: " + url
                    + " got " + response.getStatus());

        return fromXML(clazz, response.getEntity(String.class));
    }
}
