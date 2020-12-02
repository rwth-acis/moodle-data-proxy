package i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO;

import org.json.JSONObject;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoodleUser extends MoodleDataPOJO {

	// ActorRoles document defines these maps
	private final static Map<String, Integer> ROLE_NAME_TO_ID = new HashMap<String, Integer>() {
		{
			put("student", 1);
			put("manager", 2);
			put("editingteacher", 3);
			put("teacher", 4);
		}
	};
	private final static Map<Integer, String> ROLE_ID_TO_NAME = new HashMap<Integer, String>() {
		{
			put(1, "student");
			put(2, "manager");
			put(3, "editingteacher");
			put(4, "noneditingteacher");
		}
	};

	private int id;
	private String email;
	private final String moodleToken;
	private String fullname;
	// CourseRoles = key : courseID, value : role ID
	private Map<Integer, List<Integer>> courseRoles = new HashMap<>();

	public MoodleUser(JSONObject userdata) {
		if (userdata.isNull("id")) {
			logger.severe("Cannot create MoodleUser: Missing id!");
			this.id = 0;
		} else {
			this.id = userdata.getInt("id");
		}
		if (getUserToken(userdata) == null) {
			logger.severe("Cannot create MoodleUser " + this.id + ": Requiring either idnumber or email!");
			this.moodleToken = "";
		} else {
			this.moodleToken = getUserToken(userdata);
		}
		if (userdata.isNull("email")) {
			logger.warning("Missing expected field email for user " + this.id);
			this.email = "";
		} else {
			this.email = userdata.getString("email");
		}
		if (userdata.isNull("fullname")) {
			logger.warning("Missing expected field fullname for user " + this.id);
			this.fullname = "";
		} else {
			this.fullname = userdata.getString("fullname");
		}
	}

	public static String getRoleName(int roleID) {
		return ROLE_ID_TO_NAME.get(roleID);
	}

	public static int getRoleID(String roleName) {
		return ROLE_NAME_TO_ID.get(roleName);
	}

	public String getMoodleToken() {
		return this.moodleToken;
	}

	public int getID() {
		return id;
	}

	public void setID(int id) {
		this.id = id;
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

	/**
	 * Given a user object, this function returns either the 'idnumber'
	 * which stores the oidc sub, or if this field does not exist returns
	 * the base64 encoded email address
	 * 
	 * @param user User JSONObject from Moodle.
	 * @return Generated token for the user.
	 */
	private String getUserToken(JSONObject user) {
		String token = null;
		try {
			if (user.isNull("idnumber")) {
				if (user.isNull("email")) {
					return null;
				} else {
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

	public List<Integer> getCourseRoles(Integer courseID) {
		return this.courseRoles.get(courseID);
	}

	public void putCourseRoles(Integer courseID, List<Integer> roleID) {
		this.courseRoles.put(courseID, roleID);
	}

	public void putCourseRoles(Integer courseID, JSONArray rolesJSON) {
		List<Integer> tmp = new ArrayList<>();
		try {
			for (Object role : rolesJSON) {
				JSONObject roleJSON = (JSONObject) role;
				String roleName = roleJSON.getString("shortname");
				Integer roleID = MoodleUser.getRoleID(roleName);
				tmp.add(roleID);
			}
			this.courseRoles.put(courseID, tmp);
		} catch (Exception e) {
			logger.severe("Error while parsing user role in MoodleUser: "
				+ e.getStackTrace());
		}
	}

	@Override
	public String toString() {
		return "MoodleUser [email=" + email + ", fullname=" + fullname + ", id=" + id + "]";
	}

}
