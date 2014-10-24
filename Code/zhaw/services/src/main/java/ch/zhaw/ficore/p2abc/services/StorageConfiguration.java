package ch.zhaw.ficore.p2abc.services;

/**
 * Abstract class to hold StorageConfigurations.
 * 
 * @author mroman
 */
public abstract class StorageConfiguration implements Cloneable {
	public StorageConfiguration clone() throws CloneNotSupportedException {
		return this;
	}
}