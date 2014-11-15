package ch.zhaw.ficore.p2abc.services.issuance;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.zhaw.ficore.p2abc.configuration.ConnectionParameters;
import ch.zhaw.ficore.p2abc.configuration.IssuanceConfiguration;
import ch.zhaw.ficore.p2abc.configuration.ServicesConfiguration;
import ch.zhaw.ficore.p2abc.services.issuance.xml.AttributeInfoCollection;

/**
 * An AttributeInfoProvider that is not coupled with any
 * actual identity source. Use in testing or as a reference.
 * 
 * @author mroman
 */
public class JdbcAttributeInfoProvider extends AttributeInfoProvider {
	
    private Logger logger;
    
	/**
	 * Constructor
	 */
	public JdbcAttributeInfoProvider(IssuanceConfiguration configuration) {
		super(configuration);
		logger = LogManager.getLogger();
	}
	
	/**
	 * No Operation.
	 */
	public void shutdown() {
		
	}
	
	/**
	 * Returns a AttributeInfoCollection filled with dummy attributes.
	 * 
	 * @return an AttributeInfoCollection
	 */
	public AttributeInfoCollection getAttributes(String name) {
		AttributeInfoCollection aiCol = new AttributeInfoCollection(name);
		
		Connection conn = null;
		ResultSet rs = null;
        
        try {
        
            ConnectionParameters connParams = ServicesConfiguration.getIssuanceConfiguration().getAttributeConnectionParameters();
            Class.forName(connParams.getDriverString());
            conn = DriverManager.getConnection(connParams.getConnectionString());  
            
            DatabaseMetaData md = conn.getMetaData();
            rs = md.getColumns(null, null, name, null);
            while(rs.next()) {
                logger.info("Name: " + rs.getString(4));
                logger.info("Type (as per java.sql.Types): " + rs.getString(5));
                logger.info("Type (as per TYPE_NAME): " + rs.getString(6));
                logger.info("---");
                
                int type = rs.getInt(5);
                
                if(type == java.sql.Types.VARCHAR) {
                    aiCol.addAttribute(rs.getString(4), "xs:string", "urn:abc4trust:1.0:encoding:string:sha-256");
                }
                else if(type == java.sql.Types.BIGINT || type == java.sql.Types.INTEGER || 
                        type == java.sql.Types.SMALLINT) {
                    aiCol.addAttribute(rs.getString(4), "xs:integer", "urn:abc4trust:1.0:encoding:integer:signed");
                }
                else {
                    throw new RuntimeException("Unknown type: " + type);
                }
            }
            
            return aiCol;
        }
        catch(Exception e) {
            logger.catching(e);
            throw new RuntimeException(e);
        }
        finally {
            if(conn != null)
                try {
                    rs.close();
                    conn.close();
                } catch (SQLException e) {
                    logger.catching(e);
                }
        }
	}
}