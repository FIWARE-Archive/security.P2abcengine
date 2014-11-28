package ch.zhaw.ficore.p2abc.configuration;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Parameters for connections to attribute or authentication sources.
 * 
 * This class holds parameters to use when connecting to a standard TCP or UDP
 * service such as LDAP or a SQL database. It is assumed that the information in
 * this class is sufficient to connect.
 * 
 * @author Stephan Neuhaus &lt;stephan.neuhaus@zhaw.ch&gt;
 * @version 1.0
 */

public class ConnectionParameters {
    private static final int MAX_PORT = (1 << 16) - 1;

    private String serverName;
    private int serverPort;
    private String authenticationMethod;
    private String user;
    private String password;
    private boolean useTls;
    private String connectionString;
    private String driverString;

    public ConnectionParameters() {
        super();
    }

    /**
     * Creates connection parameters.
     * 
     * Uses authentication method "simple" by default.
     * 
     * @param serverName
     *            the server to connect to
     * @param serverPort
     *            the port number; may be 0 for default
     * @param normalDefaultPort
     *            the default port when not using TLS
     * @param tlsDefaultPort
     *            the default port when using TLS
     * @param user
     *            the user name to use when authenticating
     * @param password
     *            the password to use when authenticating
     * @param useTls
     *            whether or not to use TLS
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

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getAuthenticationMethod() {
        return authenticationMethod;
    }

    public void setAuthenticationMethod(String authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean usesTls() {
        return useTls;
    }

    public void setUseTls(boolean useTls) {
        this.useTls = useTls;
    }

    public boolean getUseTls() {
        return useTls;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public void setDriverString(String driverString) {
        this.driverString = driverString;
    }

    public String getDriverString() {
        return driverString;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("serverName", serverName)
                .append("serverPort", serverPort).append("user", user)
                .append("password", "(withheld)").append("useTls", useTls)
                .toString();
    }
}
