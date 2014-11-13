package ch.zhaw.ficore.p2abc.services.issuance;

public class QueryHelper {
	
	private QueryHelper() {
		
	}
	
	public static String buildQuery(String query, String uid) {
	    /*TODO: Provide some mechanism to actually allow someone to use "_UID_" in the query
	     * without being replaced by uid. 
	    */
		return query.replaceAll("_UID_", uid);
	}
	
	public static String ldapSanitize(String input) {
		//TODO: Read the ldap RFC to correctly sanitize strings....
		return input.replaceAll("[^a-zA-Z]", "");
	}
}
