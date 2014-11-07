package ch.zhaw.ficore.p2abc.configuration;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Parameters for connections to attribute or authentication sources.
 * 
 * This class holds parameters to use when connecting to a standard TCP or UDP
 * service such as LDAP or a SQL database. It is assumed that the information in
 * this class is sufficient to connect.
 * 
 * This class is <em>almost</em> immutable: once connectivity has been
 * established, users of this class should call
 * {@link #passwordNoLongerNeeded()} to erase the memory containing the
 * password.
 * 
 * @author Stephan Neuhaus &lt;stephan.neuhaus@zhaw.ch&gt;
 * @version 1.0
 */
@XmlRootElement(name="connection-parameters")
@XmlAccessorType(XmlAccessType.FIELD)
public class ConnectionParameters {
    private static final int MAX_PORT = (1 << 16) - 1;

    @XmlElement(name="server-name")
    private String serverName;

    @XmlElement(name="server-port")
    private int serverPort;

    @XmlElement(name="authentication-method")
    private String authenticationMethod;

    @XmlElement(name="user")
    private String user;

    @XmlElement(name="password")
    private String password;

    @XmlElement(name="use-tls")
    private boolean useTls;

    public ConnectionParameters() {
        super();
    }

    /**
     * Creates connection parameters.
     * 
     * Uses authentication method "simple" by default.
     * 
     * @param serverName the server to connect to
     * @param serverPort the port number; may be 0 for default
     * @param normalDefaultPort the default port when not using TLS
     * @param tlsDefaultPort the default port when using TLS
     * @param user the user name to use when authenticating
     * @param password the password to use when authenticating
     * @param useTls whether or not to use TLS
     */
    public ConnectionParameters(String serverName, int serverPort,
            int normalDefaultPort, int tlsDefaultPort, String user,
            String password, boolean useTls) {
        super();
        this.serverName = serverName;
        if (serverPort > 0 && serverPort <= MAX_PORT)
            this.serverPort = serverPort;
        else
            this.serverPort = useTls ? tlsDefaultPort : normalDefaultPort;
        this.authenticationMethod = "simple";
        this.user = user;
        this.password = password;
        this.useTls = useTls;
    }

    public String getServerName() {
        return serverName;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getAuthenticationMethod() {
        return authenticationMethod;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public boolean usesTls() {
        return useTls;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("serverName", serverName)
                .append("serverPort", serverPort).append("user", user)
                .append("password", "(withheld)").append("useTls", useTls)
                .toString();
    }
}