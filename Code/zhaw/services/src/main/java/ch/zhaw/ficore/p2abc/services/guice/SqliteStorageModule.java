package ch.zhaw.ficore.p2abc.services.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

import eu.abc4trust.keyManager.KeyStorage;

import ch.zhaw.ficore.p2abc.storage.*;
import ch.zhaw.ficore.p2abc.services.*;

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
		}
		catch(Exception e) {
			//TODO: What to do when config fails?? -- munt
			e.printStackTrace();
			System.exit(-1);
		}
	}
}