package ch.zhaw.ficore.p2abc.configuration;

import java.io.File;
import java.io.PrintStream;
import java.nio.charset.Charset;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.zhaw.ficore.p2abc.configuration.IssuanceConfiguration.IdentitySource;


/** Holds the configuration for all the services.
 * 
 * This class, implemented only through static methods, holds the configuration
 * for all the services defined here.  This class can be loaded from and
 * stored to a file, and parts of the configuration can be selectively
 * retrieved and updated.
 * 
 * Note for implementers and other people who add to this class: You cannot
 * employ simple getters and setters on fields, for the simple reason that this
 * class is being used from within a servlet container, which means that there
 * may be several threads trying to get information from or change information
 * in this class.  You will have to get and set entire configurations.
 * 
 * @author Stephan Neuhaus &lt;stephan.neuhaus@zhaw.ch&gt;
 * @version 1.0
 */
@XmlRootElement(name="configuration")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServicesConfiguration {

    /** Configuration data for issuance service. */
    @XmlElement(name="issuance-configuration")
    private IssuanceConfiguration issuanceConfiguration;

    /** Configuration data for verification service. */
    @XmlElement(name="verification-configuration")
    private VerificationConfiguration verificationConfiguration;

    /** Configuration data for user service. */
    @XmlElement(name="user-configuration")
    private UserConfiguration userConfiguration;

    /** Storage configuration. */
    @XmlElements({
        @XmlElement(type=SqliteStorageConfiguration.class, name="sqlite-storage-configuration")
    })
    private StorageConfiguration storageConfiguration;

    private static Logger logger = LogManager.getLogger();

    private static ServicesConfiguration instance = new ServicesConfiguration();
    
    private final static String defaultConfigPath = "/tmp/servicesConfiguration.xml";
    private static String configPath;
    
    static {
        if(System.getProperty("configPath") == null) 
            configPath = defaultConfigPath;
        else
            configPath = System.getProperty("configPath");
        
        /* We set a default configuration here */
        ConnectionParameters cp = new ConnectionParameters("localhost", 10389, 10389, 10389, "uid=admin, ou=system", "secret", false);
        IssuanceConfiguration cfgData = new IssuanceConfiguration(IdentitySource.LDAP, cp, IdentitySource.LDAP, cp, "(cn=_UID_)");
        ServicesConfiguration.setIssuanceConfiguration(cfgData);
        
        /* and replace it with the configuration loaded from a file */
        File f = new File(configPath);
        ServicesConfiguration.loadFrom(f);
    }
    
    /** Magic Cookie.
     * 
     * The Magic Cookie is used for service administration such as changing
     * or reloading the configuration as well as storing or creating
     * CredentialSpecifications and more.
     * 
     * The default value is <b>*magic*</b>. It's <em>HIGHLY</em> recommended
     * to change it to a secure value and to change it frequently.  Please
     * only communicate over secure channels and secure applications when 
     * transmitting the magic cookie. 
     */
    @XmlAttribute(name="magic-cookie")
    private static String magicCookie = "*magic*";
    
    /** URI base
     * The URI base is used as a prefix to URIs for example in the generation
     * of CredentialSpecifications.
     */
    @XmlAttribute(name="uri-base")
    private static String uriBase = "urn:fiware:privacy:";

    /**
     * Verifies the correctness of the magic cookie (i.e. if it matches
     * the one stored in this configuration.)
     * 
     * @param magicCookie The value to check against
     * @return true iff the parameter is the same as the stored cookie.
     */
    public static synchronized boolean isMagicCookieCorrect(String magicCookie) {
        return ServicesConfiguration.magicCookie.equals(magicCookie);
    }

    /**
     * Sets the magic cookie to the given value.
     * 
     * @param magicCookie the new value of the magic cookie.
     */
    public static synchronized void setMagicCookie(String magicCookie) {
        ServicesConfiguration.magicCookie = magicCookie;
    }
    
    /**
     * Returns the magic cookie.
     * 
     * @return magic cookie
     */
    public static synchronized String getMagicCookie() {
        return magicCookie;
    }
    
    /**
     * Returns the URI base
     */
    public static synchronized String getURIBase() {
        return uriBase;
    }

    /** Private do-nothing constructor to prevent construction of instances. */
    public ServicesConfiguration () {
        storageConfiguration = new SqliteStorageConfiguration();
    }


    /**
     * Returns the current storage configuration.
     * 
     * @return the current storage configuration
     */
    public static synchronized StorageConfiguration getStorageConfiguration() {
        logger.entry();
        return logger.exit(instance.storageConfiguration);
    }

    /** Returns the current issuance configuration.
     *  
     * @return the current issuance parameters. 
     */
    public static synchronized IssuanceConfiguration getIssuanceConfiguration() {
        logger.entry();
        return logger.exit(instance.issuanceConfiguration);
    }

    public static synchronized VerificationConfiguration getVerificationConfiguration() {
        logger.entry();
        return logger.exit(instance.verificationConfiguration);
    }

    public static synchronized UserConfiguration getUserConfiguration() {
        logger.entry();
        return logger.exit(instance.userConfiguration);
    }


    /** Replaces the current issuance configuration. 
     * 
     * The new configuration is scrutinised and, if all sanity checks are
     * passed, the old configuration is replaced with the new one.  If there
     * is something wrong with the configuration, the current configuration
     * is retained.
     * 
     * @param newConfig the new configuration
     */
    public static synchronized void setIssuanceConfiguration(IssuanceConfiguration newConfig) {
        logger.entry();

        if (newConfig.isPlausible())
            instance.issuanceConfiguration = newConfig;
        else
            logger.warn("Issuance configuration not plausible, retaining old one");

        logger.exit();
    }

    public static synchronized void setVerificationConfiguration(VerificationConfiguration newConfig) {
        logger.entry();
        instance.verificationConfiguration = newConfig;
        logger.exit();
    }

    public static synchronized void setUserConfiguration(UserConfiguration newConfig) {
        logger.entry();
        instance.userConfiguration = newConfig;
        logger.exit();
    }

    public static synchronized void setFakeIssuanceParameters() {
        logger.entry();
        instance.issuanceConfiguration.setFakeSources();
        // TODO: Set more parameters?
        logger.exit();
    }
    
    public static synchronized void saveTo(File f) {
        try {
            PrintStream ps = new PrintStream(f, "UTF-8");
            saveTo(ps);
            ps.close();
        }
        catch(Exception e) {
            logger.catching(e);
            logger.warn("Error while  trying to save configuration!");
        }
        logger.exit();
    }
    
    public static synchronized void saveTo() {
        File f = new File(configPath);
        saveTo(f);
    }
    
    public static synchronized void saveTo(PrintStream ps) {
        logger.entry();

        logger.info("Saving services configuration to " + ps);

        try {
            JAXBContext jc = JAXBContext.newInstance(ServicesConfiguration.class);
            Marshaller m = jc.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.marshal(instance, ps);
        } catch (JAXBException e) {
            logger.catching(e);
            logger.warn("Error while trying to save configuration to " + ps + "; file may be corrupt!");
        }

        logger.exit();
    }

    public static synchronized void loadFrom(File ps) {
        logger.entry();

        logger.info("Loading services configuration from " + ps);

        try {
            JAXBContext jc = JAXBContext.newInstance(ServicesConfiguration.class);
            Unmarshaller u = jc.createUnmarshaller();
            ServicesConfiguration newConfiguration= (ServicesConfiguration) u.unmarshal(ps);
            if (newConfiguration.isPlausible())
                instance = newConfiguration;
            else
                logger.warn("Services configuration in " + ps + " is not plausible; keeping old configuration");
        } catch (JAXBException e) {
            logger.catching(e);
            logger.warn("Error while trying to load configuration from " + ps + "; keeping old configuration");
        }
        
        logger.exit();
    }
    
    public boolean isPlausible() {
        return true;
    }
}
