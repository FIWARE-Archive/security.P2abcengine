package ch.zhaw.ficore.p2abc.services.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

import eu.abc4trust.keyManager.KeyStorage;

import ch.zhaw.ficore.p2abc.storage.*;

public class SqliteStorageModule extends AbstractModule {
	@Override
	protected void configure() {
		try {
			this.bind(URIBytesStorage.class)
				.annotatedWith(Names.named("keyStorage"))
				.toInstance(new SqliteURIBytesStorage("/tmp/some.db", "sometable"));
			this.bind(KeyStorage.class).to(GenericKeyStorage.class).in(Singleton.class);
		}
		catch(Exception e) {
			//TODO: What to do when config fails?? -- munt
			e.printStackTrace();
			System.exit(-1);
		}
	}
}