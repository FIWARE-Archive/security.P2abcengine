package ch.zhaw.ficore.p2abc.services.helpers.user;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import com.hp.gagawa.java.elements.B;
import com.hp.gagawa.java.elements.Div;
import com.hp.gagawa.java.elements.Form;
import com.hp.gagawa.java.elements.H2;
import com.hp.gagawa.java.elements.H3;
import com.hp.gagawa.java.elements.Input;
import com.hp.gagawa.java.elements.Li;
import com.hp.gagawa.java.elements.Option;
import com.hp.gagawa.java.elements.P;
import com.hp.gagawa.java.elements.Select;
import com.hp.gagawa.java.elements.Table;
import com.hp.gagawa.java.elements.Td;
import com.hp.gagawa.java.elements.Text;
import com.hp.gagawa.java.elements.Tr;
import com.hp.gagawa.java.elements.Ul;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

import eu.abc4trust.returnTypes.ui.CredentialInUi;
import eu.abc4trust.returnTypes.ui.PseudonymListCandidate;
import eu.abc4trust.returnTypes.ui.RevealedAttributeValue;
import eu.abc4trust.returnTypes.ui.TokenCandidate;
import eu.abc4trust.xml.FriendlyDescription;

public class UserGUI {
    @SuppressWarnings("rawtypes")
    public static String toXML(Class clazz, Object obj) throws JAXBException {
        JAXBContext context = JAXBContext
                .newInstance(clazz);
        javax.xml.bind.Marshaller m = context.createMarshaller();
        m.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
        StringWriter w = new StringWriter();
        m.marshal(obj, w);
        return w.toString();
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Object fromXML(Class clazz, String xml) throws JAXBException {
        return JAXB.unmarshal(new StringReader(xml), clazz);
    }
    
    @SuppressWarnings("rawtypes")
    public static Object postRequest(String url, String xml, Class clazz) throws ClientHandlerException, UniformInterfaceException, JAXBException {
        Client client = new Client();
        
        WebResource webResource = client
                .resource(url);
        
        ClientResponse response = webResource.type("application/xml")
                .post(ClientResponse.class, xml);
        
        if(response.getStatus() != 200)
            throw new RuntimeException("postRequest failed for: " + url +" got " + response.getStatus());
        
        return fromXML(clazz, response.getEntity(String.class));
    }
    
    public static Div getDivForTokenCandidates(List<TokenCandidate> tcs, int policyId, String uiContext) {
        Div enclosing = new Div();
        enclosing.appendChild(new P().appendChild(new Text(Integer.toString(tcs.size()))));
        for(TokenCandidate tc : tcs) {
            Div div = new Div();
            div.setCSSClass("tokenCandidate");
            div.appendChild(new H2().appendChild(new Text("Candidate")));
            enclosing.appendChild(div);
            
            Table tbl = new Table();
            Tr row = null;
            Td td = null;
            
            div.appendChild(new H3().appendChild(new Text("Credentials " + tc.credentials.size())));
            div.appendChild(tbl);
            
            for(CredentialInUi c : tc.credentials) {
                Form f = new Form("post");
                
                row = new Tr();
                td = new Td();
                td.appendChild(new Text("Credential"));
                row.appendChild(td); 
                td = new Td();
                td.appendChild(new Text(c.uri.toString()));
                row.appendChild(td);       
                tbl.appendChild(row);
                
                row = new Tr();
                td = new Td();
                td.appendChild(new Text("Specification"));
                row.appendChild(td); 
                td = new Td();
                td.appendChild(new Text(c.desc.getCredentialSpecificationUID().toString()));
                row.appendChild(td);       
                tbl.appendChild(row);
                
                f.appendChild(new Input().setType("hidden")
                        .setName("uic")
                        .setValue(uiContext));
                f.appendChild(new Input().setType("hidden")
                        .setName("policyId") //chosenPolicy
                        .setValue(Integer.toString(policyId)));
                f.appendChild(new Input().setType("hidden")
                        .setName("candidateId") //chosenPresentationToken or chosenIssuanceToken (weird stuff)
                        .setValue(Integer.toString(tc.candidateId))); 
                
                Select sel = new Select();
                sel.setName("pseudonymId");
                for(PseudonymListCandidate pc : tc.pseudonymCandidates) {
                    sel.appendChild(new Option().appendChild(new Text(Integer.toString(pc.candidateId)))); //chosenPseudonymList
                    //TODO: What the hell is this pseudonym thing?
                }
                f.appendChild(sel);
                
                f.appendChild(new Input()
                    .setType("submit")
                    .setValue("Continue using this candidate."));
            }
        }
        P p = new P().appendChild(new B().appendChild(new Text("Revealed attributes")));
        enclosing.appendChild(p);
        
        Ul ul = new Ul();
        
        for(TokenCandidate tc : tcs) {
            List<RevealedAttributeValue> reveals = tc.revealedAttributeValues;
            for(RevealedAttributeValue reveal : reveals) {
                for(FriendlyDescription desc : reveal.descriptions) {
                    ul.appendChild(new Li().appendChild(new Text(desc.getValue())));
                }
            }
        }

        enclosing.appendChild(ul);
        return enclosing;
    }
}
