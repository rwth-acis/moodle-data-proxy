package i5.las2peer.services.moodleDataProxyService.privacy_control_service_rmi;

import org.json.JSONException;
import org.json.JSONObject;

public class StatementConsentHandler {
	public static enum PurposeCode {
		FORUM_DATA(1),
		VIEWING_DATA(2),
		ASSIGNMENT_DATA(3);
		
		public final int code;
		
		PurposeCode(int code) {
			this.code = code;
		}
	}
	
	public static int getStatementPurposeCode(JSONObject statement) throws JSONException {
		JSONObject verbJSON = statement.getJSONObject("verb");
		JSONObject displayJSON = verbJSON .getJSONObject("display");
		String verbName = displayJSON.getString("en-US");
		
		int retVal;
		switch(verbName) {
		case "posted": case "replied":
			retVal = PurposeCode.FORUM_DATA.code;
			break;
		case "viewed":
			retVal = PurposeCode.VIEWING_DATA.code;
			break;
		case "completed":
			retVal = PurposeCode.ASSIGNMENT_DATA.code;
			break;
		default:
			retVal = 0;
			break;
		}
		
		return retVal;
	}
	
	public static JSONObject replaceUserInfoWithPseudonym(JSONObject statement, String pseudonym) throws JSONException {
		JSONObject actorBefore = statement.getJSONObject("actor");
		
		JSONObject account = actorBefore.getJSONObject("account");
		account.put("name", pseudonym);
		
		JSONObject actorAfter = new JSONObject();
		actorAfter.put("account", account);
		
		statement.put("actor", actorAfter);
		
		return statement;
	}
}
