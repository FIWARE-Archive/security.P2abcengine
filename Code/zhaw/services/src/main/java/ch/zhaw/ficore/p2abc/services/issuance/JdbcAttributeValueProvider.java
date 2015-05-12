package ch.zhaw.ficore.p2abc.services.issuance;

import java.math.BigInteger;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ch.zhaw.ficore.p2abc.configuration.ConnectionParameters;
import ch.zhaw.ficore.p2abc.configuration.IssuanceConfiguration;
import ch.zhaw.ficore.p2abc.configuration.ServicesConfiguration;
import eu.abc4trust.xml.AttributeDescription;
import eu.abc4trust.xml.AttributeDescriptions;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.IssuancePolicyAndAttributes;
import eu.abc4trust.xml.ObjectFactory;

public class JdbcAttributeValueProvider extends AttributeValueProvider {

    private static final XLogger logger = new XLogger(LoggerFactory.getLogger(JdbcAttributeValueProvider.class));

    private ObjectFactory of;

    public JdbcAttributeValueProvider(IssuanceConfiguration config) {
        super(config);
        of = new ObjectFactory();
    }

    public void shutdown() {

    }

    @SuppressWarnings("resource")
    public List<eu.abc4trust.xml.Attribute> getAttributes(String query,
            String uid, CredentialSpecification credSpec) throws Exception {

        Connection conn = null;
        ResultSet rs = null;
        Statement stmt = null;

        try {

            ConnectionParameters connParams = ServicesConfiguration
                    .getIssuanceConfiguration()
                    .getAttributeConnectionParameters();
            Class.forName(connParams.getDriverString());
            conn = DriverManager
                    .getConnection(connParams.getConnectionString());

            String sqlQuery = QueryHelper.buildQuery(query,
                    QueryHelper.sqlSanitize(uid));

            stmt = conn.createStatement();
            rs = stmt.executeQuery(sqlQuery);

            if (rs.next()) {
                AttributeDescriptions attrDescs = credSpec
                        .getAttributeDescriptions();
                List<AttributeDescription> descriptions = attrDescs
                        .getAttributeDescription();
                IssuancePolicyAndAttributes ipa = of
                        .createIssuancePolicyAndAttributes();
                List<eu.abc4trust.xml.Attribute> attributes = ipa
                        .getAttribute();
                for (AttributeDescription attrDesc : descriptions) {
                    Object value = rs.getString(attrDesc.getType().toString());

                    /*
                     * TODO: We can't support arbitrary types here (yet).
                     * Currently only integer/string are supported
                     */
                    if (attrDesc.getDataType().toString().equals("xs:integer")
                            && attrDesc
                                    .getEncoding()
                                    .toString()
                                    .equals("urn:abc4trust:1.0:encoding:integer:signed")) {
                        value = BigInteger.valueOf((Integer
                                .parseInt(((String) value))));
                    } else if (attrDesc.getDataType().toString()
                            .equals("xs:string")
                            && attrDesc
                                    .getEncoding()
                                    .toString()
                                    .equals("urn:abc4trust:1.0:encoding:string:sha-256")) {
                        value = (String) value.toString();
                    } else {
                        throw new RuntimeException(
                                "Unsupported combination of encoding and dataType!");
                    }

                    eu.abc4trust.xml.Attribute attrib = of.createAttribute();
                    attrib.setAttributeDescription(attrDesc);
                    attrib.setAttributeValue(value);
                    attrib.setAttributeUID(new URI(ServicesConfiguration
                            .getURIBase()
                            + "jdbc:"
                            + attrDesc.getType().toString()));
                    attributes.add(attrib);
                }
                return attributes;
            } else {
                throw new RuntimeException("Didn't get a result :(");
            }
        } 
        
        catch(RuntimeException e) {
        	logger.catching( e);
            throw new RuntimeException(e);
        }
      
        catch (Exception e) {
            logger.catching( e);
            throw new RuntimeException(e);
        } finally {
                try {
                    if (rs != null)
                        rs.close();
                } catch (SQLException e) {
                    logger.catching( e);
                }
                try {
                    if (stmt != null)
                        stmt.close();
                } catch (SQLException e) {
                    logger.catching( e);
                }
                try {
                    if (conn != null)
                    	conn.close();
                } catch (SQLException e) {
                    logger.catching( e);
                }
        }

    }
}
