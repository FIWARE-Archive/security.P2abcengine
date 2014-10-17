package ch.zhaw.ficore.p2abc.services.issuance;

import ch.zhaw.ficore.p2abc.services.issuance.xml.*;

public class FakeAuthenticationProvider extends AuthenticationProvider {
	
	
	public FakeAuthenticationProvider(ServiceConfiguration srvcCfg) {
		super(srvcCfg);
	}
	
	public void shutdown() {
		
	}
	
	public boolean authenticate(AuthenticationInformation authInfo) {
		if(!(authInfo instanceof AuthInfoSimple))
			return false;
		
		AuthInfoSimple simpleAuth = (AuthInfoSimple) authInfo;
		
		if(simpleAuth.username.equals("CaroleKing") && simpleAuth.password.equals("Jazzman"))
			return true;
		
		return false;
	} 
}