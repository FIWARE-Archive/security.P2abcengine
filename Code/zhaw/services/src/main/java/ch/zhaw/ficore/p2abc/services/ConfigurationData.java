package ch.zhaw.ficore.p2abc.services;

public interface ConfigurationData {
  /** Checks the configuration data for plausibility.
   * 
   * This method checks the itself for plausibility and returns true or false,
   * respectively.  This method should not try to find <em>operational</em>
   * errors such as unreachable database servers or wrong passwords; rather,
   * this method should check for errors in the configuration, such as 
   * missing or implausible configuration parameters. 
   * 
   * @return true if this configuration is good, false otherwise.
   */
  public boolean isGood();
  
  public ConfigurationData clone() throws CloneNotSupportedException;
}
