package ch.zhaw.ficore.p2abc.services.issuance;

import org.apache.commons.lang3.builder.ToStringBuilder;

import ch.zhaw.ficore.p2abc.helper.ConnectionParameters;

/** Data container for issuance service configuration.
 * 
 * At the moment, the issuance service offers two identity sources, LDAP and
 * Keyrock. For LDAP, no transport protocol can be configured, so it's up to
 * the LDAP issuer to decide what transport protocol to use.
 * 
 * @author Stephan Neuhaus &lt;stephan.neuhaus@zhaw.ch&gt;
 * @version 1.0
 */
public class IssuanceConfigurationData {
  /** What source will we be using for attributes or authentication? */
  public enum IdentitySource {
    KEYROCK,  /** Use keyrock. */
    LDAP,     /** Use LDAP. */
    FAKE,     /** Use a fake (testing only). */
  }

  //private Logger logger;

  /** What attribute source should we use? */
  private IdentitySource attributeSource;
  
  /** How to connect to the attribute provider? */
  private ConnectionParameters attributeConnectionParameters;
  
  /** What authentication source should we use? */
  private IdentitySource authenticationSource;
  
  /** How to connect to the authentication provider? */
  private ConnectionParameters authenticationConnectionParameters;
  
  /** The binding query. */
  private String bindQuery;
  
  /** Constructs an empty issuance configuration.
   * 
   *  @warning: {#isGood()} will return <code>false</code> when using
   *  this constructor.
   */
  public IssuanceConfigurationData() {
    //logger = LogManager.getLogger();
  }
  
  
  public IssuanceConfigurationData(IdentitySource attributeSource,
      ConnectionParameters attributeConnectionParameters,
      IdentitySource authenticationSource,
      ConnectionParameters authenticationConnectionParameters,
      String bindQuery) {
    super();
    this.attributeSource = attributeSource;
    this.attributeConnectionParameters = attributeConnectionParameters;
    this.authenticationSource = authenticationSource;
    this.authenticationConnectionParameters = authenticationConnectionParameters;
    this.bindQuery = bindQuery;
  }


  public IdentitySource getAttributeSource() {
    return attributeSource;
  }


  public ConnectionParameters getAttributeConnectionParameters() {
    return attributeConnectionParameters;
  }


  public IdentitySource getAuthenticationSource() {
    return authenticationSource;
  }


  public ConnectionParameters getAuthenticationConnectionParameters() {
    return authenticationConnectionParameters;
  }


  public String getBindQuery() {
    return bindQuery;
  }


  /** Signals that a connection to the attribute provider has been established.
   * 
   * This method causes the stored password for the attribute provider
   * connection to be deleted.
   */
  public void attributeConnectionSucceeded() {
    attributeConnectionParameters.passwordNoLongerNeeded();
  }
  
  /** Signals that a connection to the authentication provider has been established.
   * 
   * This method causes the stored password for the authentication provider
   * connection to be deleted.
   */
  public void authenticationConnectionSucceeded() {
    authenticationConnectionParameters.passwordNoLongerNeeded();
  }
    
  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("attributeSource", attributeSource)
      .append("attributeConnectionParameters", attributeConnectionParameters)
      .append("authenticationSource", authenticationSource)
      .append("authenticationConnectionParameters", authenticationConnectionParameters)
      .toString();
  }
  
  public void setFakeSources() {
    attributeSource = IdentitySource.FAKE;
    authenticationSource = IdentitySource.FAKE;
  }


  public boolean isPlausible() {
    // TODO Auto-generated method stub
    return true;
  }

}

