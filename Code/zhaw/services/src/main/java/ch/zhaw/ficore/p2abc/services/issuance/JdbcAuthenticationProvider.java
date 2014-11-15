package ch.zhaw.ficore.p2abc.services.issuance;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.zhaw.ficore.p2abc.configuration.ConnectionParameters;
import ch.zhaw.ficore.p2abc.configuration.IssuanceConfiguration;
import ch.zhaw.ficore.p2abc.configuration.ServicesConfiguration;
import ch.zhaw.ficore.p2abc.services.issuance.xml.AuthInfoSimple;

/**
 * An AuthenticationProvider that is not coupled with any actual
 * identity source. Use this for testing or as a reference.
 * 
 * @author mroman
 */
public class JdbcAuthenticationProvider extends AuthenticationProvider {
	
    private Logger logger;
    
	/**
	 * Constructor
	 */
	public JdbcAuthenticationProvider(IssuanceConfiguration configuration) {
		super(configuration);
		logger = LogManager.getLogger();
	}
	
	/**
	 * No operation.
	 */
	public void shutdown() {
		
	}
	
	/**
	 * Performs the authentication. Uses a dummy hardcoded combination
	 * of a username "CaroleKing" and "Jazzman" as the password.
	 */
	public boolean authenticate(AuthenticationInformation authInfo) {
	    logger.info("jdbc auth");
	    
		if(!(authInfo instanceof AuthInfoSimple))
			return false;
		
		AuthInfoSimple simpleAuth = (AuthInfoSimple) authInfo;
		
		String bindQuery = ServicesConfiguration.getIssuanceConfiguration().getBindQuery();
		bindQuery = QueryHelper.buildQuery(bindQuery, simpleAuth.username);
		
		try {
		
		    ConnectionParameters connParams = ServicesConfiguration.getIssuanceConfiguration().getAuthenticationConnectionParameters();
            Class.forName(connParams.getDriverString());
            Connection conn = DriverManager.getConnection(connParams.getConnectionString());  
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(bindQuery);
            
            String pwHash = DigestUtils.sha1Hex(simpleAuth.password);
            String dbHash = "";
            if(rs.next()) {
                dbHash = rs.getString(1);
            }
            
            if(pwHash.equals(dbHash))
                return true;
            return false;
            
		}
		catch(Exception e) {
		    logger.catching(e);
		    return false;
		}
	}
	
	public String getUserID() {
		return "CaroleKing";
	}
}