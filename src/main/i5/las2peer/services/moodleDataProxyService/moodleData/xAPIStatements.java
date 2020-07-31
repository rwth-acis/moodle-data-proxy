package i5.las2peer.services.moodleDataProxyService.moodleData;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.json.JSONObject;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleDataPOJO;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodlePost;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleDiscussion;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleUser;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleGrade;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleExercise;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleModule;
import i5.las2peer.logging.L2pLogger;

public class xAPIStatements {
	private final static L2pLogger logger = L2pLogger.getInstance(MoodleDataPOJO.class.getName());

	public static JSONObject createActor(MoodleUser moodleUser, String moodleDomain) {
		JSONObject actor = new JSONObject();
		// Actor
		actor.put("objectType", "Agent");
		actor.put("name", moodleUser.getFullname());

		// Account -- new object based on the latest xAPI validation
		JSONObject account = new JSONObject();
		account.put("name", moodleUser.getEmail());
		account.put("homePage", moodleDomain);
		actor.put("account", account);
		return actor;
	}

	public static JSONObject createVerb(String verb) {
		// create verb based on switch
		String id = null;
		switch (verb) {
			case "posted":
				id = "https://w3id.org/xapi/acrossx/verbs/posted";
				break;
			case "replied":
				id = "http://id.tincanapi.com/verb/replied";
				break;
			case "submitted":
				id = "https://w3id.org/xapi/dod-isd/verbs/submitted";
				break;
			case "created":
				id = "https://w3id.org/xapi/dod-isd/verbs/created";
				break;
			case "received":
				id = "https://w3id.org/xapi/dod-isd/verbs/received";
				break;
			case "answered":
				id = "https://w3id.org/xapi/dod-isd/verbs/answered";
				break;
			case "completed":
				id = "https://w3id.org/xapi/dod-isd/verbs/completed";
				break;
			case "viewed":
				id = "http://id.tincanapi.com/verb/viewed";
				break;
		}

		JSONObject verbObj = new JSONObject();
		verbObj.put("id", id);
		JSONObject display = new JSONObject();
		display.put("en-US", verb);
		verbObj.put("display", display);
		return verbObj;
	}

  // create standard xAPI statement
	public static String createXAPIStatement(MoodleUser moodleUser,
			String activity, MoodleDataPOJO moodleModule, String moodleDomain) {
		return createBasicXAPI(moodleUser, activity, moodleModule, moodleDomain,
				moodleModule.getCreated()).toString();
	}

	// create xAPI statement with custom timestamp
	public static String createXAPIStatement(MoodleUser moodleUser,
			String activity, MoodleDataPOJO moodleModule, long viewed,
			String moodleDomain) {
		return createBasicXAPI(moodleUser, activity, moodleModule, moodleDomain,
				viewed).toString();
	}

	// create xAPI statement with custom timestamp and custom name
	public static String createXAPIStatement(MoodleUser moodleUser,
			String activity, MoodleDataPOJO moodleModule, long viewed, String newName,
			String moodleDomain) {
		JSONObject viewEvent = createBasicXAPI(moodleUser, activity, moodleModule,
			moodleDomain, viewed);
		if (newName.length() > 0) {
			JSONObject object = viewEvent.getJSONObject("object");
			JSONObject definition = object.getJSONObject("definition");
			JSONObject name = definition.getJSONObject("name");
			name.put("en-US", newName);
			definition.put("name", name);
			object.put("definition", definition);
			viewEvent.put("object", object);
		}
		return viewEvent.toString();
	}

	// create xAPI statement from exercise with grade data
	public static String createXAPIStatement(MoodleUser moodleUser,
			String activity, MoodleExercise moodleExercise, MoodleGrade gradeData,
			String moodleDomain) {
		JSONObject statement = createBasicXAPI(moodleUser, activity, moodleExercise,
				moodleDomain, gradeData.getCreated());

		// result
		JSONObject result = createResult(gradeData, moodleExercise.getGradepass());

		statement.put("result", result);
		return statement.toString();
	}

	private static JSONObject createBasicXAPI(MoodleUser moodleUser,
			String activity, MoodleDataPOJO moodleModule, String moodleDomain,
			long age) {
		JSONObject actor = createActor(moodleUser, moodleDomain);

		// verb
		JSONObject verb = createVerb(activity);

		// object
		JSONObject object = createObject(moodleModule, moodleDomain);

		// timestamp
		LocalDateTime dateObj = LocalDateTime.ofEpochSecond(
				age, 0, ZoneOffset.UTC);
		String timestamp = dateObj.toString() + "Z";

		JSONObject statement = new JSONObject();
		statement.put("actor", actor);
		statement.put("verb", verb);
		statement.put("object", object);
		statement.put("timestamp", timestamp);
		return statement;
	}

