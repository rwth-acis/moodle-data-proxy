package i5.las2peer.services.moodleDataProxyService.privacy_control_service_rmi;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class DataProcessingRequestResponse {
	private Set<Integer> purposeCodes;
	private String pseudonym;
	
	public DataProcessingRequestResponse(String rawResponse) throws JSONException {
		JSONTokener tokener = new JSONTokener(rawResponse);
		JSONObject responseJSON = new JSONObject(tokener);
		
		this.pseudonym = responseJSON.getString("pseudonym");
		
		this.purposeCodes = new HashSet<Integer>();
		JSONArray purposeCodesJSON = responseJSON.getJSONArray("purposes");
		for (Object codeObject : purposeCodesJSON) {
			Integer code = (Integer) codeObject;
			this.purposeCodes.add(code);
		}
	}

	public Set<Integer> getPurposeCodes() {
		return purposeCodes;
	}

	public void setPurposeCodes(Set<Integer> purposeCodes) {
		this.purposeCodes = purposeCodes;
	}

	public String getPseudonym() {
		return pseudonym;
	}

	public void setPseudonym(String pseudonym) {
		this.pseudonym = pseudonym;
	}
}
