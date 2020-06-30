package i5.las2peer.services.moodleDataProxyService.moodleData;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MoodleWebServiceConnection {
	private static String token = null;
	private static String domainName = null;
	private static String restFormat = "&moodlewsrestformat=json";

	/**
	 * @param token access token for the moodle instance
	 * @param domainName domain of the moodle instance
	 */
	public MoodleWebServiceConnection(String token, String domainName) {
		MoodleWebServiceConnection.token = token;
		MoodleWebServiceConnection.domainName = domainName;
	}

	public String getDomainName() {
		return domainName;
	}

	/**
	 * This function requests a Rest function to the initiated moodle web server.
	 *
	 * @param functionName This the function name for the moodle rest request.
	 * @param urlParameters These are the parameters in one String for the moodle rest request.
	 * @return Returns the output of the moodle rest request.
	 * @throws IOException if an I/O exception occurs.
	 **/
	private String restRequest(String functionName, String urlParameters) throws IOException {
		// Send request
		String serverUrl = domainName + "/webservice/rest/server.php" + "?wstoken=" + token + "&wsfunction="
				+ functionName + restFormat;

		HttpURLConnection con = (HttpURLConnection) new URL(serverUrl).openConnection();

		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		con.setRequestProperty("Content-Language", "en-US");
		con.setDoOutput(true);
		con.setUseCaches(false);
		con.setDoInput(true);

		DataOutputStream wr = new DataOutputStream(con.getOutputStream());

		if (urlParameters != null) {
			wr.writeBytes(urlParameters);
		}
		wr.flush();
		wr.close();

		// Get Response
		InputStream is = con.getInputStream();
		BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		String line;
		StringBuilder response = new StringBuilder();
		while ((line = rd.readLine()) != null) {
			response.append(line);
			response.append('\r');
		}
		rd.close();
		return response.toString();
	}

	/**
	 * @return Returns the information to all courses in moodle
	 * @throws IOException if an I/O exception occurs.
	 **/
	public JSONArray core_course_get_courses() throws IOException {
		return new JSONArray(restRequest("core_course_get_courses", null));
	}

	/**
	 * @param courseId This is Id of the course you want to have enrolled users of
	 * @return Returns enrolled users for specified course
	 * @throws IOException if an I/O exception occurs.
	 **/
	public JSONArray core_enrol_get_enrolled_users(int courseId) throws IOException {
		String urlParameters = "courseid=" + URLEncoder.encode(Integer.toString(courseId), "UTF-8");
		return new JSONArray(restRequest("core_enrol_get_enrolled_users", urlParameters));
	}

	/**
	 * @return site information and current user information
	 * @throws IOException if an I/O exception occurs.
	 */
	public JSONObject core_webservice_get_site_info() throws IOException {
		return new JSONObject(restRequest("core_webservice_get_site_info", ""));
	}

	/**
	 * @param field which should be checked
	 * @param value of the field
	 * @return user object for given field
	 * @throws IOException if an I/O exception occurs.
	 */
	public JSONObject core_user_get_users_by_field(String field, int value) throws IOException {
		String urlParameters = "field=" + URLEncoder.encode(field, "UTF-8");
		urlParameters += "&values[0]=" + URLEncoder.encode(Integer.toString(value), "UTF-8");
		JSONArray userJSON = new JSONArray(restRequest("core_user_get_users_by_field", urlParameters));
		return userJSON.getJSONObject(0);
	}

	/**
	 * @param cmid specifies the unique coursemodule id
	 * @return module object with the given cmid
	 * @throws IOException if an I/O exception occurs.
	 */
	public JSONObject core_course_get_course_module(int cmid) throws IOException {
		String urlParameters = "cmid=" + URLEncoder.encode(Integer.toString(cmid), "UTF-8");
		JSONObject moduleJSON = new JSONObject(restRequest("core_course_get_course_module", urlParameters));
		return moduleJSON.getJSONObject("cm");
	}

	/**
	 * @param courseId id of the course you want to have updates
	 * @param since long containing the unix timestamp
	 * @return updates since given timestamp
	 * @throws IOException if an I/O exception occurs.
	 */
	public JSONArray core_course_get_updates_since(int courseId, long since) throws IOException {
		String urlParameters = "courseid=" + URLEncoder.encode(Integer.toString(courseId), "UTF-8");
		urlParameters += "&since=" + URLEncoder.encode(Long.toString(since), "UTF-8");
		JSONObject updateJSON = new JSONObject(restRequest("core_course_get_updates_since", urlParameters));
		return updateJSON.getJSONArray("instances");
	}


	/**
	 * @param courseId id of the course you want to have updates
	 * @return entire content list of course containing all modules
	 * @throws IOException if an I/O exception occurs.
	 */
	public JSONArray core_course_get_contents(int courseId) throws IOException {
		String urlParameters = "courseid=" + URLEncoder.encode(Integer.toString(courseId), "UTF-8");
		return new JSONArray(restRequest("core_course_get_contents", urlParameters));
	}

	/**
	 * @param assignmentId id of the assignment you want to get the submissions from.
	 * @return submissions for given id.
	 * @throws IOException if an I/O exception occurs.
	 */
	public JSONArray mod_assign_get_submissions(int assignmentId) throws IOException {
		String urlParameters = "assignmentids[0]=" + URLEncoder.encode(Integer.toString(assignmentId), "UTF-8");
		JSONObject submissionJSON = new JSONObject(restRequest("mod_assign_get_submissions", urlParameters));
		submissionJSON = (JSONObject) ((JSONArray) submissionJSON.getJSONArray("assignments")).get(0);
		return submissionJSON.getJSONArray("submissions");
	}

	/**
	 * @param userId This is Id of the user you want to have the courses of
	 * @return Returns courses where the specified user is enrolled in
	 * @throws IOException if an I/O exception occurs.
	 **/
	public String core_enrol_get_users_courses(int userId) throws IOException {
		String urlParameters = "userid=" + URLEncoder.encode(Integer.toString(userId), "UTF-8");
		return restRequest("core_enrol_get_users_courses", urlParameters);
	}

	/**
	 * @param courseId This is Id of the course you want to have grades of
	 * @return Returns grades for all users, who are enrolled in the specified course
	 * @throws IOException if an I/O exception occurs.
	 **/
	public JSONArray gradereport_user_get_grade_items(int courseId) throws IOException {
		String urlParameters = "courseid=" + URLEncoder.encode(Integer.toString(courseId), "UTF-8");
		JSONObject reportJSON = new JSONObject(restRequest("gradereport_user_get_grade_items", urlParameters));
		return reportJSON.getJSONArray("usergrades");
	}

	/**
	 * @param courseId This is Id of the course you want to have grades of
	 * @param userId This is Id of the user you want to have grades of
	 * @return Returns grades for the specified course and user
	 * @throws IOException if an I/O exception occurs.
	 **/
	public String gradereport_user_get_grade_items(int courseId, int userId) throws IOException {
		String urlParameters = "courseid=" + URLEncoder.encode(Integer.toString(courseId), "UTF-8") + "&userid="
				+ URLEncoder.encode(Integer.toString(userId), "UTF-8");
		return restRequest("gradereport_user_get_grade_items", urlParameters);
	}

	/**
	 * @param attemptId This is Id of the course you want to have grades of
	 * @return Returns quiz information for the specified course
	 * @throws IOException if an I/O exception occurs.
	 */
	public String mod_quiz_get_attempt_review(int attemptId) throws IOException {
		String urlParameters = "attemptid=" + URLEncoder.encode(Integer.toString(attemptId), "UTF-8");
		return restRequest("mod_quiz_get_attempt_review", urlParameters);
	}

	/**
	 * @param courseId This is Id of the course you want to have grades of
	 * @return Returns quiz information for the specified course
	 * @throws IOException if an I/O exception occurs.
	 */
	public JSONObject mod_quiz_get_quizzes_by_courses(int courseId) throws IOException {
		String urlParameters = "courseids[0]=" + URLEncoder.encode(Integer.toString(courseId), "UTF-8");
		return new JSONObject(restRequest("mod_quiz_get_quizzes_by_courses", urlParameters));
	}

	/**
	 * @param courseId This is Id of the course which forums should be returned
	 * @return Returns forum information for the specified course
	 * @throws IOException if an I/O exception occurs.
	 */
	public JSONArray mod_forum_get_forums_by_courses(int courseId) throws IOException {
		String urlParameters = "courseids[0]=" + URLEncoder.encode(Integer.toString(courseId), "UTF-8");
		return new JSONArray(restRequest("mod_forum_get_forums_by_courses", urlParameters));
	}

	/**
	 * @param forumid This is Id of the forum which discussions should be returned
	 * @return Returns discussion information for the specified forum
	 * @throws IOException if an I/O exception occurs.
	 */
	public JSONArray mod_forum_get_forum_discussions(int forumid) throws IOException {
		String urlParameters = "forumid=" + URLEncoder.encode(Integer.toString(forumid), "UTF-8");
		JSONObject forumJSON = new JSONObject(restRequest("mod_forum_get_forum_discussions", urlParameters));
		return forumJSON.getJSONArray("discussions");
	}

	/**
	 * @param discussionid This is Id of the discussion which posts should be returned
	 * @return Returns posts of the specified discussion
	 * @throws IOException if an I/O exception occurs.
	 */
	public JSONArray mod_forum_get_discussion_posts(int discussionid) throws IOException {
		String urlParameters = "discussionid=" + URLEncoder.encode(Integer.toString(discussionid), "UTF-8");
		JSONObject postsJSON = new JSONObject(restRequest("mod_forum_get_discussion_posts", urlParameters));
		return postsJSON.getJSONArray("posts");
	}

	/**
	 * @param courseId This is Id of the course which books should be returned
	 * @return Returns book information for the specified course
	 * @throws IOException if an I/O exception occurs.
	 */
	public JSONObject mod_book_get_books_by_courses(int courseId) throws IOException {
		String urlParameters = "courseids[0]=" + URLEncoder.encode(Integer.toString(courseId), "UTF-8");
		return new JSONObject(restRequest("mod_book_get_books_by_courses", urlParameters));
	}

	/**
	 * @param courseId This is Id of the course which DBs should be returned
	 * @return Returns database information for the specified course
	 * @throws IOException if an I/O exception occurs.
	 */
	public JSONObject mod_data_get_databases_by_courses(int courseId) throws IOException {
		String urlParameters = "courseids[0]=" + URLEncoder.encode(Integer.toString(courseId), "UTF-8");
		return new JSONObject(restRequest("mod_data_get_databases_by_courses", urlParameters));
	}

	/**
	 * @param courseId This is Id of the course which pages should be returned
	 * @return Returns page information for the specified course
	 * @throws IOException if an I/O exception occurs.
	 */
	public JSONObject mod_page_get_pages_by_courses(int courseId) throws IOException {
		String urlParameters = "courseids[0]=" + URLEncoder.encode(Integer.toString(courseId), "UTF-8");
		return new JSONObject(restRequest("mod_page_get_pages_by_courses", urlParameters));
	}
}
