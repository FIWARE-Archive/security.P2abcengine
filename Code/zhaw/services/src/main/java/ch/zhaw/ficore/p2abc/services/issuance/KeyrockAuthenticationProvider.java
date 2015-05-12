package ch.zhaw.ficore.p2abc.services.issuance;

import java.net.URLEncoder;

import javax.naming.NamingException;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ch.zhaw.ficore.p2abc.configuration.IssuanceConfiguration;
import ch.zhaw.ficore.p2abc.services.helpers.RESTHelper;
import ch.zhaw.ficore.p2abc.xml.AuthInfoKeyrock;
import ch.zhaw.ficore.p2abc.xml.AuthenticationInformation;

/**
 * An AuthenticationProvider that is not coupled with any actual identity
 * source. Use this for testing or as a reference.
 * 
 * @author mroman
 */
public class KeyrockAuthenticationProvider extends AuthenticationProvider {

    private static final XLogger logger = new XLogger(
            LoggerFactory.getLogger(KeyrockAuthenticationProvider.class));
    private String userId;

    /**
     * Constructor
     * 
     * @param configuration
     *            Configuration (Issuance)
     */
    public KeyrockAuthenticationProvider(IssuanceConfiguration configuration) {
        super(configuration);
    }

    /**
     * No operation.
     */
    public void shutdown() {

    }

    /**
     * Performs the authentication. Uses a dummy hardcoded combination of a
     * username "CaroleKing" and "Jazzman" as the password.
     * 
     * @throws NamingException
     */
    public boolean authenticate(AuthenticationInformation authInfo)
            throws NamingException {
        logger.info("keyrock auth");

        if (!(authInfo instanceof AuthInfoKeyrock))
            return false;

        AuthInfoKeyrock keyrockAuth = (AuthInfoKeyrock) authInfo;

        try {

            String json = (String) RESTHelper
                    .getRequestUnauth("https://account.lab.fiware.org/user?access_token="
                            + URLEncoder.encode(keyrockAuth.accessToken,
                                    "UTF-8"));
            JSONObject result = (JSONObject) JSONValue.parse(json);

            logger.info(json);

            userId = DigestUtils.sha1Hex((String) result.get("email"));
            logger.info(userId);

            return true;

        } catch (Exception e) {
            logger.catching(e);
            return false;
        }
    }

    public String getUserID() {
        return userId;
    }
}