	private static JSONObject createObject(MoodleDataPOJO moduleData, String domainName) {
		if (moduleData instanceof MoodlePost)
			return createPost((MoodlePost) moduleData, domainName);
		else if (moduleData instanceof MoodleDiscussion)
			return createDiscussion((MoodleDiscussion) moduleData, domainName);
		else if (moduleData instanceof MoodleExercise)
			return createExercise((MoodleExercise) moduleData, domainName);
		else if (moduleData instanceof MoodleModule)
			return createModule((MoodleModule) moduleData, domainName);
		return null;
	}

	private static JSONObject createDiscussion(MoodleDiscussion discussionData, String domainName) {
		JSONObject object = new JSONObject();
		object.put("id", domainName + "/mod/forum/discuss.php?d=" + discussionData.getDiscussion());

		JSONObject definition = new JSONObject();
		definition.put("type", "http://id.tincanapi.com/activitytype/discussion");

		JSONObject name = new JSONObject();
		name.put("en-US", discussionData.getSubject());
		definition.put("name", name);

		JSONObject description = new JSONObject();
		description.put("en-US", discussionData.getMessage());

		definition.put("description", description);

		// definition.interactionType -- new property based on the latest xAPI validation
		definition.put("interactionType", "other");

		object.put("definition", definition);
		object.put("objectType", "Activity");
		return object;
	}

	private static JSONObject createPost(MoodlePost postData, String domainName) {
		JSONObject object = new JSONObject();
		object.put("id", domainName + "/mod/forum/discuss.php?d=" +
			postData.getDiscussionid() + "#p" + postData.getId());

		JSONObject definition = new JSONObject();
		definition.put("type", "http://id.tincanapi.com/activitytype/forum-reply");

		JSONObject name = new JSONObject();
		name.put("en-US", postData.getSubject());
		definition.put("name", name);

		JSONObject description = new JSONObject();
		description.put("en-US", postData.getMessage());

		definition.put("description", description);

		// definition.interactionType -- new property based on the latest xAPI validation
		definition.put("interactionType", "other");

		object.put("definition", definition);
		object.put("objectType", "Activity");
		return object;
	}

	private static JSONObject createExercise(MoodleExercise exerciseData,
			String domainName) {
		JSONObject object = new JSONObject();
		object.put("id", domainName + "/mod/" + exerciseData.getModname() +
			"/view.php?id=" + exerciseData.getId());

		JSONObject definition = new JSONObject();
		definition.put("type", "http://id.tincanapi.com/activitytype/school-assignment");

		JSONObject name = new JSONObject();
		name.put("en-US", exerciseData.getName());
		definition.put("name", name);

		JSONObject description = new JSONObject();
		description.put("en-US", "A " + exerciseData.getModname() + " where students " +
				"pass with a score of at least " + exerciseData.getGradepass() +
				"/" + exerciseData.getGrade());

		definition.put("description", description);

		// definition.interactionType -- new property based on the latest xAPI validation
		definition.put("interactionType", "other");

		object.put("definition", definition);
		object.put("objectType", "Activity");
		return object;
	}

	private static JSONObject createResult(MoodleGrade gradeData, String gradepass) {
		JSONObject result = new JSONObject();
		result.put("completion", true);
		result.put("response", gradeData.getFeedback());

		if (gradeData.getTimestart() < gradeData.getTimefinish()) {
			long duration = gradeData.getTimestart() - gradeData.getTimefinish();
			result.put("duration", "P" + duration + "D");
		}

		// Score -- new object based on the latest xAPI validation
		JSONObject score = new JSONObject();
		score.put("min", gradeData.getGrademin());
		score.put("max", gradeData.getGrademax());
		score.put("raw", gradeData.getGraderaw());

		// Scale Calculation raw/max
		double scaled = gradeData.getGrademax() != 0 ?
				(gradeData.getGraderaw() / gradeData.getGrademax()) : 0.0;
		score.put("scaled", scaled);
		result.put("score", score);

		try {
			float passingGrade = Float.parseFloat(gradepass);
			boolean passed = gradeData.getGraderaw() >= passingGrade;
			result.put("success", passed);
		} catch (Exception e) {
			logger.warning("Cannot convert " + gradepass + " to float");
		}

		return result;
	}

	private static JSONObject createModule(MoodleModule moduleData,
			String domainName) {
		JSONObject object = new JSONObject();
		object.put("id", domainName + "/mod/" + moduleData.getModname() +
				"/view.php?id=" + moduleData.getId());

		JSONObject definition = new JSONObject();
		definition.put("type", "https://w3id.org/xapi/seriousgames/activity-types/item");

		JSONObject name = new JSONObject();
		name.put("en-US", moduleData.getModname() + "_" + moduleData.getInstance());
		definition.put("name", name);

		// definition.interactionType -- new property based on the latest xAPI validation
		definition.put("interactionType", "other");

		object.put("definition", definition);
		object.put("objectType", "Activity");
		return object;
	}
}
