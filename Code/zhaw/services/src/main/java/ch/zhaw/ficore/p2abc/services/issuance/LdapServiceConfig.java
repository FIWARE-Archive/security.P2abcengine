package ch.zhaw.ficore.p2abc.services.issuance;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="ldap-service-config")
public class LdapServiceConfig {
	/**
	 * Hostname of the LDAP Server.
	 */
	@XmlElement(name="host")
	private String host;

	/**
	 * Port used by the LDAP Server.
	 */
	@XmlElement(name="port")
	private int port;

	/**
	 * Used for searches (serves as the basis for LDAP
	 * searches done by the LDAP-binding)
	 */
	@XmlElement(name="name")
	private String name;

	/**
	 * Auth. Id. The username that the LDAP connection will use to bind.
	 */
	@XmlElement(name="auth-id")
	private String authId;

	/**
	 * Auth. Pw. The password that the LDAP connection will use to bind.
	 */
	@XmlElement(name="auth-pw")
	private String authPw;

	/**
	 * A magic value that only trusted administrators of the webservices
	 * should know. It's used as a means of authorization to re-configure
	 * the webservice.
	 */
	@XmlElement(name="magic-cookie")
	private String magicCookie;

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getName() {
		return name;
	}

	public String getAuthId() {
		return authId;
	}

	public String getAuthPw() {
		return authPw;
	}

	public String getMagicCookie() {
		return magicCookie;
	}



	/**
	 * Verifies that the loaded configuration is valid and sane.
	 *
	 * @return true if configuration is valid and sane, false otherwise
	 */
	public boolean verify() {
		return (host != null) && (port != 0) && (name != null) && (authId != null) && (authPw != null) &&
				(magicCookie != null);
	}

	/**
	 * Loads an LdapServiceConfig from an xml-file.
	 * This method will call System.exit if configuration could not
	 * be loaded. 
	 *
	 * @param path Path the the configuration xml-file.
	 * @return an instance of LdapServiceConfig
	 */
	public static LdapServiceConfig fromFile(String path) {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(LdapServiceConfig.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			LdapServiceConfig lsc = (LdapServiceConfig)jaxbUnmarshaller.unmarshal(new File(path));
			return lsc;
		}
		catch(Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	/**
	 * Checks if the given magicCookie is the same as the one
	 * in this configuration. 
	 *
	 * @return true if magicCookie is correct, false otherwise.
	 */
	public boolean isMagicCookieCorrect(String magicCookie) {
		return magicCookie.equals(this.magicCookie);
	}
}
