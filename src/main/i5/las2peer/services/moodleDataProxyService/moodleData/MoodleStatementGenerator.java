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
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import i5.las2peer.services.moodleDataProxyService.MoodleDataProxyService;
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

	// courses, users, and assignments are cached
	private static Map<Integer, MoodleUser> userMap = new HashMap<Integer, MoodleUser>();
	private static Map<Integer, MoodleDataPOJO> modMap = new HashMap<Integer, MoodleDataPOJO>();
	private static Map<Integer, JSONArray> roleMap = new HashMap<>();

	public static Map<Integer, MoodleUser> getUserMap() {
		return userMap;
	}

	public static void setUserMap(Map<Integer, MoodleUser> userMap) {
		MoodleStatementGenerator.userMap = userMap;
	}

	public MoodleStatementGenerator(MoodleWebServiceConnection moodle) {
		MoodleStatementGenerator.moodle = moodle;
	}

	/**
	 * @param courseid id of the course that should be checked for updates
	 * @param since    epoch timestamp of oldest updates that should be returned
	 * @return Returns an ArrayList of new updates to forums and assignments,
	 * @throws IOException if an I/O exception occurs.
	 */
	public static ArrayList<String> courseUpdatesSince(int courseid, long since) throws IOException {
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

		roleMap = getUserRoles(courseid);

		ArrayList<String> statements = new ArrayList<>();
		statements.addAll(getForumUpdates(courseid, forumids, since));
		statements.addAll(getSubmissions(courseid, since));
		if (MoodleDataProxyService.isMoodleFunctionEnabled("local_t4c_get_recent_course_activities")) {
			statements.addAll(getEvents(courseid, since));
		}
		else {
			logger.warning("Cannot retrieve event data because tech4comp Moodle plugin not enabled.");
		}

		return statements;
	}

	/**
	 * @param forumids Array of module ids which belong to recently updated forums
	 * @param since    time of oldes changes to get included
	 * @return Returns an ArrayList of new discussions and discussion posts
	 * @throws IOException if an I/O exception occurs.
	 */
	private static ArrayList<String> getForumUpdates(int courseID, ArrayList<Integer> forumids, long since)
			throws IOException {
		ArrayList<String> forumUpdates = new ArrayList<String>();
		for (int forumid : forumids) {
			JSONArray discussions = moodle.mod_forum_get_forum_discussions(forumid);
			logger.info("Got discussions:\n" + discussions.toString());
			for (Object discussionObj : discussions) {
				JSONObject discussion = (JSONObject) discussionObj;

				// add new discussions
				if (since < discussion.getLong("created")) {
					int creatorId = discussion.getInt("userid");
					MoodleUser actor = getUser(creatorId, courseID);
					MoodleDiscussion object = new MoodleDiscussion(discussion);
					JSONObject builtStatement = xAPIStatements.createXAPIStatement(actor, "posted", object,
							moodle.getDomainName());
					addStatementContextExtensions(builtStatement, creatorId, courseID);
					int postID = discussion.getInt("id");
					addPostContextExtensions(builtStatement, postID);
					forumUpdates.add(builtStatement.toString() + "*" + actor.getMoodleToken());
				}

				// add new posts
				if (since < discussion.getLong("timemodified")) {
					JSONArray posts = moodle.mod_forum_get_discussion_posts(discussion.getInt("discussion"));
					for (Object postObj : posts) {
						JSONObject post = (JSONObject) postObj;
						if (since < post.getLong("timecreated") && post.getBoolean("hasparent")) {
							int creatorId = post.getJSONObject("author").getInt("id");
							MoodleUser actor = getUser(creatorId, courseID);
							MoodlePost object = new MoodlePost(post);
							JSONObject builtStatement = xAPIStatements.createXAPIStatement(actor, "replied", object,
									moodle.getDomainName());
							addStatementContextExtensions(builtStatement, creatorId, courseID);
							// Add ID of parent post in context extension
							int parentPostID = post.getInt("parentid");
							addReplyContextExtensions(builtStatement, parentPostID);
							forumUpdates.add(builtStatement.toString() + "*" + actor.getMoodleToken());
						}
					}
				}
			}
		}
		return forumUpdates;
	}

	/**
	 * @param gradeJSON JSON Array containing grading data for every user
	 * @param since     time of oldes changes to get included
	 * @return Returns an ArrayList of new submissions and grades
	 * @throws IOException if an I/O exception occurs.
	 */
	private static ArrayList<String> getSubmissions(int courseID, long since) throws IOException {
		JSONArray gradeJSON = null;
		ArrayList<String> submissions = new ArrayList<String>();
		try {
			gradeJSON = moodle.gradereport_user_get_grade_items(courseID);
		} catch (JSONException e) {
			logger.warning("Could not get 'usergrades' when using gradereport_user_get_grade_items.");
			return submissions;
		}

		for (Object userObj : gradeJSON) {
			JSONObject userReport = (JSONObject) userObj;
			for (Object submissionObj : userReport.getJSONArray("gradeitems")) {
				JSONObject submission = (JSONObject) submissionObj;

				// add new submission
				if (submission.isNull("gradedatesubmitted") && submission.isNull("gradedategraded")) {
					// neither submitted nor graded yet
					continue;
				} else if (submission.isNull("gradedatesubmitted") && submission.getLong("gradedategraded") < since) {
					// submitted but already graded (then gradedatesubmitted gets set to null again)
					// but was graded before current interval
					continue;
				} else if (submission.isNull("gradedategraded") && submission.getLong("gradedatesubmitted") < since) {
					// submitted and not yet graded, but was submitted before current interval
					continue;
				}
				
				if (!submission.isNull("itemtype") && submission.getString("itemtype").equals("course")) {
					// this is the grading for the course in total
					continue;
				}
				
				logger.info("Got submission:\n" + submission.toString());
				int userID = userReport.getInt("userid");
				MoodleUser actor = getUser(userID, courseID);
				MoodleExercise exercise = (MoodleExercise) getModule(submission.getInt("cmid"));

				// add new grade
				if (!submission.isNull("gradedategraded") && submission.getLong("gradedategraded") > since) {
					// got graded in the current interval
					MoodleGrade grade = new MoodleGrade(submission);
					if (!submission.isNull("modname") && submission.getString("modname") == "quiz") {
						JSONArray attempts = moodle.mod_quiz_get_user_attempts(submission.getInt("iteminstance"),
								userID);
						JSONObject attempt = (JSONObject) attempts.get(0);
						grade.setTimestart(attempt.getLong("timestart"));
						grade.setTimefinish(attempt.getLong("timefinish"));
					}
					JSONObject builtStatement = xAPIStatements.createXAPIStatement(actor, "completed", exercise, grade,
							moodle.getDomainName());
					addStatementContextExtensions(builtStatement, userID, courseID);
					submissions.add(builtStatement.toString() + "*" + actor.getMoodleToken());
				} else if (!submission.isNull("gradedatesubmitted") && submission.getLong("gradedatesubmitted") > since) {
					JSONObject builtStatement = xAPIStatements.createXAPIStatement(actor, "submitted", exercise,
							submission.getLong("gradedatesubmitted"), moodle.getDomainName());
					addStatementContextExtensions(builtStatement, userID, courseID);
					submissions.add(builtStatement.toString() + "*" + actor.getMoodleToken());
				}
			}
		}
		return submissions;
	}

	/**
	 * @param events JSON Array of recent events
	 * @param since  time of oldes event to get included
	 * @return Returns an ArrayList of new events
	 * @throws IOException if an I/O exception occurs.
	 */
	private static ArrayList<String> getEvents(int courseID, long since) throws IOException {
		JSONArray events = moodle.local_t4c_get_recent_course_activities(courseID, since);
		logger.info("Got events:\n" + events.toString());
		ArrayList<String> viewEvents = new ArrayList<String>();
		for (Object eventObject : events) {
			JSONObject event = (JSONObject) eventObject;
			int userID = event.getInt("userid");
			MoodleUser actor = getUser(userID, courseID);
			MoodleDataPOJO object = getModule(event.getInt("contextinstanceid"));

			// if target is not a module, log the target name and id
			// instead of the module name
			String overwriteName = "";
			if (!event.getString("target").equals("course_module")) {
				overwriteName = event.getString("target") + "_" + event.getInt("objectid");
			}
			JSONObject builtStatement = xAPIStatements.createXAPIStatement(actor, "viewed", object,
					event.getLong("timecreated"), overwriteName, moodle.getDomainName());
			addStatementContextExtensions(builtStatement, userID, courseID);
			viewEvents.add(builtStatement.toString() + "*" + actor.getMoodleToken());
		}
		return viewEvents;
	}

	/**
	 * Function that retrieves the user from the cached userMap if available, else
	 * creates it with data from Moodle.
	 * 
	 * @param userid id of the user whose information should be returned
	 * @return Returns a MoodleUser object associated with given userid
	 * @throws IOException if an I/O exception occurs.
	 */
	private static MoodleUser getUser(int userID, int courseID) throws IOException {
		MoodleUser user = userMap.get(userID);
		if (user == null) {
			JSONObject userJSON = moodle.core_user_get_users_by_field("id", userID);
			if (userJSON == null) {
				logger.warning("User with ID" + userID + " not found on Moodle!");
				return null;
			}
			user = new MoodleUser(userJSON);
			userMap.put(userID, user);
		}
		// Add roles to user
		if (user.getCourseRoles(courseID) == null) {
			JSONArray rolesJSON = roleMap.get(user.getId());
			user.putCourseRoles(courseID, rolesJSON);
		}
		return user;
	}

	/**
	 * @param cmid id of the module which information should be returned
	 * @return Returns a MoodleModule object associated with given cmid
	 * @throws IOException if an I/O exception occurs.
	 */
	private static MoodleDataPOJO getModule(int cmid) throws IOException {
		MoodleDataPOJO module = modMap.get(cmid);
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
			modMap.put(cmid, module);
		}
		return module;
	}

	/**
	 * Function retrieves roles of all enrolled users in a Moodle course.
	 * @param courseID
	 * @return A map where the key is the Moodle user ID and the value is
	 * the JSONArray "roles" from the core_enrol_get_enrolled_users Moodle function
	 * call
	 */
	private static Map<Integer, JSONArray> getUserRoles(int courseID) {
		Map<Integer, JSONArray> retVal = new HashMap<>();
		try {
			JSONArray enrolledUsers = moodle.core_enrol_get_enrolled_users(courseID);
			for (Object user : enrolledUsers) {
				JSONObject userJSON = (JSONObject) user;
				Integer userID;
				try {
					userID = userJSON.getInt("id");
				} catch (JSONException e) {
					logger.severe("Could not get user ID from core_enrol_get_enrolled_users while parsing roles. "
						+ e.getStackTrace());
					continue;
				}
				JSONArray roles;
				try {
					roles = userJSON.getJSONArray("roles");
				} catch (JSONException e) {
					logger.severe("Could not get user role from core_enrol_get_enrolled_users while parsing roles. User ID: "
						+ userID);
					continue;
				}
				retVal.put(userID, roles);
			}
		} catch (Exception e) {
			logger.severe("Error while reaching core_enrol_get_enrolled_users:" + e.getStackTrace());
		}
		return retVal;
	}

	/**
	 * Add context extensions to a statement. This includes actor roles and course
	 * info.
	 * 
	 * @param statement The statement to be edited.
	 * @param userID    User ID on Moodle.
	 * @param courseID  Course ID on Moodle.
	 */
	private static void addStatementContextExtensions(JSONObject statement, int userID, int courseID) {
		// prepare roles
		MoodleUser user = userMap.get(userID);
		List<Integer> roleList = user.getCourseRoles(courseID);
		JSONArray rolesJSON = new JSONArray();
		for (int roleID : roleList) {
			JSONObject roleJSON = new JSONObject();
			roleJSON.put("roleid", roleID);
			roleJSON.put("rolename", MoodleUser.getRoleName(roleID));
			rolesJSON.put(roleJSON);
		}
		// prepare course info
		JSONObject courseInfoJSON = new JSONObject();
		courseInfoJSON.put("courseid", courseID);
		courseInfoJSON.put("courseplatform", "Moodle");

		JSONObject extensionJSON = new JSONObject();
		extensionJSON.put("https://tech4comp.de/xapi/context/extensions/actorRoles", rolesJSON);
		extensionJSON.put("https://tech4comp.de/xapi/context/extensions/courseInfo", courseInfoJSON);

		JSONObject contextJSON = new JSONObject();
		contextJSON.put("extensions", extensionJSON);
		statement.put("context", contextJSON);
	}

	/**
	 * Add context extensions about a post reply's direct parent.
	 * It must be called after the regular context extensions have been added.
	 * @param statement The statement to be edited.
	 * @param parentPostID ID of the direct parent.
	 */
	private static void addReplyContextExtensions(JSONObject statement, int parentPostID) {
		JSONObject contextJSON = statement.getJSONObject("context");
		JSONObject extensionsJSON = contextJSON.getJSONObject("extensions");
		JSONObject parentIDJSON = new JSONObject();

		parentIDJSON.put("parentid", parentPostID);
		extensionsJSON.put("https://tech4comp.de/xapi/context/extensions/postParentID", parentIDJSON);
		contextJSON.put("extensions", extensionsJSON);
		statement.put("context", contextJSON);
	}

	/**
	 * Add context extensions about the root post of a discussion.
	 * It must be called after the regular context extensions have been added.
	 * @param statement The statement to be edited.
	 * @param postID ID of the root post.
	 */
	private static void addPostContextExtensions(JSONObject statement, int postID) {
		JSONObject contextJSON = statement.getJSONObject("context");
		JSONObject extensionsJSON = contextJSON.getJSONObject("extensions");
		JSONObject postIDJSON = new JSONObject();

		postIDJSON.put("rootpostid", postID);
		extensionsJSON.put("https://tech4comp.de/xapi/context/extensions/rootPostID", postIDJSON);
		contextJSON.put("extensions", extensionsJSON);
		statement.put("context", contextJSON);
	}

}
