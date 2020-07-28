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
import java.util.Map;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleWebServiceConnection;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleDataPOJO;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleExercise;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleCourse;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleDiscussion;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodlePost;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleUser;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleGrade;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleModule;
import i5.las2peer.services.moodleDataProxyService.moodleData.xAPIStatements;
import i5.las2peer.logging.L2pLogger;
import java.util.logging.Level;

public class MoodleStatementGenerator {
	private static MoodleWebServiceConnection moodle;
	private final static L2pLogger logger = L2pLogger.getInstance(MoodleDataPOJO.class.getName());

	// courses,  users, and assignments are cached
  private static Map<Integer, MoodleCourse> courseList = new HashMap<Integer, MoodleCourse>();
  private static Map<Integer, MoodleUser> userList = new HashMap<Integer, MoodleUser>();
	private static Map<Integer, MoodleDataPOJO> modList = new HashMap<Integer, MoodleDataPOJO>();

  public MoodleStatementGenerator(MoodleWebServiceConnection moodle) {
    MoodleStatementGenerator.moodle = moodle;
  }

	/**
	 * @param courseid id of the course that should be checked for updates
	 * @param since epoch timestamp of oldest updates that should be returned
	 * @return Returns an ArrayList of new updates to forums and assignments,
	 * @throws IOException if an I/O exception occurs.
	 */
	public static ArrayList<String> courseUpdatesSince(int courseid, long since)
	 	throws IOException {
	  // parse updated modules
	  ArrayList<Integer> forumids = new ArrayList<Integer>();
	  ArrayList<Integer> assignmentids = new ArrayList<Integer>();
	  JSONArray updates = moodle.core_course_get_updates_since(courseid, since);
	  logger.info("Got updates:\n" + updates.toString());
	  for (Object updateObject : updates) {
			JSONObject update = (JSONObject) updateObject;
			for (Object updateInfoObj : update.getJSONArray("updates")) {
		  	JSONObject updateInfo = (JSONObject) updateInfoObj;

				// collect updates forums
			  if (updateInfo.getString("name").equals("discussions")) {
					JSONObject module = moodle.core_course_get_course_module(update.getInt("id"));
					logger.info("Got forum:\n" + module.toString());
					forumids.add(module.getInt("instance"));
				}
			}
		}

		ArrayList<String> statements = getForumUpdates(forumids, since);
		statements.addAll(getSubmissions(moodle.gradereport_user_get_grade_items(
				courseid), since));
		// get events
		statements.addAll(getEvents(moodle.local_t4c_get_recent_course_activities(
				courseid, since), since));

		return statements;
	}

 	/**
 	 * @param forumids Array of module ids which belong to recently updated forums
	 * @param since time of oldes changes to get included
 	 * @return Returns an ArrayList of new discussions and discussion posts
	 * @throws IOException if an I/O exception occurs.
 	 */
  private static ArrayList<String> getForumUpdates(ArrayList<Integer> forumids,
			long since) throws IOException {
		ArrayList<String> forumUpdates = new ArrayList<String>();
		for (int forumid : forumids) {
			JSONArray discussions = moodle.mod_forum_get_forum_discussions(forumid);
			logger.info("Got discussions:\n" + discussions.toString());
			for (Object discussionObj : discussions) {
				JSONObject discussion = (JSONObject) discussionObj;

				// add new discussions
				if (since < discussion.getLong("created")) {
					int creatorId = discussion.getInt("userid");
					MoodleUser actor = getUser(creatorId);
					MoodleDiscussion object = new MoodleDiscussion(discussion);
					forumUpdates.add(xAPIStatements.createXAPIStatement(
						actor, "posted", object, moodle.getDomainName()) + "*" +
						actor.getMoodleToken());
				}

				// add new posts
				if (since < discussion.getLong("timemodified")) {
					JSONArray posts = moodle.mod_forum_get_discussion_posts(
						discussion.getInt("discussion"));
					for (Object postObj : posts) {
						JSONObject post = (JSONObject) postObj;
						if (since < post.getLong("timecreated") && post.getBoolean("hasparent")) {
							int creatorId = post.getJSONObject("author").getInt("id");
							MoodleUser actor = getUser(creatorId);
							MoodlePost object = new MoodlePost(post);
							forumUpdates.add(xAPIStatements.createXAPIStatement(
								actor, "replied", object, moodle.getDomainName()) + "*" +
								actor.getMoodleToken());
						}
					}
				}
			}
 		}
		return forumUpdates;
  }

