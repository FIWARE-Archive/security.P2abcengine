package ch.zhaw.p2abce.playground;

import java.net.URI;
import java.net.URISyntaxException;

import eu.abc4trust.cryptoEngine.idemix.issuer.IdemixCryptoEngineIssuerImpl;
import eu.abc4trust.cryptoEngine.issuer.CryptoEngineIssuer;
import eu.abc4trust.xml.SystemParameters;

public class Issuance {

  public static void main(String[] args) throws URISyntaxException {
    int securityLevel = 128;
    URI uri = new URI("urn:abc4trust:1.0:algorithm:idemix");
    
    CryptoEngineIssuer issuer = new IdemixCryptoEngineIssuerImpl(null, null, null, null, null, null);
    SystemParameters sp = issuer.setupSystemParameters(securityLevel, uri);
 }

}
