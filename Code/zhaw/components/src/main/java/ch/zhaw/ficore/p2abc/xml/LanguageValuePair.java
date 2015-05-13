package ch.zhaw.ficore.p2abc.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/** The XML for a language-value pair.
 *
 * @author Roman M&uuml;ntener &lt;roman.muentener@zhaw.ch&gt;
 *
 */
@XmlRootElement(name = "langValuePair", namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
public class LanguageValuePair {
    @XmlElement(name = "language", required = true, namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(
            value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
            justification = "Field is read from another project")
    public String language;

    @XmlElement(name = "value", required = true, namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(
            value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
            justification = "Field is read from another project")
    public String value;

    public LanguageValuePair() {
    }

    public LanguageValuePair(final String l, final String v) {
        this.language = l;
        this.value = v;
    }
}
