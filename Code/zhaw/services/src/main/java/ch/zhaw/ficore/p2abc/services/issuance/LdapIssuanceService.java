package ch.zhaw.ficore.p2abc.services.issuance;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.List;

import org.apache.commons.lang.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.zhaw.ficore.p2abc.services.ConfigurationException;
import ch.zhaw.ficore.p2abc.services.ServicesConfiguration;
import ch.zhaw.ficore.p2abc.services.StorageModuleFactory;
import ch.zhaw.ficore.p2abc.services.UserStorageManager; //from Code/core-abce/abce-services (COPY)
import ch.zhaw.ficore.p2abc.services.issuance.xml.*;
import ch.zhaw.ficore.p2abc.storage.SqliteURIBytesStorage;
import ch.zhaw.ficore.p2abc.storage.URIBytesStorage;
import ch.zhaw.ficore.p2abc.storage.UnsafeTableNameException;
import ch.zhaw.ficore.p2abc.services.helpers.issuer.*;
import ch.zhaw.ficore.p2abc.services.guice.*;

import eu.abc4trust.guice.ProductionModuleFactory.CryptoEngine;
import eu.abc4trust.keyManager.KeyManager;
import eu.abc4trust.keyManager.KeyStorage;
import eu.abc4trust.xml.ABCEBoolean;
import eu.abc4trust.xml.Attribute;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.ObjectFactory;
import eu.abc4trust.xml.SystemParameters;
import eu.abc4trust.xml.IssuerParameters;
import eu.abc4trust.xml.IssuerParametersInput;
import eu.abc4trust.xml.FriendlyDescription;
import eu.abc4trust.xml.IssuanceMessage;
import eu.abc4trust.xml.IssuanceMessageAndBoolean;
import eu.abc4trust.xml.IssuancePolicy;
import eu.abc4trust.xml.IssuancePolicyAndAttributes;
import eu.abc4trust.cryptoEngine.util.SystemParametersUtil;
import eu.abc4trust.util.CryptoUriUtil;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import java.io.*;

import com.google.inject.Guice;
import com.google.inject.Injector;

@Path("/ldap-issuance-service")
public class LdapIssuanceService {
	@Context
	ServletContext context;

	private static final URI CRYPTOMECHANISM_URI_IDEMIX = URI
			.create("urn:abc4trust:1.0:algorithm:idemix");

	private static final String errMagicCookie = "Magic-Cookie is not correct!";
	private static final String errNoCredSpec = "CredentialSpecification is missing!";
	private static final String errNoIssuancePolicy = "IssuancePolicy is missing!";
	private static final String errNoQueryRule = "QueryRule is missing!";
	
	private ObjectFactory of = new ObjectFactory(); 

	private final String fileStoragePrefix = "issuer_storage/"; //TODO: Files

	private Logger logger;

	static {
		IssuanceConfigurationData cfgData;
		try {
			cfgData = new IssuanceConfigurationData(false, "localhost", 10389,
					"uid=admin, ou=system", "secret");
		} catch (ConfigurationException e) {
			cfgData = null;
		}
		ServicesConfiguration.setIssuanceConfiguration(cfgData);
		initializeWithConfiguration();
	}

	public static void initializeWithConfiguration() {
		IssuanceConfigurationData configuration = ServicesConfiguration.getIssuanceConfiguration();
	}


	public LdapIssuanceService() throws ClassNotFoundException, SQLException, UnsafeTableNameException {
		logger = LogManager.getLogger();
	}

	@GET()
	@Path("/status")
	@Produces({MediaType.TEXT_PLAIN})
	public Response issuerStatus() {
		try {
			JAXBContext jc = JAXBContext.newInstance( new Class[] { QueryRule.class,
					AuthenticationRequest.class, IssuanceRequest.class, AttributeInfoCollection.class,
					LanguageValuePair.class, AttributeInformation.class, AuthInfoSimple.class
			}
					);
			jc.generateSchema(new SchemaOutputResolver() {
				@Override
				public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
					File file = new File("/tmp/out.xsd");
					return new StreamResult(file);
				}
			});
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return Response.ok().build();
	}
	
