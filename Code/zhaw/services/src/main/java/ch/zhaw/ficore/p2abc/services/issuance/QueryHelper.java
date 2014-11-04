package ch.zhaw.ficore.p2abc.services.issuance;

public class QueryHelper {
	
	private QueryHelper() {
		
	}
	
	public static String buildQuery(String query, String uid) {
		return query.replaceAll("_UID_", uid.replaceAll("_", "__")).replaceAll("__", "_");
	}
	
	public static String ldapSanitize(String input) {
		//TODO: Read the ldap RFC to correctly sanitize strings....
		return input.replaceAll("[^a-zA-Z]", "");
	}
}
