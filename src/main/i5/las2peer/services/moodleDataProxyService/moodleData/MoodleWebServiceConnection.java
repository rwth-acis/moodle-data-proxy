package i5.las2peer.services.moodleDataProxyService.moodleData;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
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

import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleAssignSubmission;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleCourse;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleUserData;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleUserGradeItem;
import i5.las2peer.services.moodleDataProxyService.moodleData.xAPIStatements.xAPIStatements;

public class MoodleWebServiceConnection {
	private static String token = null;
	private static String domainName = null;
	private static String restFormat = "&moodlewsrestformat=json";

	/**
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
	 */
	private String restRequest(String functionName, String urlParameters) throws ProtocolException, IOException {
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
	 */
	public String core_course_get_courses() throws ProtocolException, IOException {
		return restRequest("core_course_get_courses", null);
	}

	/**
	 * @param courseId This is Id of the course you want to have enrolled users of
	 * @return Returns enrolled users for specified course
	 */
	public String core_enrol_get_enrolled_users(int courseId) throws ProtocolException, IOException {
		String urlParameters = "courseid=" + URLEncoder.encode(Integer.toString(courseId), "UTF-8");
		return restRequest("core_enrol_get_enrolled_users", urlParameters);
	}

	public String core_course_get_updates_since(int courseId, long since) throws ProtocolException, IOException {
		String urlParameters = "courseid=" + URLEncoder.encode(Integer.toString(courseId), "UTF-8");
		urlParameters += "&since=" + URLEncoder.encode(Long.toString(since), "UTF-8");
		return restRequest("core_course_get_updates_since", urlParameters);
	}

	public String mod_assign_get_submissions(int assignmentId) throws ProtocolException, IOException {
		String urlParameters = "assignmentids[0]=" + URLEncoder.encode(Integer.toString(assignmentId), "UTF-8");
		return restRequest("mod_assign_get_submissions", urlParameters);
	}

	/**
	 * @param userId This is Id of the user you want to have the courses of
	 * @return Returns courses where the specified user is enrolled in
	 */
	public String core_enrol_get_users_courses(int userId) throws ProtocolException, IOException {
		String urlParameters = "userid=" + URLEncoder.encode(Integer.toString(userId), "UTF-8");
		return restRequest("core_enrol_get_users_courses", urlParameters);
	}

	/**
	 * @param courseId This is Id of the course you want to have grades of
	 * @return Returns grades for all users, who are enrolled in the specified course
	 */
	public String gradereport_user_get_grade_items(int courseId) throws ProtocolException, IOException {

		String urlParameters = "courseid=" + URLEncoder.encode(Integer.toString(courseId), "UTF-8");
		return restRequest("gradereport_user_get_grade_items", urlParameters);
	}

	/**
	 * @param courseId This is Id of the course you want to have grades of
	 * @param userId This is Id of the user you want to have grades of
	 * @return Returns grades for the specified course and user
	 */
	public String gradereport_user_get_grade_items(int courseId, int userId) throws ProtocolException, IOException {
		String urlParameters = "courseid=" + URLEncoder.encode(Integer.toString(courseId), "UTF-8") + "&userid="
				+ URLEncoder.encode(Integer.toString(userId), "UTF-8");
		return restRequest("gradereport_user_get_grade_items", urlParameters);
	}

	/**
	 * @param attemptId This is Id of the course you want to have grades of
	 * @return Returns quiz information for the specified course
	 */
	public String mod_quiz_get_attempt_review(int attemtId) throws ProtocolException, IOException {
		String urlParameters = "attemptid=" + URLEncoder.encode(Integer.toString(attemtId), "UTF-8");
		return restRequest("mod_quiz_get_attempt_review", urlParameters);
	}

	/**
	 * @param courseId This is Id of the course you want to have grades of
	 * @return Returns quiz information for the specified course
	 */
	public String mod_quiz_get_quizzes_by_courses(int courseId) throws ProtocolException, IOException {
		String urlParameters = "courseids[0]=" + URLEncoder.encode(Integer.toString(courseId), "UTF-8");
		return restRequest("mod_quiz_get_quizzes_by_courses", urlParameters);
	}

	/**
	 * @param courseId This is Id of the course you want to the summary of
	 * @param courses This is moodle data in json format for course information
	 * @return a summary of the course
	 */
	public String getCourseSummaryById(Integer courseId, String courses) {
		JSONArray jsonCourses = new JSONArray(courses);
		String courseSummary = null;
		// course summary
		for (Object ob : jsonCourses) {
			JSONObject jsonCourse = (JSONObject) ob;
			if (jsonCourse.getInt("id") == courseId && jsonCourse.get("summary") != JSONObject.NULL) {
				courseSummary = jsonCourse.getString("summary").replaceAll("<.*?>", "");
			}
		}
		return courseSummary;
	}

	/**
	 * @param gradereport This is moodle data in json format for the grades
	 * @param userinfo This is moodle data in json format for the user information
	 * @param quizzes This is moodle data in json format for quiz information
	 * @param courses This is moodle data in json format for course information
	 * @return Returns an ArrayList of statements
	 */
	public ArrayList<String> statementGenerator(String gradereport, String userinfo, String quizzes, String courses)
			throws JSONException {
		ArrayList<String> statements = new ArrayList<>();

		JSONObject jsonGradeReport = new JSONObject(gradereport);
		JSONArray jsonUserGrades = (JSONArray) jsonGradeReport.get("usergrades");
		JSONArray jsonUserInfo = new JSONArray(userinfo);
		JSONObject jsonModQuiz = new JSONObject(quizzes);
		JSONArray jsonQuizzes = (JSONArray) jsonModQuiz.get("quizzes");
		JSONArray jsonCourse = new JSONArray(courses);

		Integer courseId;
		String userFullName;
		Integer userId;
		String email;
		String courseName;
		String courseSummary;
		MoodleUserData moodleUserData;
		MoodleUserGradeItem moodleUserGradeItem;

		for (int i = 0; i < jsonUserGrades.length(); i++) {

			moodleUserData = new MoodleUserData();
			courseId = null;
			userFullName = null;
			userId = null;
			email = null;
			courseName = null;
			courseSummary = null;

			JSONObject jsonUser = (JSONObject) jsonUserGrades.get(i);

			courseId = jsonUser.getInt("courseid");
			moodleUserData.setCourseId(courseId);

			MoodleCourse moodleCourse = statementGeneratorCourse(jsonCourse, courseId);
			moodleUserData.setMoodleCourse(moodleCourse);

			courseSummary = getCourseSummaryById(courseId, courses);
			moodleUserData.setCourseSummary(courseSummary);

			userFullName = jsonUser.getString("userfullname");
			moodleUserData.setUserFullName(userFullName);

			userId = jsonUser.getInt("userid");
			moodleUserData.setUserId(userId);
			// get email
			email = statementGeneratorGetEmail(jsonUserInfo, userId);
			if (email == null) {
				email = userFullName.replace(" ", ".") + userId + "@example.com";
			}
			moodleUserData.setEmail(email);

			// get course name
			courseName = statementGeneratorGetCourseName(jsonUserInfo, userId, courseId);
			moodleUserData.setCourseName(courseName);

			JSONArray jsonGradeItems = (JSONArray) jsonUser.get("gradeitems");

			for (int j = 0; j < jsonGradeItems.length() - 1; j++) {
				moodleUserGradeItem = new MoodleUserGradeItem();
				statements = statementGeneratorGetGrades(j, jsonGradeItems, jsonQuizzes, moodleUserData, statements,
						moodleUserGradeItem);
			} // end of loop jsonGradeItems
		} // end of loop jsonUserGrades

		return statements;
	}

	/*
	 * Splitted previously implemented statementGenerator
	 * This function retrieves the email of the student
	 */
	private String statementGeneratorGetEmail(JSONArray jsonUserInfo, Integer userId) {
		String email = "";
		for (int k = 0; k < jsonUserInfo.length(); k++) {
			JSONObject jsonInfo = (JSONObject) jsonUserInfo.get(k);
			if (jsonInfo.getInt("id") == userId) {
				if (jsonInfo.get("email") != JSONObject.NULL)
					email = jsonInfo.getString("email");
				break;
			}
		}
		return email;
	}

	/*
	 * Splitted previously implemented statementGenerator
	 * This function retrieves the course names of the student
	 */
	private String statementGeneratorGetCourseName(JSONArray jsonUserInfo, Integer userId, Integer courseId) {
		String courseName = null;

		for (int k = 0; k < jsonUserInfo.length(); k++) {
			JSONObject jsonInfo = (JSONObject) jsonUserInfo.get(k);
			if (jsonInfo.getInt("id") == userId) {
				JSONArray jsonEnrolled = (JSONArray) jsonInfo.get("enrolledcourses");
				for (int l = 0; l < jsonUserInfo.length(); l++) {
					JSONObject jsonEnrolledCourse = (JSONObject) jsonEnrolled.get(l);
					if (jsonEnrolledCourse.getInt("id") == courseId) {
						courseName = jsonEnrolledCourse.getString("fullname");
						break;
					}
				}

			}
		}
		return courseName;
	}

	/*
	 * Splitted previously implemented statementGenerator
	 * This function retrieves the summary of quizzes
	 */
	private String statementGeneratorGetQuizzes(JSONArray jsonQuizzes, Integer courseId, Integer itemId) {
		String quizSummary = null;
		for (Object ob : jsonQuizzes) {
			JSONObject jsonQuiz = (JSONObject) ob;
			if (jsonQuiz.getInt("course") == courseId && jsonQuiz.getInt("coursemodule") == itemId
					&& jsonQuiz.get("intro") != JSONObject.NULL) {
				quizSummary = jsonQuiz.getString("intro");
			}
		}
		return quizSummary;
	}

	/*
	 * Splitted previously implemented statementGenerator
	 * This function takes into account the grade items
	 */
	private ArrayList<String> statementGeneratorGetGrades(int index, JSONArray jsonGradeItems, JSONArray jsonQuizzes,
			MoodleUserData moodleUserData, ArrayList<String> statements, MoodleUserGradeItem moodleUserGradeItem) {
		String itemName = null;
		Integer itemId = null;
		String itemModule = null;
		String gradeDateSubmitted = null;
		Double percentageFormatted = null;
		String feedback = null;
		String quizSummary = null;
		double gradeMin = 0.0;
		double gradeMax = 0.0;
		double gradeRaw = 0.0;

		JSONObject jsonItem = (JSONObject) jsonGradeItems.get(index);

		itemName = !jsonItem.isNull("itemname") ? jsonItem.getString("itemname") : "";
		moodleUserGradeItem.setItemname(itemName);

		itemId = jsonItem.getInt("id");
		moodleUserGradeItem.setId(itemId);

		quizSummary = statementGeneratorGetQuizzes(jsonQuizzes, moodleUserData.getCourseId(), itemId);
		moodleUserData.setQuizSummary(quizSummary);

		itemModule = !jsonItem.isNull("itemmodule") ? jsonItem.getString("itemmodule") : "";
		moodleUserGradeItem.setItemmodule(itemModule);

		if (jsonItem.get("gradedatesubmitted") != JSONObject.NULL) {
			/*
			Date date = new Date();
			date.setTime(jsonItem.getLong("gradedatesubmitted") * 1000);
			
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
			gradeDateSubmitted = sdf.format(date) + "Z";
			*/
			moodleUserGradeItem.setGradedatesubmitted(jsonItem.getLong("gradedatesubmitted"));
		}

		// get rid of % and shift the format to 0.XXXX
		if (jsonItem.get("percentageformatted") != JSONObject.NULL
				&& !jsonItem.getString("percentageformatted").equals("-")) {
			double d = Double.parseDouble(jsonItem.getString("percentageformatted").replaceAll("%", "").trim());
			d = d / 100;
			percentageFormatted = d;
		}
		moodleUserGradeItem.setPercentageformatted(Double.toString(percentageFormatted));

		// get rid of <p></p>
		if (jsonItem.get("feedback") != JSONObject.NULL) {
			feedback = jsonItem.getString("feedback").replaceAll("<p>", "").replaceAll("</p>", "");
		}
		moodleUserGradeItem.setFeedback(feedback);

		if (jsonItem.get("grademin") != JSONObject.NULL) {
			gradeMin = jsonItem.getDouble("grademin");
		}
		moodleUserGradeItem.setGrademin(gradeMin);

		if (jsonItem.get("grademax") != JSONObject.NULL) {
			gradeMax = jsonItem.getDouble("grademax");
		}
		moodleUserGradeItem.setGrademax(gradeMax);

		if (jsonItem.get("graderaw") != JSONObject.NULL) {
			gradeRaw = jsonItem.getDouble("graderaw");
		}
		moodleUserGradeItem.setGraderaw(gradeRaw);

		moodleUserData.setMoodleUserGradeItem(moodleUserGradeItem);

		// Creating xAPI Statements
		if (percentageFormatted != null) {
			statements = xAPIStatements.createXAPIStatements(moodleUserData, statements, domainName);
		}
		return statements;
	}

	private MoodleCourse statementGeneratorCourse(JSONArray jsonCourses, Integer courseId) {
		MoodleCourse moodleCourse = new MoodleCourse();
		for (Object ob : jsonCourses) {
			JSONObject jsonCourse = (JSONObject) ob;
			if (jsonCourse.getInt("id") == courseId) {
				moodleCourse.setCategoryId(jsonCourse.getInt("categoryid"));
				moodleCourse.setStartDate(jsonCourse.getLong("startdate"));
				moodleCourse.setEndDate(jsonCourse.getLong("enddate"));

				Date d = new Date();
				DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");

				if (moodleCourse.getStartDate() != null && moodleCourse.getEndDate() != null) {
					long startDate = moodleCourse.getStartDate().longValue() * 1000;
					Instant instant = Instant.ofEpochMilli(startDate);
					System.out.println(fmt.format(instant.atZone(ZoneId.systemDefault())));

					Date courseStartDate = Date.from(instant);

					long endDate = moodleCourse.getEndDate().longValue() * 1000;
					Instant instantEndDate = Instant.ofEpochMilli(endDate);
					System.out.println(fmt.format(instantEndDate.atZone(ZoneId.systemDefault())));

					Date courseEndDate = Date.from(instantEndDate);

					long diff = courseEndDate.getTime() - courseStartDate.getTime();

					int diffDays = (int) (diff / (24 * 60 * 60 * 1000));
					System.out.println("difference between days: " + diffDays);
					moodleCourse.setDuration(diffDays);

				} else {
					moodleCourse.setDuration(0);
				}
				moodleCourse.setFullName(jsonCourse.getString("fullname"));
			}
		}
		return moodleCourse;
	}

	public MoodleUserData getMoodleUserData(JSONArray jsonUserInfo, JSONObject uGrades) {
		for (int k = 0; k < jsonUserInfo.length(); k++) {
			JSONObject user = jsonUserInfo.getJSONObject(k);
			if (user.getInt("id") == uGrades.getInt("userid")) {
				MoodleUserData u = new MoodleUserData();
				u.setEmail(user.getString("email"));
				u.setUserId(user.getInt("id"));
				u.setUserFullName(user.getString("fullname"));
				return u;
			}
		}
		// not found :(
		return null;
	}

	public MoodleAssignSubmission getUserSubmission(JSONArray assignSubmissions, int userId) {
		for (int k = 0; k < assignSubmissions.length(); k++) {
			JSONObject submission = assignSubmissions.getJSONObject(k);
			if (submission.getInt("userid") == userId) {
				MoodleAssignSubmission mas = new MoodleAssignSubmission();
				mas.setId(submission.getInt("id"));
				mas.setUserid(userId);
				mas.setTimecreated(submission.getLong("timecreated"));
				mas.setTimemodified(submission.getLong("timemodified"));
				return mas;
			}
		}
		return null; // not found :(
	}
}