	@POST()
	@Path("/issuanceRequest/")
	@Consumes({MediaType.APPLICATION_XML})
	public Response issuanceRequest(IssuanceRequest request) {
		try {
			IssuanceConfigurationData configuration = ServicesConfiguration.getIssuanceConfiguration();
			AttributeValueProvider attrValProvider = AttributeValueProvider.getAttributeValueProvider(configuration);
			AuthenticationProvider authProvider = AuthenticationProvider.getAuthenticationProvider(configuration);
			
			if(!authProvider.authenticate(request.authRequest.authInfo))
				return Response.status(Response.Status.FORBIDDEN).build();
			
			this.initializeHelper(CryptoEngine.IDEMIX);
			IssuanceHelper instance = IssuanceHelper.getInstance();
			
			CredentialSpecification credSpec = instance.keyManager.getCredentialSpecification(
					new URI(request.credentialSpecificationUid));
			IssuancePolicy ip = instance.issuanceStorage.getIssuancePolicy(
					new URI(request.credentialSpecificationUid));
			
			IssuancePolicyAndAttributes ipa = of.createIssuancePolicyAndAttributes();
			
			ipa.setIssuancePolicy(ip);
			ipa.getAttribute().addAll(attrValProvider.getAttributes(
					QueryHelper.buildQuery("", authProvider.getUserID()), credSpec));
			
			return Response.ok(of.createIssuancePolicyAndAttributes(ipa), MediaType.APPLICATION_XML).build();
		}
		catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Store QueryRule.
	 * 
	 * This method is protected by the magic cookie.
	 * 
	 * @param magicCookie the magic cookie
	 * @param credentialSpecificationUid UID of the credSpec
	 * @return Response
	 */
	@PUT()
	@Path("/storeQueryRule/{magicCookie}/{credentialSpecificationUid}")
	@Consumes({MediaType.APPLICATION_XML})
	public Response storeQueryRule(@PathParam("magicCookie") String magicCookie,
			@PathParam("credentialSpecificationUid") String credentialSpecificationUid,
			QueryRule rule) {

		logger.entry();

		if(!ServicesConfiguration.isMagicCookieCorrect(magicCookie))
			return logger.exit(Response.status(Response.Status.FORBIDDEN).entity(errMagicCookie).build());

		try {
			this.initializeHelper(CryptoEngine.IDEMIX);
			IssuanceHelper instance = IssuanceHelper.getInstance();
			
			instance.issuanceStorage.addQueryRule(new URI(credentialSpecificationUid), rule);
			return logger.exit(Response.ok("OK").build());
		}
		catch(Exception e) {
			logger.catching(e);
			return logger.exit(Response.serverError().build());
		}
	}

	/**
	 * Retrieve a QueryRule.
	 * 
	 * This method is protected by the magic cookie.
	 * 
	 * @param magicCookie the magic cookie
	 * @param credentialSpecificationUid
	 * @return QueryRule
	 */
	@GET()
	@Path("/getQueryRule/{magicCookie}/{credentialSpecificationUid}")
	@Consumes({MediaType.APPLICATION_XML})
	public Response getQueryRule(@PathParam("magicCookie") String magicCookie,
			@PathParam("credentialSpecificationUid") String credentialSpecificationUid) {

		logger.entry();

		if(!ServicesConfiguration.isMagicCookieCorrect(magicCookie))
			return logger.exit(Response.status(Response.Status.FORBIDDEN).entity(errMagicCookie).build());

		try {
			this.initializeHelper(CryptoEngine.IDEMIX);
			IssuanceHelper instance = IssuanceHelper.getInstance();
			
			QueryRule rule = instance.issuanceStorage.getQueryRule(new URI(credentialSpecificationUid));
			if(rule == null)
				return logger.exit(Response.status(Response.Status.NOT_FOUND).build());
			else
				return logger.exit(Response.ok(rule, MediaType.APPLICATION_XML).build());
		}
		catch(Exception e) {
			logger.catching(e);
			return logger.exit(Response.serverError().build());
		}
	}
	
	/**
	 * Store IssuancePolicy.
	 * 
	 * This method is protected by the magic cookie.
	 * 
	 * @param magicCookie the magic cookie
	 * @param credentialSpecificationUid UID of the credSpec
	 * @return Response
	 */
	@PUT()
	@Path("/storeIssuancePolicy/{magicCookie}/{credentialSpecificationUid}")
	@Consumes({MediaType.APPLICATION_XML})
	public Response storeIssuancePolicy(@PathParam("magicCookie") String magicCookie,
			@PathParam("credentialSpecificationUid") String credentialSpecificationUid,
			IssuancePolicy policy) {

		logger.entry();

		if(!ServicesConfiguration.isMagicCookieCorrect(magicCookie))
			return logger.exit(Response.status(Response.Status.FORBIDDEN).entity(errMagicCookie).build());

		try {
			this.initializeHelper(CryptoEngine.IDEMIX);
			IssuanceHelper instance = IssuanceHelper.getInstance();
			
			instance.issuanceStorage.addIssuancePolicy(new URI(credentialSpecificationUid), policy);
			return logger.exit(Response.ok("OK").build());
		}
		catch(Exception e) {
			logger.catching(e);
			return logger.exit(Response.serverError().build());
		}
	}

	/**
	 * Retrieve an IssuancePolicy
	 * 
	 * This method is protected by the magic cookie.
	 * 
	 * @param magicCookie the magic cookie
	 * @param credentialSpecificationUid
	 * @return IssuancePolicy
	 */
	@GET()
	@Path("/getIssuancePolicy/{magicCookie}/{credentialSpecificationUid}")
	@Consumes({MediaType.APPLICATION_XML})
	public Response getIssuancePolicy(@PathParam("magicCookie") String magicCookie,
			@PathParam("credentialSpecificationUid") String credentialSpecificationUid) {

		logger.entry();

		if(!ServicesConfiguration.isMagicCookieCorrect(magicCookie))
			return logger.exit(Response.status(Response.Status.FORBIDDEN).entity(errMagicCookie).build());

		try {
			this.initializeHelper(CryptoEngine.IDEMIX);
			IssuanceHelper instance = IssuanceHelper.getInstance();
			
			IssuancePolicy policy = instance.issuanceStorage.getIssuancePolicy(new URI(credentialSpecificationUid));
			if(policy == null)
				return logger.exit(Response.status(Response.Status.NOT_FOUND).build());
			else
				return logger.exit(Response.ok(of.createIssuancePolicy(policy), MediaType.APPLICATION_XML).build());
		}
		catch(Exception e) {
			logger.catching(e);
			return logger.exit(Response.serverError().build());
		}
	}


	/**
	 * This method can be used to test the authentication.
	 * It returns a response with status code OK if the authentication
	 * was successful, otherwise it returns a response with status code FORBIDDEN.
	 * 
	 * @param authReq an AuthenticationRequest
	 * @return response
	 */
	@POST()
	@Path("/testAuthentication")
	@Consumes({MediaType.APPLICATION_XML})
	public Response testAuthentication(AuthenticationRequest authReq) {

		IssuanceConfigurationData configuration = ServicesConfiguration.getIssuanceConfiguration();
		AuthenticationProvider authProvider = AuthenticationProvider.getAuthenticationProvider(configuration);
		
		if(authProvider.authenticate(authReq.authInfo))
			return Response.ok("OK").build();
		else
			return Response.status(Response.Status.FORBIDDEN).entity("ERR").build();
	}

	/**
	 * This method can be used to obtain the AttributeInfoCollection
	 * that may later be converted into a CredentialSpecification. 
	 * This method contacts the identity source to obtain the necessary
	 * attributes for <em>name</em>. <em>name</em> refers to a <em>kind</em> of credential
	 * a user can get issued. For example <em>name</em> may refer to an objectClass
	 * in LDAP. However, the exact behaviour of <em>name</em> depends on the configuration
	 * of this service. 
	 * 
	 * This method is protected by the magic cookie.
	 * 
	 * @param magicCookie the magic cookie
	 * @param name name (see description of this method above)
	 * @return an AttributeInfoCollection as application/xml.
	 */
	@GET()
	@Path("/attributeInfoCollection/{magicCookie}/{name}")
	public Response attributeInfoCollection(@PathParam("magicCookie") String magicCookie, 
			@PathParam("name") String name) {
		
		IssuanceConfigurationData configuration = ServicesConfiguration.getIssuanceConfiguration();
		AttributeInfoProvider attribInfoProvider = AttributeInfoProvider.getAttributeInfoProvider(configuration);
		
		if(!ServicesConfiguration.isMagicCookieCorrect(magicCookie))
			return Response.status(Response.Status.FORBIDDEN).entity(errMagicCookie).build();

		return Response.ok(attribInfoProvider.getAttributes(name), MediaType.APPLICATION_XML).build();
	}

	/**
	 * Generates (or creates) the corresponding CredentialSpecification
	 * for a given AttributeInfoCollection. This method assumes that the
	 * given AttributeInfoCollection is sane. 
	 * 
	 * This method is protected by the magic cookie.
	 * 
	 * @param magicCookie the magic cookie
	 * @param attrInfoColl the AttributeInfoCollection
	 * @return a CredentialSpecification
	 */
	@POST()
	@Path("/genCredSpec/{magicCookie}")
	@Consumes({MediaType.APPLICATION_XML})
	public Response genCredSpec(@PathParam("magicCookie") String magicCookie, AttributeInfoCollection attrInfoCol) {
		if(!ServicesConfiguration.isMagicCookieCorrect(magicCookie))
			return Response.status(Response.Status.FORBIDDEN).entity(errMagicCookie).build();

		return Response.ok(of.createCredentialSpecification(new CredentialSpecGenerator().
				generateCredentialSpecification(attrInfoCol)),
				MediaType.APPLICATION_XML).build();
	}
	

	/*
	 * The following section contains code copied from the original issuance service from the tree
	 * Code/core-abce/abce-service
	 */

	/* SECTION */

	private void initializeHelper(CryptoEngine cryptoEngine) {
		logger.info("IssuanceService loading...");

		try {
			if (IssuanceHelper.isInit()) {
				logger.info("IssuanceHelper is initialized");
			} else {

				logger.info("Initializing IssuanceHelper...");

				IssuanceHelper.initInstanceForService(cryptoEngine,
						"", "", 
						StorageModuleFactory.getModulesForServiceConfiguration(
								ServicesConfiguration.ServiceType.ISSUANCE));

				logger.info("IssuanceHelper is initialized");
			}
		} catch (Exception e) {
			System.out.println("Create Domain FAILED " + e);
			e.printStackTrace();
		}
	}
	
	/**
	 * Store a CredentialSpecification at the issuer.
	 * 
	 * This method is protected by the magic cookie.
	 * 
	 * @param magicCookie the magic cookie
	 * @param credentialSpecificationUid UID of the CredentialSpecification
	 * @param credSpec the CredentialSpecification to store
	 * @return Response
	 */
	@PUT()
	@Path("/storeCredentialSpecification/{magicCookie}/{credentialSpecifationUid}")
	@Consumes({ MediaType.APPLICATION_XML })
	public Response storeCredentialSpecification(
			@PathParam("magicCookie") String magicCookie, 
			@PathParam("credentialSpecifationUid") URI credentialSpecifationUid,
			CredentialSpecification credSpec) {

		logger.entry();

		if(!ServicesConfiguration.isMagicCookieCorrect(magicCookie))
			return logger.exit(Response.status(Response.Status.FORBIDDEN).entity(errMagicCookie).build());

		logger.info("IssuanceService - storeCredentialSpecification: \""
				+ credentialSpecifationUid + "\"");

		try {
			this.initializeHelper(CryptoEngine.IDEMIX);
			
			IssuanceHelper instance = IssuanceHelper.getInstance();
			
			CryptoEngine engine = CryptoEngine.IDEMIX;
			KeyManager keyManager = instance.keyManager;

			boolean r1 = keyManager.storeCredentialSpecification(
					credentialSpecifationUid, credSpec);

			ABCEBoolean createABCEBoolean = this.of.createABCEBoolean();
			createABCEBoolean.setValue(r1);

			return logger.exit(
					Response.ok(of.createABCEBoolean(createABCEBoolean), MediaType.APPLICATION_XML).build());
		} catch (Exception ex) {
			logger.catching(ex);
			return logger.exit(Response.serverError().build());
		}
	}

	/**
	 * Retreive a CredentialSpecification from the issuer.
	 * 
	 * This method is protected by the magic cookie.
	 * 
	 * @param magicCookie the magic cookie
	 * @param credentialSpecificationUid UID of the CredentialSpecification to retreive
	 * @return Response (CredentialSpecification)
	 */
	@GET()
	@Path("/getCredentialSpecification/{magicCookie}/{credentialSpecificationUid}")
	public Response getCredentialSpecification(
			@PathParam("magicCookie") String magicCookie,
			@PathParam("credentialSpecificationUid") String credentialSpecificationUid) {
		logger.entry();

		if(!ServicesConfiguration.isMagicCookieCorrect(magicCookie))
			return logger.exit(Response.status(Response.Status.FORBIDDEN).entity(errMagicCookie).build());

		logger.info("IssuanceService - getCredentialSpecification: " + credentialSpecificationUid);
		

		try {
			this.initializeHelper(CryptoEngine.IDEMIX);
			
			IssuanceHelper instance = IssuanceHelper.getInstance();

			CredentialSpecification credSpec = instance.keyManager.getCredentialSpecification(new URI(credentialSpecificationUid));

			if(credSpec == null) {
				return logger.exit(Response.status(Response.Status.NOT_FOUND).entity(errNoCredSpec).build());
			}
			else
				return logger.exit(Response.ok(of.createCredentialSpecification(credSpec), MediaType.APPLICATION_XML).build());
		} 
		catch(Exception ex) {
			logger.catching(ex);
			return logger.exit(Response.serverError().build());
		}
	}

	/**
	 * This method generates a fresh set of system parameters for the given
	 * security level, expressed as the bitlength of a symmetric key with
	 * comparable security, and cryptographic mechanism. Issuers can generate
	 * their own system parameters, but can also reuse system parameters
	 * generated by a different entity. More typically, a central party (e.g., a
	 * standardization body) will generate and publish system parameters for a
	 * number of different key lengths that will be used by many Issuers.
	 * Security levels 80 and 128 MUST be supported; other values MAY also be
	 * supported.
	 * 
	 * Currently, the supported mechanism URIs are
	 * urn:abc4trust:1.0:algorithm:idemix for Identity Mixer and
	 * urn:abc4trust:1.0:algorithm:uprove for U-Prove.
	 * 
	 * This method will overwrite any existing system parameters.
	 * 
	 * Protected by magic cookie
	 * 
	 * @param magicCookie
	 * @param securityLevel
	 * @param cryptoMechanism
	 * @return
	 * @throws Exception
	 */
	@POST()
	@Path("/setupSystemParameters/{magicCookie}")
	@Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
	public Response setupSystemParameters(
			@PathParam("magicCookie") String magicCookie,
			@QueryParam("securityLevel") int securityLevel,
			@QueryParam("cryptoMechanism") URI cryptoMechanism)
					throws Exception {
		
		logger.entry();
		
		if(!ServicesConfiguration.isMagicCookieCorrect(magicCookie))
			return logger.exit(Response.status(Response.Status.FORBIDDEN).entity(errMagicCookie).build());
		
		try {
			logger.info("IssuanceService - setupSystemParameters "
					+ securityLevel + ", " + cryptoMechanism);
	
			CryptoEngine cryptoEngine = this.parseCryptoMechanism(cryptoMechanism);
	
			this.initializeHelper(cryptoEngine);
	
			IssuanceHelper issuanceHelper = IssuanceHelper.getInstance();
	
			int idemixKeylength = this.parseIdemixSecurityLevel(securityLevel);
	
			int uproveKeylength = this.parseUProveSecurityLevel(securityLevel);
	
			SystemParameters systemParameters = issuanceHelper
					.createNewSystemParametersWithIdemixSpecificKeylength(
							idemixKeylength, uproveKeylength);
	
			SystemParameters serializeSp = SystemParametersUtil
					.serialize(systemParameters);
			
			return logger.exit(
					Response.ok(this.of.createSystemParameters(serializeSp), 
							MediaType.APPLICATION_XML).build());
		}
		catch(Exception e) {
			logger.catching(e);
			return logger.exit(Response.serverError().build());
		}
	}

	private int parseIdemixSecurityLevel(int securityLevel) {
		if (securityLevel == 80) {
			return 1024;
		}
		return com.ibm.zurich.idmx.utils.SystemParameters
				.equivalentRsaLength(securityLevel);
	}

	private int parseUProveSecurityLevel(int securityLevel) {
		switch (securityLevel) {
		case 80:
			return 2048;
		case 128:
			return 3072;
		}
		throw new RuntimeException("Unsupported securitylevel: \""
				+ securityLevel + "\"");
	}

	private CryptoEngine parseCryptoMechanism(URI cryptoMechanism) {
		if (cryptoMechanism == null) {
			throw new RuntimeException("No cryptographic mechanism specified");
		}
		if (cryptoMechanism.equals(CRYPTOMECHANISM_URI_IDEMIX)) {
			return CryptoEngine.IDEMIX;
		}
		throw new IllegalArgumentException("Unkown crypto mechanism: \""
				+ cryptoMechanism + "\"");
	}
	
	// H2.1 Update(jdn): added crypto engine.
    /**
     * This method generates a fresh issuance key and the corresponding Issuer
     * parameters. The issuance key is stored in the Issuer’s key store, the
     * Issuer parameters are returned as output of the method. The input to this
     * method specify the credential specification credspec of the credentials
     * that will be issued with these parameters, the system parameters syspars,
     * the unique identifier uid of the generated parameters, the hash algorithm
     * identifier hash, and, optionally, the parameters identifier for any
     * Issuer-driven Revocation Authority.
     * 
     * Currently, the only supported hash algorithm is SHA-256 with identifier
     * urn:abc4trust:1.0:hashalgorithm:sha-256.
     * 
     * Protected by magic cookie.
     * 
     * @return
     * @throws Exception
     */
    /*
     * curl --header "Content-Type:application/xml" -X POST -d @credSpecAndSysParams.xml http://localhost:9500/abce-services/issuer/setupIssuerParameters/?cryptoEngine=IDEMIX\&issuerParametersUid=urn%3A%2F%2Ftest%2Ffoobar\&hash=urn:abc4trust:1.0:hashalgorithm:sha-256
     */
    @POST()
    @Path("/setupIssuerParameters/{magicCookie}")
    @Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML})
    public Response setupIssuerParameters(
    		@PathParam("magicCookie") String magicCookie,
            IssuerParametersInput issuerParametersInput)
                    throws Exception {
    	
    	logger.entry();
		
		if(!ServicesConfiguration.isMagicCookieCorrect(magicCookie))
			return logger.exit(Response.status(Response.Status.FORBIDDEN).entity(errMagicCookie).build());

		try {
	        CryptoEngine cryptoEngine = this
	                .parseCryptoMechanism(issuerParametersInput.getAlgorithmID());
	
	        this.initializeHelper(cryptoEngine);
	
	        this.validateInput(issuerParametersInput);
	        URI hashAlgorithm = issuerParametersInput.getHashAlgorithm();
	
	        String systemAndIssuerParamsPrefix = "";
	
	        IssuanceHelper instance = IssuanceHelper.getInstance();
	
	        KeyManager keyManager = instance.keyManager;
	        SystemParameters systemParameters = keyManager.getSystemParameters();
	
	        URI credentialSpecUid = issuerParametersInput.getCredentialSpecUID();
	        CredentialSpecification credspec = keyManager
	                .getCredentialSpecification(credentialSpecUid);
	
	        if (credspec == null) {
	            return logger.exit(Response.status(Response.Status.NOT_FOUND).entity(errNoCredSpec).build());
	        }
	
	        URI issuerParametersUid = issuerParametersInput.getParametersUID();
	        URI hash = hashAlgorithm;
	        URI revocationParametersUid = issuerParametersInput
	                .getRevocationParametersUID();
	        List<FriendlyDescription> friendlyDescriptions = issuerParametersInput
	                .getFriendlyIssuerDescription();
	        System.out.println("FriendlyIssuerDescription: "
	                + friendlyDescriptions.size());
	        IssuerParameters issuerParameters = instance.setupIssuerParameters(
	                cryptoEngine, credspec, systemParameters,
	                issuerParametersUid, hash, revocationParametersUid,
	                systemAndIssuerParamsPrefix, friendlyDescriptions);
	
	        logger.info("IssuanceService - issuerParameters generated");
	
	        SystemParameters serializeSp = SystemParametersUtil
	                .serialize(systemParameters);
	
	        issuerParameters.setSystemParameters(serializeSp);
	        return logger.exit(Response.ok(this.of.createIssuerParameters(issuerParameters), 
	        		MediaType.APPLICATION_XML).build());
		}
		catch(Exception e) {
			logger.catching(e);
			return logger.exit(Response.serverError().build());
		}
    }

    private void validateInput(IssuerParametersInput issuerParametersTemplate) {
        if (issuerParametersTemplate == null) {
            throw new IllegalArgumentException(
                    "issuer paramters input is required");
        }

        if (issuerParametersTemplate.getCredentialSpecUID() == null) {
            throw new IllegalArgumentException(
                    "Credential specifation UID is required");
        }

        if (issuerParametersTemplate.getParametersUID() == null) {
            throw new IllegalArgumentException(
                    "Issuer parameters UID is required");
        }

        if (issuerParametersTemplate.getAlgorithmID() == null) {
            throw new IllegalArgumentException(
                    "Crypto Algorithm ID is required");
        }

        if (issuerParametersTemplate.getHashAlgorithm() == null) {
            throw new IllegalArgumentException("Hash algorithm is required");
        }

        if (!issuerParametersTemplate.getHashAlgorithm().equals(
                CryptoUriUtil.getHashSha256())) {
            throw new IllegalArgumentException("Unknown hashing algorithm");
        }

    }
    
    /**
     * This method is invoked by the Issuer to initiate an issuance protocol
     * based on the given issuance policy ip and the list of attribute
     * type-value pairs atts to be embedded in the new credential. It returns an
     * IssuanceMessage that is to be sent to the User and fed to the
     * issuanceProtocolStep method on the User’s side. The IssuanceMessage
     * contains a Context attribute that will be the same for all message
     * exchanges in this issuance protocol, to facilitate linking the different
     * flows of the protocol.
     * 
     * In case of an issuance “from scratch”, i.e., for which the User does not
     * have to prove ownership of existing credentials or established
     * pseudonyms, the given issuance policy ip merely specifies the credential
     * specification and the issuer parameters for the credential to be issued.
     * In this case, the returned issuance message is the first message in the
     * actual cryptographic issuance protocol.
     * 
     * In case of an “advanced” issuance, i.e., where the User has to prove
     * ownership of existing credentials or pseudonyms to carry over attributes,
     * a user secret, or a device secret, the returned IssuanceMessage is simply
     * a wrapper around the issuance policy ip with a fresh Context attribute.
     * The returned boolean indicates whether this is the last flow of the
     * issuance protocol. If the IssuanceMessage is not the final one, the
     * Issuer will subsequently invoke its issuanceProtocolStep method on the
     * next incoming IssuanceMessage from the User. The issuer also returns the
     * uid of the stored issuance log entry that contains an issuance token
     * together with the attribute values provided by the issuer to keep track
     * of the issued credentials.
     * 
     * Protected by magic cookie.
     */
    @POST()
    @Path("/initIssuanceProtocol/{magicCookie}")
    @Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML})
    public Response initIssuanceProtocol(
    		@PathParam("magicCookie") String magicCookie,
            IssuancePolicyAndAttributes issuancePolicyAndAttributes)
                    throws Exception {

        logger.entry();
        
        if(!ServicesConfiguration.isMagicCookieCorrect(magicCookie))
			return logger.exit(Response.status(Response.Status.FORBIDDEN).entity(errMagicCookie).build());

        try {
	        IssuancePolicy ip = issuancePolicyAndAttributes.getIssuancePolicy();
	        List<Attribute> attributes = issuancePolicyAndAttributes.getAttribute();
	
	        URI issuerParametersUid = ip.getCredentialTemplate()
	                .getIssuerParametersUID();
	
	        CryptoEngine cryptoEngine = this.getCryptoEngine(issuerParametersUid);
	
	        this.initializeHelper(cryptoEngine);
	
	        this.initIssuanceProtocolValidateInput(issuancePolicyAndAttributes);
	
	        IssuanceHelper issuanceHelper = IssuanceHelper.getInstance();
	
	        /*this.loadCredentialSpecifications();
	
	        this.loadIssuerParameters();*/ //commented out by munt. Apparentely these just load
	        //credSpecs from a preconfigured location on the file system. Our cred specs should already
	        //be in the storage used by the keyManager.
	
	        IssuanceMessageAndBoolean issuanceMessageAndBoolean = issuanceHelper.initIssuanceProtocol(ip, attributes);
	
	        return logger.exit(Response.ok(this.of
	                .createIssuanceMessageAndBoolean(issuanceMessageAndBoolean), MediaType.APPLICATION_XML).build());
        }
        catch(Exception e) {
        	logger.catching(e);
        	return logger.exit(Response.serverError().build());
        }

    }

    private CryptoEngine getCryptoEngine(URI issuerParametersUid) {
        if (issuerParametersUid.toString().endsWith("idemix")) {
            return CryptoEngine.IDEMIX;
        }

        throw new IllegalArgumentException(
                "Unkown crypto engine from issuer parameters uid: \""
                        + issuerParametersUid + "\"");
    }
    
    private void initIssuanceProtocolValidateInput(
            IssuancePolicyAndAttributes issuancePolicyAndAttributes) {
        if (issuancePolicyAndAttributes == null) {
            throw new IllegalArgumentException(
                    "\"issuancePolicyAndAttributes\" is required.");
        }

        if (issuancePolicyAndAttributes.getIssuancePolicy() == null) {
            throw new IllegalArgumentException(
                    "\"Issuance policy\" is required.");
        }

        if (issuancePolicyAndAttributes.getAttribute() == null) {
            throw new IllegalArgumentException("\"Attributes\" are required.");
        }
    }
    
    /**
     * This method performs one step in an interactive issuance protocol. On
     * input an incoming issuance message m received from the User, it returns
     * the outgoing issuance message that is to be sent back to the User, a
     * boolean indicating whether this is the last message in the protocol, and
     * the uid of the stored issuance log entry that contains an issuance token
     * together with the attribute values provided by the issuer to keep track
     * of the issued credentials. The Context attribute of the outgoing message
     * has the same value as that of the incoming message, allowing the Issuer
     * to link the different messages of this issuance protocol.
     */
    @POST()
    @Path("/issuanceProtocolStep")
    @Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML})
    public Response issuanceProtocolStep(
            final IssuanceMessage issuanceMessage) {
    	
    	logger.entry();

        logger.info("IssuanceService - step - context : "
                + issuanceMessage.getContext());
        
        try {
	        CryptoEngine engine = this.getCryptoEngine(issuanceMessage);
	
	        this.initializeHelper(engine);
	
	        IssuanceMessageAndBoolean response;
	        try {
	            response = IssuanceHelper.getInstance().issueStep(engine, issuanceMessage);
	        } catch (Exception e) {
	            logger.info("- got Exception from IssuaceHelper/ABCE Engine - processing IssuanceMessage from user...");
	            e.printStackTrace();
	            throw new IllegalStateException("Failed to proces IssuanceMessage from user");
	        }
	
	        IssuanceMessage issuanceMessageFromResponce = response
	                .getIssuanceMessage();
	        if (response.isLastMessage()) {
	            logger.info(" - last message for context : "
	                    + issuanceMessageFromResponce.getContext());
	        } else {
	            logger.info(" - more steps context : "
	                    + issuanceMessageFromResponce.getContext());
	        }
	
	        return logger.exit(Response.ok(this.of.createIssuanceMessageAndBoolean(response),
	        		MediaType.APPLICATION_XML).build());
        }
        catch(Exception e) {
        	logger.catching(e);
        	return logger.exit(Response.serverError().build());
        }
    }

    private CryptoEngine getCryptoEngine(final IssuanceMessage issuanceMessage) {
        CryptoEngine engine = CryptoEngine.IDEMIX;

        if (issuanceMessage.getAny().get(0) instanceof JAXBElement) {
            engine = CryptoEngine.IDEMIX;
            return engine;
        }
        throw new RuntimeException("We only support idemix. Sorry :(");
    }

	/* END SECTION */
}
