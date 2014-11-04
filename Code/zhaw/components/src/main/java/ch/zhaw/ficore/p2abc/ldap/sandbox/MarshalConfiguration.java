package ch.zhaw.ficore.p2abc.ldap.sandbox;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import ch.zhaw.ficore.p2abc.configuration.ConnectionParameters;
import ch.zhaw.ficore.p2abc.configuration.IssuanceConfiguration;
import ch.zhaw.ficore.p2abc.configuration.IssuanceConfiguration.IdentitySource;
import ch.zhaw.ficore.p2abc.configuration.ServicesConfiguration;

public class MarshalConfiguration {

    private static final String CONFIGURATION_FILE = "configuration.xml";

    public static void main(String[] args) throws FileNotFoundException {
        ConnectionParameters cp = new ConnectionParameters("localhost", 10930, 10930, 10930, "me", "</configuration>", false);
        IssuanceConfiguration ic = new IssuanceConfiguration(IdentitySource.LDAP, cp, IdentitySource.KEYROCK, cp, "(cn=_UID_)");
        ServicesConfiguration.setIssuanceConfiguration(ic);
        
        PrintStream ps = new PrintStream(CONFIGURATION_FILE);
        ServicesConfiguration.saveTo(ps);
        
        File f = new File(CONFIGURATION_FILE);
        ServicesConfiguration.loadFrom(f);
        
        IssuanceConfiguration newConfig = ServicesConfiguration.getIssuanceConfiguration();
        assert newConfig.getAttributeSource() == IdentitySource.LDAP;
    }

}
