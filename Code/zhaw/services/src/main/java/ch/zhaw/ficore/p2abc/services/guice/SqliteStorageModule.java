package ch.zhaw.ficore.p2abc.services.guice;

import ch.zhaw.ficore.p2abc.configuration.SqliteStorageConfiguration;
import ch.zhaw.ficore.p2abc.services.ServiceType;
import ch.zhaw.ficore.p2abc.services.issuance.GenericIssuanceStorage;
import ch.zhaw.ficore.p2abc.services.issuance.IssuanceStorage;
import ch.zhaw.ficore.p2abc.storage.GenericIssuerCredentialStorage;
import ch.zhaw.ficore.p2abc.storage.GenericUserCredentialStorage;
import ch.zhaw.ficore.p2abc.storage.GenericKeyStorage;
import ch.zhaw.ficore.p2abc.storage.GenericSecretStorage;
import ch.zhaw.ficore.p2abc.storage.GenericTokenStorageIssuer;
import ch.zhaw.ficore.p2abc.storage.SqliteURIBytesStorage;
import ch.zhaw.ficore.p2abc.storage.URIBytesStorage;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

public class SqliteStorageModule extends AbstractModule {
	
	private SqliteStorageConfiguration configuration;
	private ServiceType type;
	
	public SqliteStorageModule(SqliteStorageConfiguration configuration, ServiceType type) {
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
		    /**
		     * REM: Some tables are only required by one party (and not by both)
		     * which means that unused tables will be created. This, of course, isn't an issue
		     * (it will just create for example user_keyStorage which will always be empty).
		     * Once more is known about storage and testing has been done we might do some switch
		     * cases here for service type to not create empty tables but again, this is not high priority
		     * yet because empty tables don't hurt anybody. -- munt
		     */
		    
			this.bind(URIBytesStorage.class)
				.annotatedWith(Names.named("keyStorage"))
				.toInstance(new SqliteURIBytesStorage(file, name + "_" + "keyStorage"));
			this.bind(eu.abc4trust.keyManager.KeyStorage.class).
			    to(GenericKeyStorage.class).in(Singleton.class);
			
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
			this.bind(eu.abc4trust.abce.internal.issuer.tokenManagerIssuer.TokenStorageIssuer.class)
			    .to(GenericTokenStorageIssuer.class).in(Singleton.class);
			
			this.bind(URIBytesStorage.class)
			    .annotatedWith(Names.named("secretStorage"))
			    .toInstance(new SqliteURIBytesStorage(file, name + "_" + "secretStorage"));
			this.bind(eu.abc4trust.abce.internal.user.credentialManager.SecretStorage.class)
			    .to(GenericSecretStorage.class).in(Singleton.class);
			
			this.bind(URIBytesStorage.class)
			    .annotatedWith(Names.named("issuerSecretKeyStorage"))
			    .toInstance(new SqliteURIBytesStorage(file, name + "_" + "issuerSecretKeyStorage"));
			this.bind(eu.abc4trust.abce.internal.issuer.credentialManager.CredentialStorage.class)
			    .to(GenericIssuerCredentialStorage.class).in(Singleton.class);
			
			this.bind(URIBytesStorage.class)
			    .annotatedWith(Names.named("credentialStorage"))
			    .toInstance(new SqliteURIBytesStorage(file, name + "_" + "credentialStorage"));
			this.bind(URIBytesStorage.class)
			    .annotatedWith(Names.named("pseudonymStorage"))
			    .toInstance(new SqliteURIBytesStorage(file, name + "_" + "pseudonymStorage"));
			this.bind(eu.abc4trust.abce.internal.user.credentialManager.CredentialStorage.class)
			    .to(GenericUserCredentialStorage.class).in(Singleton.class);
			
		}
		catch(Exception e) {
			//TODO: What to do when config fails?? -- munt
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
