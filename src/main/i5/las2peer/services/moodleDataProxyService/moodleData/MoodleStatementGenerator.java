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
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleAssignSubmission;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleCourse;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleDiscussion;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodlePost;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleUser;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleUserGradeItem;
import i5.las2peer.services.moodleDataProxyService.moodleData.xAPIStatements.xAPIStatements;
import i5.las2peer.logging.L2pLogger;
import java.util.logging.Level;

public class MoodleStatementGenerator {
	private static MoodleWebServiceConnection moodle;
	private final static L2pLogger logger = L2pLogger.getInstance("Logger");

  private static Map<Integer, MoodleCourse> courseList = new HashMap<Integer, MoodleCourse>();
  private static Map<Integer, MoodleUser> userList = new HashMap<Integer, MoodleUser>();
	private static Map<Integer, MoodleDataPOJO> modList = new HashMap<Integer, MoodleDataPOJO>();

  public MoodleStatementGenerator(MoodleWebServiceConnection moodle) {
    MoodleStatementGenerator.moodle = moodle;
		L2pLogger.setGlobalConsoleLevel(Level.INFO);

		// init course and user list
		//TODO
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
		  int cmid = ((JSONObject) updateObject).getInt("id");
		  JSONObject module = moodle.core_course_get_course_module(cmid);
		  logger.info("Got module:\n" + module.toString());

		 // add module to id list based on type
			try {
			  String modtype = module.getString("modname");
				switch(modtype) {
					case "forum":
						forumids.add(module.getInt("instance"));
						break;
					case "assign":
					  assignmentids.add(module.getInt("instance"));
						break;
					default:
					  logger.warning("Unknown type " + modtype + " of module " + cmid + ". Ignoring!");
						break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		ArrayList<String> statements = getForumUpdates(forumids, since);
		//statements.addAll(getAssignUpdates(assignmentids));

		return statements;
	}

 	/**
 	 * @param forumids Array of module ids which belong to recently updated forums
	 * @param since time of oldes changes to get included
 	 * @return Returns an ArrayList of new discussions and discussion posts
	 * @throws IOException if an I/O exception occurs.
 	 */
  private static ArrayList<String> getForumUpdates(ArrayList<Integer> forumids, long since)
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
 	 * @param forumids Array of module ids which belong to recently updated
	 * assignments
 	 * @return Returns an ArrayList of new updates to assignments
 	 */
  private static ArrayList<String> getAssignUpdates(ArrayList<Integer> assignmentids) {
	  //TODO
		return null;
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
}
