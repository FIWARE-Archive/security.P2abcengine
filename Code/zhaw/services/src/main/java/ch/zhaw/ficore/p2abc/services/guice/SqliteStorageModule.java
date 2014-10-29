package ch.zhaw.ficore.p2abc.services.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

import eu.abc4trust.keyManager.KeyStorage;
import eu.abc4trust.abce.internal.issuer.tokenManagerIssuer.TokenStorageIssuer;
import eu.abc4trust.abce.internal.user.credentialManager.SecretStorage;

import ch.zhaw.ficore.p2abc.storage.*;
import ch.zhaw.ficore.p2abc.services.*;
import ch.zhaw.ficore.p2abc.services.issuance.*;

public class SqliteStorageModule extends AbstractModule {
	
	private SqliteStorageConfiguration configuration;
	private ServicesConfiguration.ServiceType type;
	
	public SqliteStorageModule(SqliteStorageConfiguration configuration, ServicesConfiguration.ServiceType type) {
		this.configuration = configuration;
		this.type = type;
	}
	
	@Override
	protected void configure() {
		
		String name = "unknown";
		String file = configuration.getDatabaseFilePath();
		
		switch(type) {
		case ISSUANCE:
			name = "issuer";
			break;
		case USER:
			name = "user";
			break;
		case VERIFICATION:
			name = "verifier";
			break;
		}
		
		try {
			this.bind(URIBytesStorage.class)
				.annotatedWith(Names.named("keyStorage"))
				.toInstance(new SqliteURIBytesStorage(file, name + "_" + "keyStorage"));
			this.bind(KeyStorage.class).to(GenericKeyStorage.class).in(Singleton.class);
			
			this.bind(URIBytesStorage.class)
				.annotatedWith(Names.named("issuancePolicyStorage"))
				.toInstance(new SqliteURIBytesStorage(file, name + "_" + "issuancePolicyStorage"));
			this.bind(URIBytesStorage.class)
				.annotatedWith(Names.named("queryRuleStorage"))
				.toInstance(new SqliteURIBytesStorage(file, name + "_" + "queryRuleStorage"));
			this.bind(IssuanceStorage.class).to(GenericIssuanceStorage.class).in(Singleton.class);
			
			this.bind(URIBytesStorage.class)
			    .annotatedWith(Names.named("tokensStorageIssuer"))
			    .toInstance(new SqliteURIBytesStorage(file, name + "_" + "tokensStorageIssuer"));
			this.bind(URIBytesStorage.class)
                .annotatedWith(Names.named("pseudonymsStorageIssuer"))
                .toInstance(new SqliteURIBytesStorage(file, name + "_" + "pseudonymsStorageIssuer"));
			this.bind(URIBytesStorage.class)
                .annotatedWith(Names.named("logStorageIssuer"))
                .toInstance(new SqliteURIBytesStorage(file, name + "_" + "logStorageIssuer"));
			this.bind(TokenStorageIssuer.class).to(GenericTokenStorageIssuer.class).in(Singleton.class);
			
			this.bind(URIBytesStorage.class)
			    .annotatedWith(Names.named("secretStorage"))
			    .toInstance(new SqliteURIBytesStorage(file, name + "_" + "secretStorage"));
			this.bind(SecretStorage.class).to(GenericSecretStorage.class).in(Singleton.class);
			
		}
		catch(Exception e) {
			//TODO: What to do when config fails?? -- munt
			e.printStackTrace();
			System.exit(-1);
		}
	}
}