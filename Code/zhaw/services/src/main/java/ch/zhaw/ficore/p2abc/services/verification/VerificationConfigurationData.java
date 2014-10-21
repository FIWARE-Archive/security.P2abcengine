package ch.zhaw.ficore.p2abc.services.verification;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.zhaw.ficore.p2abc.services.ConfigurationData;
import ch.zhaw.ficore.p2abc.services.issuance.IssuanceConfigurationData;


public class VerificationConfigurationData implements ConfigurationData {
  private Logger logger;
  
  public VerificationConfigurationData() {
    logger = LogManager.getLogger();
  }
  
  @Override
  public boolean isGood() {
    logger.entry();
    boolean ret = false;
    
    return logger.exit(ret);
  }

  @Override
  public VerificationConfigurationData clone() throws CloneNotSupportedException {
    VerificationConfigurationData ret = (VerificationConfigurationData) super.clone();
    return ret;
  }

}
