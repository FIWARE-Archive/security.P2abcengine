package ch.zhaw.ficore.p2abc.services.helpers.issuer;

import java.io.StringReader;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import com.hp.gagawa.java.elements.A;
import com.hp.gagawa.java.elements.Body;
import com.hp.gagawa.java.elements.Div;
import com.hp.gagawa.java.elements.H2;
import com.hp.gagawa.java.elements.Head;
import com.hp.gagawa.java.elements.Html;
import com.hp.gagawa.java.elements.Link;
import com.hp.gagawa.java.elements.P;
import com.hp.gagawa.java.elements.Text;
import com.hp.gagawa.java.elements.Title;

public class IssuerGUI {

    private static String cssURL = "/css/style.css";

    /**
     * Serializes an object using JAXB to a XML.
     * 
     * @param clazz
     *            Class of the object
     * @param obj
     *            the object
     * @return XML as string
     * @throws JAXBException when serialization fails.
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
     * @throws JAXBException when deserialization fails
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Object fromXML(Class clazz, String xml) throws JAXBException {
        return JAXB.unmarshal(new StringReader(xml), clazz);
    }

    public static Html getHtmlPramble(String title, HttpServletRequest req) {
        Html html = new Html();
        Head head = new Head().appendChild(new Title().appendChild(new Text(
                title)));
        html.appendChild(head);
        head.appendChild(new Link().setHref(req.getContextPath()+cssURL).setRel("stylesheet")
                .setType("text/css"));
        return html;
    }

    public static Body getBody(Div mainDiv) {
        Div containerDiv = new Div().setCSSClass("containerDiv");
        Div navDiv = new Div().setCSSClass("navDiv");
        containerDiv.appendChild(navDiv);
        containerDiv.appendChild(mainDiv);
        navDiv.appendChild(new A().setHref("./obtainCredentialSpecification").appendChild(
                new Text("Obtain Credential Specification")));
        navDiv.appendChild(new A().setHref("./profile").appendChild(
                new Text("Profile")));
        navDiv.appendChild(new Div().setStyle("clear: both"));
        return new Body().appendChild(containerDiv);
    }

    public static Html errorPage(String msg, HttpServletRequest req) {
        Html html = getHtmlPramble("ERROR", req);
        Div mainDiv = new Div().setCSSClass("mainDiv");
        html.appendChild(getBody(mainDiv));
        mainDiv.appendChild(new H2().appendChild(new Text("Error")));
        mainDiv.appendChild(new P().setCSSClass("error").appendChild(
                new Text(msg)));
        return html;
    }
}
