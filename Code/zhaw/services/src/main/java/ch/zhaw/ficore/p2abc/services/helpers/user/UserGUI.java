package ch.zhaw.ficore.p2abc.services.helpers.user;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.hp.gagawa.java.elements.A;
import com.hp.gagawa.java.elements.B;
import com.hp.gagawa.java.elements.Body;
import com.hp.gagawa.java.elements.Div;
import com.hp.gagawa.java.elements.Form;
import com.hp.gagawa.java.elements.H2;
import com.hp.gagawa.java.elements.H3;
import com.hp.gagawa.java.elements.Head;
import com.hp.gagawa.java.elements.Html;
import com.hp.gagawa.java.elements.Input;
import com.hp.gagawa.java.elements.Li;
import com.hp.gagawa.java.elements.Link;
import com.hp.gagawa.java.elements.Option;
import com.hp.gagawa.java.elements.P;
import com.hp.gagawa.java.elements.Select;
import com.hp.gagawa.java.elements.Table;
import com.hp.gagawa.java.elements.Td;
import com.hp.gagawa.java.elements.Text;
import com.hp.gagawa.java.elements.Title;
import com.hp.gagawa.java.elements.Tr;
import com.hp.gagawa.java.elements.Ul;

import eu.abc4trust.returnTypes.ui.CredentialInUi;
import eu.abc4trust.returnTypes.ui.PseudonymInUi;
import eu.abc4trust.returnTypes.ui.PseudonymListCandidate;
import eu.abc4trust.returnTypes.ui.RevealedAttributeValue;
import eu.abc4trust.returnTypes.ui.RevealedFact;
import eu.abc4trust.returnTypes.ui.TokenCandidate;
import eu.abc4trust.xml.FriendlyDescription;

public class UserGUI {

    private static String cssURL = "/css/style.css";

    public static Html getHtmlPramble(String title, HttpServletRequest req) {
        Html html = new Html();
        Head head = new Head().appendChild(new Title().appendChild(new Text(
                title)));
        html.appendChild(head);
        head.appendChild(new Link().setHref(req.getContextPath() + cssURL)
                .setRel("stylesheet").setType("text/css"));
        return html;
    }

    public static Body getBody(Div mainDiv) {
        Div containerDiv = new Div().setCSSClass("containerDiv");
        Div navDiv = new Div().setCSSClass("navDiv");
        containerDiv.appendChild(navDiv);
        containerDiv.appendChild(mainDiv);
        navDiv.appendChild(new A().setHref("./obtainCredential").appendChild(
                new Text("Obtain Credential")));
        navDiv.appendChild(new A().setHref("./requestResource").appendChild(
                new Text("Request Resource")));
        navDiv.appendChild(new A().setHref("./profile").appendChild(
                new Text("Profile")));
        navDiv.appendChild(new A().setHref("./loadSettings").appendChild(
                new Text("Load Settings")));
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

    /**
     * Helper function that generates HTML for the UI. Specifically it generates
     * the HTML for the identity selection.
     * 
     * @param tcs
     *            A list of TokenCandidates
     * @param policyId
     *            Id of the policy used by the TokenCandidates
     * @param uiContext
     *            Context string
     * @param backURL URL to go back.
     * @return Div (HTML)
     */
    public static Div getDivForTokenCandidates(List<TokenCandidate> tcs,
            int policyId, String uiContext, String backURL) {
        Div enclosing = new Div();
        enclosing.appendChild(new P().appendChild(new Text(Integer.toString(tcs
                .size()))));
        for (TokenCandidate tc : tcs) {
            Div div = new Div();
            div.setCSSClass("tokenCandidate");
            div.appendChild(new H2().appendChild(new Text("Candidate")));
            enclosing.appendChild(div);

            Table tbl = new Table();
            Tr row = null;
            Td td = null;

            div.appendChild(new H3().appendChild(new Text("Credentials "
                    + tc.credentials.size())));
            div.appendChild(tbl);

            for (CredentialInUi c : tc.credentials) {
                Form f = new Form(backURL).setMethod("post");

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
                td.appendChild(new Text(c.desc.getCredentialSpecificationUID()
                        .toString()));
                row.appendChild(td);
                tbl.appendChild(row);

                f.appendChild(new Input().setType("hidden").setName("uic")
                        .setValue(uiContext));
                f.appendChild(new Input().setType("hidden").setName("policyId") // chosenPolicy
                        .setValue(Integer.toString(policyId)));
                f.appendChild(new Input().setType("hidden")
                        .setName("candidateId") // chosenPresentationToken or
                                                // chosenIssuanceToken (weird
                                                // stuff)
                        .setValue(Integer.toString(tc.candidateId)));

                Select sel = new Select();
                sel.setName("pseudonymId");
                for (PseudonymListCandidate pc : tc.pseudonymCandidates) {
                    sel.appendChild(new Option().appendChild(new Text(Integer
                            .toString(pc.candidateId)))); // chosenPseudonymList
                    System.out.println("____P_____");
                    for(PseudonymInUi p : pc.pseudonyms) {
                        System.out.println(p.uri.toString());
                        System.out.println(p.pseudonym.getPseudonymUID().toString());
                    }
                    // TODO: What the hell is this pseudonym thing?
                }
                f.appendChild(sel);

                f.appendChild(new Input().setType("submit").setValue(
                        "Continue using this candidate."));

                enclosing.appendChild(f);
            }
        }
        P p = new P().appendChild(new B().appendChild(new Text(
                "Revealed attributes")));
        enclosing.appendChild(p);

        Ul ul = new Ul();

        for (TokenCandidate tc : tcs) {
            List<RevealedAttributeValue> reveals = tc.revealedAttributeValues;
            for (RevealedAttributeValue reveal : reveals) {
                for (FriendlyDescription desc : reveal.descriptions) {
                    ul.appendChild(new Li().appendChild(new Text(desc
                            .getValue())));
                }
            }
        }

        enclosing.appendChild(ul);

        ul = new Ul();

        p = new P()
                .appendChild(new B().appendChild(new Text("Revealed facts")));
        enclosing.appendChild(p);

        for (TokenCandidate tc : tcs) {
            List<RevealedFact> facts = tc.revealedFacts;
            for (RevealedFact fact : facts) {
                for (FriendlyDescription desc : fact.descriptions) {
                    ul.appendChild(new Li().appendChild(new Text(desc
                            .getValue())));
                }
            }
        }

        enclosing.appendChild(ul);

        return enclosing;
    }
}
