package ch.zhaw.ficore.p2abc.services.helpers.issuer;

import javax.servlet.http.HttpServletRequest;

import com.hp.gagawa.java.elements.A;
import com.hp.gagawa.java.elements.Body;
import com.hp.gagawa.java.elements.Div;
import com.hp.gagawa.java.elements.H1;
import com.hp.gagawa.java.elements.H2;
import com.hp.gagawa.java.elements.Head;
import com.hp.gagawa.java.elements.Html;
import com.hp.gagawa.java.elements.Link;
import com.hp.gagawa.java.elements.P;
import com.hp.gagawa.java.elements.Script;
import com.hp.gagawa.java.elements.Text;
import com.hp.gagawa.java.elements.Title;

public class IssuerGUI {

    private static String cssURL = "/css/style.css";


    public static Html getHtmlPramble(String title, HttpServletRequest req) {
        Html html = new Html();
        Head head = new Head().appendChild(new Title().appendChild(new Text(
                title)));
        html.appendChild(head);
        head.appendChild(new Link().setHref(req.getContextPath() + cssURL)
                .setRel("stylesheet").setType("text/css"));
        head.appendChild(new Script("").setSrc(req.getContextPath() + "/csrf.js").setType("text/javascript"));
        return html;
    }

    public static Body getBody(Div mainDiv) {
        Div containerDiv = new Div().setCSSClass("containerDiv");
        containerDiv.appendChild(new H1().appendChild(new Text("Issuer")));
        Div navDiv = new Div().setCSSClass("navDiv");
        containerDiv.appendChild(navDiv);
        containerDiv.appendChild(mainDiv);
        navDiv.appendChild(new A().setHref("./obtainCredentialSpecification")
                .appendChild(new Text("Obtain Credential Specification")));
        navDiv.appendChild(new A().setHref("./profile").appendChild(
                new Text("Profile")));
        navDiv.appendChild(new Div().setStyle("clear: both"));
        Body body = new Body().appendChild(containerDiv);
        body.setAttribute("onload", "csrf();");
        return body; 
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
