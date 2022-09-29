package i5.las2peer.services.moodleDataProxyService.privacy_control_service_rmi;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;


/**
 * POJO for the response to the Privacy Control Service's
 * Data Processing Request.
 *
 */
public class DataProcessingRequestResponse {
	private Set<Integer> purposeCodes;
	private String pseudonym;
	
	
	/**
	 * Constructor for the class, using a String of the JSON response as input.
	 * @param rawResponse String of the Data Processing Request's JSON response.
	 * @throws JSONException If the JSON cannot be parsed according to API.
	 */
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
