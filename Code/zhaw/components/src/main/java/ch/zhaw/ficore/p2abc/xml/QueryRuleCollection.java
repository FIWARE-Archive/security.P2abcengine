package ch.zhaw.ficore.p2abc.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/** The XML for query rule collections.
 *
 * @author Roman M&uuml;ntener &lt;roman.muentener@zhaw.ch&gt;
 *
 */
@XmlRootElement(name = "query-rule-collection", namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
public class QueryRuleCollection {

    @XmlElementWrapper(name = "query-rules", required = true, namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
    @XmlElement(name = "query-rule", namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(
            value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
            justification = "Field is read from another project")
    public List<QueryRule> queryRules = new ArrayList<QueryRule>();

    @XmlElement(name = "uris", required = true, namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(
            value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
            justification = "Field is read from another project")
    public List<String> uris = new ArrayList<String>();

    public QueryRuleCollection() {

    }
}
