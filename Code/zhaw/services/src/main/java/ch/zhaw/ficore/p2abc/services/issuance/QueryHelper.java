package ch.zhaw.ficore.p2abc.services.issuance;

public class QueryHelper {
	
	private QueryHelper() {
		
	}
	
	public static String buildQuery(String query, String uid) {
		return query.replaceAll("_UID_", uid.replaceAll("_", "__")).replaceAll("__", "_");
	}
}
