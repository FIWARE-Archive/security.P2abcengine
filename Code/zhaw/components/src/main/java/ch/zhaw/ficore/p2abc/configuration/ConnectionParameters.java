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

    /** Creates new connection parameters. */
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
    public ConnectionParameters(final String serverName, final int serverPort,
            final int normalDefaultPort, final int tlsDefaultPort,
            final String user, final String password, final boolean useTls) {
        super();
        this.serverName = serverName;
        if (serverPort > 0 && serverPort <= MAX_PORT) {
            this.serverPort = serverPort;
        } else {
            this.serverPort = useTls ? tlsDefaultPort : normalDefaultPort;
        }
        this.authenticationMethod = "simple";
        this.user = user;
        this.password = password;
        this.useTls = useTls;
    }

    public final String getServerName() {
        return serverName;
    }

    public final void setServerName(final String serverName) {
        this.serverName = serverName;
    }

    public final int getServerPort() {
        return serverPort;
    }

    public final void setServerPort(final int serverPort) {
        this.serverPort = serverPort;
    }

    public final String getAuthenticationMethod() {
        return authenticationMethod;
    }

    public final void setAuthenticationMethod(final String authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }

    public final String getUser() {
        return user;
    }

    public final void setUser(final String user) {
        this.user = user;
    }

    public final String getPassword() {
        return password;
    }

    public final void setPassword(final String password) {
        this.password = password;
    }

    public final boolean usesTls() {
        return useTls;
    }

    public final void setUseTls(final boolean useTls) {
        this.useTls = useTls;
    }

    public final boolean getUseTls() {
        return useTls;
    }

    public final void setConnectionString(final String connectionString) {
        this.connectionString = connectionString;
    }

    public final String getConnectionString() {
        return connectionString;
    }

    public final void setDriverString(final String driverString) {
        this.driverString = driverString;
    }

    public final String getDriverString() {
        return driverString;
    }

    @Override
    public final String toString() {
        return new ToStringBuilder(this).append("serverName", serverName)
                .append("serverPort", serverPort).append("user", user)
                .append("password", "(withheld)").append("useTls", useTls)
                .toString();
    }
}