 	/**
 	 * @param gradeJSON JSON Array containing grading data for every user
	 * @param since time of oldes changes to get included
 	 * @return Returns an ArrayList of new submissions and grades
	 * @throws IOException if an I/O exception occurs.
 	 */
  private static ArrayList<String> getSubmissions(JSONArray gradeJSON, long since)
			throws IOException {
		ArrayList<String> submissions = new ArrayList<String>();
		for (Object userObj : gradeJSON) {
			JSONObject userReport = (JSONObject) userObj;
			for (Object submissionObj : userReport.getJSONArray("gradeitems")) {
				JSONObject submission = (JSONObject) submissionObj;

				// add new submission
				if (submission.isNull("gradedatesubmitted") ||
						(submission.getLong("gradedatesubmitted") < since &&
						submission.getLong("gradedategraded") < since)) {
					continue;
				}
				logger.info("Got submission:\n" + submission.toString());
				MoodleUser actor = getUser(userReport.getInt("userid"));
				MoodleExercise exercise = (MoodleExercise) getModule(submission.getInt("cmid"));

				// add new grade
				if (!submission.isNull("gradedategraded") &&
						submission.getLong("gradedategraded") > since) {
					MoodleGrade grade = new MoodleGrade(submission);
					if (!submission.isNull("modname") &&
							submission.getString("modname") == "quiz") {
						JSONArray attempts = moodle.mod_quiz_get_user_attempts(
								submission.getInt("iteminstance"), userReport.getInt("userid"));
						JSONObject attempt = (JSONObject) attempts.get(0);
						grade.setTimestart(attempt.getLong("timestart"));
						grade.setTimefinish(attempt.getLong("timefinish"));
					}
					submissions.add(xAPIStatements.createXAPIStatement(
						actor, "completed", exercise, grade, moodle.getDomainName()) +
						"*" + actor.getMoodleToken());
				}
				else {
					submissions.add(xAPIStatements.createXAPIStatement(
						actor, "submitted", exercise,
						submission.getLong("gradedatesubmitted"), moodle.getDomainName()) +
						"*" + actor.getMoodleToken());
				}
			}
 		}
		return submissions;
  }

 	/**
 	 * @param events JSON Array of recent events
	 * @param since time of oldes event to get included
 	 * @return Returns an ArrayList of new events
	 * @throws IOException if an I/O exception occurs.
 	 */
  private static ArrayList<String> getEvents(JSONArray events,
			long since) throws IOException {
		logger.info("Got events:\n" + events.toString());
		ArrayList<String> viewEvents = new ArrayList<String>();
		for (Object eventObject : events) {
			JSONObject event = (JSONObject) eventObject;
			MoodleUser actor = getUser(event.getInt("userid"));
			MoodleDataPOJO object = getModule(event.getInt(
					"contextinstanceid"));
			viewEvents.add(xAPIStatements.createXAPIStatement(
				actor, "viewed", object, event.getLong("timecreated"),
				moodle.getDomainName()) + "*" + actor.getMoodleToken());
 		}
		return viewEvents;
  }

 	/**
 	 * @param userid id of the user whose information should be returned
 	 * @return Returns a MoodleUser object associated with given userid
	 * @throws IOException if an I/O exception occurs.
 	 */
  private static MoodleUser getUser(int userid) throws IOException {
		MoodleUser user = userList.get(userid);
		if (user == null) {
			JSONObject userJSON = moodle.core_user_get_users_by_field("id", userid);
			if (userJSON == null) {
				logger.warning("User " + userid + " not found!");
				return null;
			}
			user = new MoodleUser(userJSON);
			userList.put(userid, user);
		}
		return user;
  }

 	/**
 	 * @param cmid id of the module which information should be returned
 	 * @return Returns a MoodleModule object associated with given cmid
	 * @throws IOException if an I/O exception occurs.
 	 */
  private static MoodleDataPOJO getModule(int cmid) throws IOException {
		MoodleDataPOJO module = modList.get(cmid);
		if (module == null) {
			JSONObject moduleJSON = moodle.core_course_get_course_module(cmid);
			if (moduleJSON == null) {
				logger.severe("Module " + cmid + " not found!");
				return null;
			}
			switch (moduleJSON.getString("modname")) {
				case "assign":
					module = new MoodleExercise(moduleJSON);
					break;
				case "quiz":
					module = new MoodleExercise(moduleJSON);
					break;
				default:
					try {
						module = new MoodleModule(moduleJSON);
						break;
					} catch (Exception e) {
						logger.severe("Error getting module " + cmid + ":" + e.getStackTrace());
						return null;
					}
			}
			modList.put(cmid, module);
		}
		return module;
  }
}
