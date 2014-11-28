package ch.zhaw.ficore.p2abc.services.issuance;

import ch.zhaw.ficore.p2abc.configuration.IssuanceConfiguration;
import ch.zhaw.ficore.p2abc.xml.AuthInfoSimple;
import ch.zhaw.ficore.p2abc.xml.AuthenticationInformation;

/**
 * An AuthenticationProvider that is not coupled with any actual identity
 * source. Use this for testing or as a reference.
 * 
 * @author mroman
 */
public class FakeAuthenticationProvider extends AuthenticationProvider {

    /**
     * Constructor
     * 
     * @param configuration Configuration (Issuance)
     */
    public FakeAuthenticationProvider(IssuanceConfiguration configuration) {
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
     */
    public boolean authenticate(AuthenticationInformation authInfo) {
        if (!(authInfo instanceof AuthInfoSimple))
            return false;

        AuthInfoSimple simpleAuth = (AuthInfoSimple) authInfo;

        if (simpleAuth.username.equals("CaroleKing")
                && simpleAuth.password.equals("Jazzman"))
            return true;

        return false;
    }

    public String getUserID() {
        return "CaroleKing";
    }
}