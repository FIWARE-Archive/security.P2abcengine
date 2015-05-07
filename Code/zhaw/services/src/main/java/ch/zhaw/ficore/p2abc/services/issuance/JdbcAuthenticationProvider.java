package ch.zhaw.ficore.p2abc.services.issuance;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.NamingException;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ch.zhaw.ficore.p2abc.configuration.ConnectionParameters;
import ch.zhaw.ficore.p2abc.configuration.IssuanceConfiguration;
import ch.zhaw.ficore.p2abc.configuration.ServicesConfiguration;
import ch.zhaw.ficore.p2abc.xml.AuthInfoSimple;
import ch.zhaw.ficore.p2abc.xml.AuthenticationInformation;

/**
 * An AuthenticationProvider that is not coupled with any actual identity
 * source. Use this for testing or as a reference.
 * 
 * @author mroman
 */
public class JdbcAuthenticationProvider extends AuthenticationProvider {

    private static final XLogger logger = new XLogger(LoggerFactory.getLogger(JdbcAuthenticationProvider.class));
    private String userId;

    /**
     * Constructor
     * 
     * @param configuration
     *            Configuration (Issuance)
     */
    public JdbcAuthenticationProvider(IssuanceConfiguration configuration) {
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
     * @throws NamingException 
     */
    public boolean authenticate(AuthenticationInformation authInfo) throws NamingException {
        logger.info("jdbc auth");

        if (!(authInfo instanceof AuthInfoSimple))
            return false;

        AuthInfoSimple simpleAuth = (AuthInfoSimple) authInfo;

        String bindQuery = ServicesConfiguration.getIssuanceConfiguration()
                .getBindQuery();
        
        
        String unameHash = DigestUtils.sha1Hex(simpleAuth.username);
        
        bindQuery = QueryHelper.buildQuery(bindQuery,
                QueryHelper.sqlSanitize(unameHash));

        Connection conn = null;
        ResultSet rs = null;
        Statement stmt = null;

        try {

            ConnectionParameters connParams = ServicesConfiguration
                    .getIssuanceConfiguration()
                    .getAuthenticationConnectionParameters();
            Class.forName(connParams.getDriverString());
            conn = DriverManager
                    .getConnection(connParams.getConnectionString());
            stmt = conn.createStatement();
            rs = stmt.executeQuery(bindQuery);

            String pwHash = null;
            String salt = null;
            String dbHash = "";
            if (rs.next()) {
                dbHash = rs.getString(1);
                salt = rs.getString(2);
            }
            pwHash = DigestUtils.sha1Hex(salt + simpleAuth.password);

            if (pwHash.equals(dbHash)) {
                userId = unameHash;
                return true;
            }

            return false;

        } catch (Exception e) {
            logger.catching( e);
            return false;
        } finally {
            if (conn != null)
                try {
                    rs.close();
                    stmt.close();
                    conn.close();
                } catch (SQLException e) {
                    logger.catching( e);
                }
        }
    }

    public String getUserID() {
        return userId;
    }
}