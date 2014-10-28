package ch.zhaw.ficore.p2abc.services.issuance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.zhaw.ficore.p2abc.ldap.helper.LdapConnection;
import ch.zhaw.ficore.p2abc.ldap.helper.LdapConnectionConfig;
import ch.zhaw.ficore.p2abc.services.ServicesConfiguration;
import ch.zhaw.ficore.p2abc.services.issuance.xml.AttributeInfoCollection;



/**
 * An AttributeInfoProvider that is capable of providing
 * attribute meta-information over LDAP.
 * 
 * @author mroman
 */
public class LdapAttributeInfoProvider extends AttributeInfoProvider {
	
	/* Syntax mappings determine to which xml-Datatype we map an ldap-Datatype */
	public static Map<String, List<String>> syntaxMappings = new HashMap<String, List<String>>();
	/* Mapping encodings determine how we encode the values (for the syntax mapping) in the credential */
	public static Map<String, List<String>> mappingEncodings = new HashMap<String, List<String>>();
	
	private Logger logger;
	
	/* Ldap Syntax Constants */
	private static final String directoryStringOid = "1.3.6.1.4.1.1466.115.121.1.15";
	private static final String telephoneNumberOid = "1.3.6.1.4.1.1466.115.121.1.50";
	private static final String integerOid = "1.3.6.1.4.1.1466.115.121.1.27";

	/*
	 * This sets up the dictionaries with recommended mappings for LDAP data types to
	 * p2abc data types and encodings for those mappings. For example
	 * 1.3.6.1.4.1.1466.115.121.1.27 will be mapped to xs:integer and xs:integer
	 * will be encoded per default as urn:abc4trust:1.0:encoding:integer:signed
	 */
	static {
		syntaxMappings.put(directoryStringOid, new ArrayList<String>());
		syntaxMappings.put(telephoneNumberOid, new ArrayList<String>());
		syntaxMappings.put(integerOid, new ArrayList<String>());


		syntaxMappings.get(directoryStringOid).add("xs:string");
		syntaxMappings.get(telephoneNumberOid).add("xs:string");
		syntaxMappings.get(integerOid).add("xs:integer");

		mappingEncodings.put("xs:string", new ArrayList<String>());
		mappingEncodings.put("xs:integer", new ArrayList<String>());

		mappingEncodings.get("xs:string").add("urn:abc4trust:1.0:encoding:string:sha-256");
		mappingEncodings.get("xs:integer").add("urn:abc4trust:1.0:encoding:integer:signed");
	}
	
	/**
	 * Constructor
	 */
	public LdapAttributeInfoProvider(IssuanceConfigurationData configuration) {
		super(configuration);
		logger = LogManager.getLogger(LdapAttributeInfoProvider.class.getName());
	}
	
	/**
	 * No Operation.
	 */
	public void shutdown() {
		
	}
	
	/**
	 * Returns an AttributeInfoCollection filled with the attributes of an objectClass
	 * in LDAP.
	 * 
	 * @return an AttributeInfoCollection, null if something went wrong
	 */
	public AttributeInfoCollection getAttributes(String name) {
		logger.entry();
		
		try {
			LdapConnectionConfig cfg = new LdapConnectionConfig(
			    configuration.getLdapServerPort(), configuration.getLdapServerName());
			cfg.setAuth(configuration.getLdapUser(), configuration.getLdapPassword());
			LdapConnection con = cfg.newConnection();
			
			DirContext ctx = con.getInitialDirContext();
			DirContext schema = ctx.getSchema("ou=schema");

			Attributes answer = schema.getAttributes("ClassDefinition/" + name);
			javax.naming.directory.Attribute must = answer.get("must");
			javax.naming.directory.Attribute may = answer.get("may");

			AttributeInfoCollection aic = new AttributeInfoCollection(name);

			for(int i = 0; i < must.size(); i++) {
				Attributes attrAnswer = schema.getAttributes("AttributeDefinition/" + must.get(i));
				String mapping = getMapping(attrAnswer.get("syntax").get(0).toString());
				String encoding = getEncoding(mapping);
				aic.addAttribute(must.get(i).toString(), mapping, encoding);
			}

			for(int i = 0; i < may.size(); i++) {
				Attributes attrAnswer = schema.getAttributes("AttributeDefinition/" + may.get(i));
				String mapping = getMapping(attrAnswer.get("syntax").get(0).toString());
				String encoding = getEncoding(mapping);
				aic.addAttribute(may.get(i).toString(), mapping, encoding);
			}
			
			return logger.exit(aic);
		}
		catch(Exception e) {
			logger.catching(e);
			return logger.exit(null);
		}
	}
	
	/**
	 * This function 'translates' an LDAP-Syntax into a Type supported by p2abc.
	 * (p2abc uses XML-Types).
	 * 
	 * @param syntax LDAP-Syntax
	 * @return mapping (that is, to which XML-Type shall the LDAP-Type be mapped to)
	 */
	private String getMapping(String syntax) {
		syntax = syntax.replaceAll("\\{\\d+\\}$",""); //Strip away length restrictions
		if(syntaxMappings.containsKey(syntax))
			return syntaxMappings.get(syntax).get(0);
		else
			return "xs:string"; //use string as default mapping
	}
	
	/**
	 * This function returns the recommended encoding used by the p2abc to encode
	 * XML-Types. 
	 * 
	 * @param mapping An mapping as returned by getMapping.
	 * @return the recommended encoding
	 */
	private String getEncoding(String mapping) {
		if(mappingEncodings.containsKey(mapping))
			return mappingEncodings.get(mapping).get(0);
		else
			throw new RuntimeException("Can not determine encoding for mapping: " + mapping);
	}
}