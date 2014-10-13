package eu.abc4trust.ldap;

import javax.xml.bind.*;
import javax.xml.bind.annotation.*;
import javax.xml.*;
import java.io.*;

@XmlRootElement(name="ldap-service-config")
public class LdapServiceConfig {
	@XmlElement(name="host")
	public String host;
	@XmlElement(name="port")
	public int port;
	@XmlElement(name="name")
	public String name;
	@XmlElement(name="auth-id")
	public String authId;
	@XmlElement(name="auth-pw")
	public String authPw;

	public boolean verify() {
		return (host != null) && (port != 0) && (name != null) && (authId != null) && (authPw != null);
	}


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
}
