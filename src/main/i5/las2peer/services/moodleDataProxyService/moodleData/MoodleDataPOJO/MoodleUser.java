package i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO;

import org.json.JSONObject;
import java.util.Base64;

public class MoodleUser extends MoodleDataPOJO {

	private String email;
	private final String moodleToken;
	private String fullname;
	private String lang;
	private String country;

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
		if (userdata.isNull("country")) {
			logger.warning("Missing expected field country for user " + this.id);
			this.country = "";
		}
		else {
			this.country = userdata.getString("country");
		}
		if (userdata.isNull("lang")) {
			logger.warning("Missing expected field lang for user " + this.id);
			this.lang = "";
		}
		else {
			this.lang = userdata.getString("lang");
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

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
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
}
