package i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO;

import org.json.JSONObject;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoodleUser extends MoodleDataPOJO {

	private String email;
	private final String moodleToken;
	private String fullname;
	// CourseRoles = key : courseID, value : role ID
	private Map<Integer, List<Integer>> courseRoles = new HashMap<>();

	public MoodleUser(JSONObject userdata) {
		if (userdata.isNull("id")) {
			logger.severe("Cannot create MoodleUser: Missing id!");
			this.id = 0;
		}
		else {
			this.id = userdata.getInt("id");
		}
		if (getUserToken(userdata) == null) {
			logger.severe("Cannot create MoodleUser " + this.id + ": Requiring either idnumber or email!");
			this.moodleToken = "";
		}
		else {
			this.moodleToken = getUserToken(userdata);
		}
		if (userdata.isNull("email")) {
			logger.warning("Missing expected field email for user " + this.id);
			this.email = "";
		}
		else {
			this.email = userdata.getString("email");
		}
		if (userdata.isNull("fullname")) {
			logger.warning("Missing expected field fullname for user " + this.id);
			this.fullname = "";
		}
		else {
			this.fullname = userdata.getString("fullname");
		}
  }

	public String getMoodleToken() {
		return this.moodleToken;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFullname() {
		return fullname;
	}

	public void setFullname(String fullname) {
		this.fullname = fullname;
	}

	// given a user object, this function returns either the 'idnumber'
	// which stores the oidc sub, or if this field does not exist returns
	// the base64 encoded email address
	private String getUserToken(JSONObject user) {
		String token = null;
		try {
			if (user.isNull("idnumber")) {
				if (user.isNull("email")) {
					return null;
				}
				else {
					token = user.getString("email");
					token = Base64.getEncoder().encodeToString(token.getBytes());
				}
			} else {
				token = user.getString("idnumber");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return token;
	}

	public void putCourseRoles(Integer courseID, List<Integer> roleID) {
		this.courseRoles.put(courseID, roleID);
	}

	public List<Integer> getCourseRoles(Integer courseID) {
		return this.courseRoles.get(courseID);
	}
}
