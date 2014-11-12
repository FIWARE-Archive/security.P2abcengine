package ch.zhaw.ficore.p2abc.configuration;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * StorageConfiguration for an sqlite backend.
 * 
 * @author mroman
 */
@XmlRootElement(name="sqlite-storage-configuration")
public class SqliteStorageConfiguration extends StorageConfiguration {
    
    public SqliteStorageConfiguration() {
    }

}
