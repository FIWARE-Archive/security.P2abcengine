package ch.zhaw.ficore.p2abc.services.user;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.zhaw.ficore.p2abc.services.ConfigurationData;

public class UserConfigurationData implements ConfigurationData {
  private Logger logger;
  
  public UserConfigurationData() {
    logger = LogManager.getLogger();
  }
  
  @Override
  public boolean isGood() {
    logger.entry();
    boolean ret = false;
    
    return logger.exit(ret);
  }

  @Override
  public UserConfigurationData clone() throws CloneNotSupportedException {
    UserConfigurationData ret = (UserConfigurationData) super.clone();
    return ret;
  }

}
