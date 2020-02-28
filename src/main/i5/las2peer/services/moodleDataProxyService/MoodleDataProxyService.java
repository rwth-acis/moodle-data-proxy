package i5.las2peer.services.moodleDataProxyService;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;

import i5.las2peer.api.Context;
import i5.las2peer.api.ManualDeployment;
import i5.las2peer.api.Service;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleWebServiceConnection;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleAssignSubmission;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleUserData;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleUserGradeItem;
import i5.las2peer.services.moodleDataProxyService.moodleData.xAPIStatements.xAPIStatements;

/**
 * 
 * This service is for requesting moodle data and creating corresponding xAPI statement. It sends REST requests to
 * moodle on basis of implemented functions in MoodleWebServiceConnection.
 * 
 */
@ManualDeployment
public class MoodleDataProxyService extends Service {

	private String moodleDomain;
	private String moodleToken;

	private static MoodleWebServiceConnection moodle;
	private static ScheduledExecutorService dataStreamThread = null;

	private final static int MOODLE_DATA_STREAM_PERIOD = 60; // Every minute
	private static long lastChecked = 0;
	private static HashSet<Integer> courseList = new HashSet<Integer>();

	private static Map<Integer, ArrayList<String>> oldCourseStatements = new HashMap<Integer, ArrayList<String>>();

	private final static L2pLogger logger = L2pLogger.getInstance(MoodleDataProxyService.class.getName());

	private static Context context = null;
	
	private static String email = "";

	/**
	 * 
	 * Constructor of the Service. Loads the database values from a property file and initiates values for a moodle
	 * connection.
	 * 
	 */
	public MoodleDataProxyService() {
		setFieldValues(); // This sets the values of the configuration file
		if (lastChecked == 0) {
			// Get current time
			TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			Instant instant = timestamp.toInstant();
			lastChecked = instant.getEpochSecond();
		}

		moodle = new MoodleWebServiceConnection(moodleToken, moodleDomain);
		
		if(email.equals("")) {
			try {
			String siteInfoRaw = moodle.core_webservice_get_site_info();
			JSONObject siteInfo = new JSONObject(siteInfoRaw);
			int userId = siteInfo.getInt("userid");
			String currentUserInfoRaw = moodle.core_user_get_users_by_field("id", userId);
			JSONArray currentUserInfo = new JSONArray(currentUserInfoRaw);
			JSONObject u = currentUserInfo.getJSONObject(0);
			email = u.getString("email");
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		
		if (courseList == null || courseList.isEmpty()) {
			try {
				String courses = moodle.core_course_get_courses();
				JSONArray jsonCourse = new JSONArray(courses);
				courseList = new HashSet<Integer>();
				for (Object o : jsonCourse) {
					JSONObject course = (JSONObject) o;
					courseList.add(course.getInt("id"));
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void initMoodleProxy() {
		if (dataStreamThread == null) {
			context = Context.get();
			dataStreamThread = Executors.newSingleThreadScheduledExecutor();
			dataStreamThread.scheduleAtFixedRate(new DataStreamThread(), 0, MOODLE_DATA_STREAM_PERIOD,
					TimeUnit.SECONDS);
		}
	}

	/**
	 * A function that is called by the user to send processed moodle to a mobsos data processing instance.
	 *
	 * @param courseId an integer indicating the id of a moodle course
	 * 
	 * @return a response message if everything went ok
	 * 
	 */
	@Deprecated
	public boolean submitDataForCourse(int courseId) {

		String gradeReport = "";
		String userInfo = "";
		String quizzes = "";
		String courses = "";
		ArrayList<String> newStatements;

		// Getting the moodle data
		try {
			gradeReport = moodle.gradereport_user_get_grade_items(courseId);
			userInfo = moodle.core_enrol_get_enrolled_users(courseId);
			quizzes = moodle.mod_quiz_get_quizzes_by_courses(courseId);
			courses = moodle.core_course_get_courses();
			newStatements = moodle.statementGenerator(gradeReport, userInfo, quizzes, courses);

			MoodleDataProxyService.oldCourseStatements.put(courseId, newStatements);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (JSONException e1) {
			e1.printStackTrace();
			return false;
		}

		for (String statement : newStatements) {
			context.monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_2, statement);
		}
		return true;
	}

	private class DataStreamThread implements Runnable {
		@Override
		public void run() {
			Gson gson = new Gson();
			TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

			// Get current time
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			Instant instant = timestamp.toInstant();
			long now = instant.getEpochSecond();
			for (Integer courseId : courseList) {
				try {
					String userInfoRaw = moodle.core_enrol_get_enrolled_users(courseId);
					JSONArray jsonUserInfo = new JSONArray(userInfoRaw);
					String userGradesObjectRaw = moodle.gradereport_user_get_grade_items(courseId);
					JSONObject userGradesObject = new JSONObject(userGradesObjectRaw);
					JSONArray userGrades = userGradesObject.getJSONArray("usergrades");
					for (Object o : userGrades) {
						JSONObject uGrades = (JSONObject) o;
						MoodleUserData moodleUserData = moodle.getMoodleUserData(jsonUserInfo, uGrades);
						JSONArray gradeItems = uGrades.getJSONArray("gradeitems");
						for (Object gradeItemObject : gradeItems) {
							JSONObject gradeItem = (JSONObject) gradeItemObject;
							MoodleUserGradeItem gItem = gson.fromJson(gradeItem.toString(), MoodleUserGradeItem.class);
							gItem.setCourseid(uGrades.getInt("courseid"));
							if (gItem.getGradedategraded() != null && gItem.getGradedategraded() > lastChecked) {
								// Get duration for quiz
								if (gItem.getItemtype().equals("quiz")) {
									String quizReviewRaw = moodle.mod_quiz_get_attempt_review(gItem.getId());
									JSONObject quizReview = new JSONObject(quizReviewRaw);
									JSONObject quizReviewAttempt = quizReview.getJSONObject("attempt");
									long start = quizReviewAttempt.getLong("timestart");
									long finish = quizReviewAttempt.getLong("timefinish");
									gItem.setDuration(finish - start);
									context.monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_2, xAPIStatements
											.createXAPIStatementGrades(moodleUserData, gItem, moodle.getDomainName()+"*"+email+"*"));
								} else if (gItem.getItemtype().equals("assign")) {
									String assignSubmissionsRaw = moodle
											.mod_assign_get_submissions(gItem.getIteminstance());
									JSONArray assignSubmissions = new JSONArray(assignSubmissionsRaw);
									MoodleAssignSubmission mas = moodle.getUserSubmission(assignSubmissions,
											moodleUserData.getUserId());
									long start = mas.getTimecreated();
									long finish = mas.getTimemodified();
									gItem.setDuration(finish - start);
									context.monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_2, xAPIStatements
											.createXAPIStatementGrades(moodleUserData, gItem, moodle.getDomainName()+"*"+email+"*"));
								}
								System.out.println("Item " + gItem.getId() + " graded");
							}
						}
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			lastChecked = now;
		}
	}

}
