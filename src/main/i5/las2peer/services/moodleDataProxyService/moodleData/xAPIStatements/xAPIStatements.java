package i5.las2peer.services.moodleDataProxyService.moodleData.xAPIStatements;

import java.util.ArrayList;
import org.json.JSONObject;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleDataPOJO;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodlePost;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleDiscussion;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleUserData;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleUserGradeItem;

public class xAPIStatements {

	public static JSONObject createActor(MoodleUserData moodleUserData) {
		JSONObject actor = new JSONObject();
		// Actor
		actor.put("objectType", "Agent");
		actor.put("name", moodleUserData.getUserFullName());

		// Account -- new object based on the latest xAPI validation
		JSONObject account = new JSONObject();
		account.put("name", moodleUserData.getEmail());
		account.put("homePage", moodleUserData.getDomainName());
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
		}

		JSONObject verbObj = new JSONObject();
		verbObj.put("id", id);
		JSONObject display = new JSONObject();
		display.put("en-US", verb);
		verbObj.put("display", display);
		return verbObj;
	}

	public static ArrayList<String> createXAPIStatement(MoodleUserData moodleUser,
		String activity, MoodleDataPOJO moodleModule) {
		JSONObject actor = createActor(moodleUser);

		// verb
		JSONObject verb = createVerb(activity);

		// object
		JSONObject object = createObject(moodleModule);

		// result
		JSONObject result = new JSONObject();
		result.put("completion", true);

		/*if (moodleUserData.getMoodleUserGradeItem().getFeedback() != null) {
			result.put("response", moodleUserData.getMoodleUserGradeItem().getFeedback());
		}

		int duration = moodleUserData.getMoodleCourse().getDuration();
		// needs to be updated
		result.put("duration", "P" + duration + "D");

		// Score -- new object based on the latest xAPI validation
		JSONObject score = new JSONObject();
		score.put("min", moodleUserData.getMoodleUserGradeItem().getGrademin());
		score.put("max", moodleUserData.getMoodleUserGradeItem().getGrademax());
		score.put("raw", moodleUserData.getMoodleUserGradeItem().getGraderaw());

		// Scale Calculation raw/max
		double scaled = moodleUserData.getMoodleUserGradeItem().getGrademax() != 0
				? (moodleUserData.getMoodleUserGradeItem().getGraderaw()
						/ moodleUserData.getMoodleUserGradeItem().getGrademax())
				: 0.0;
		score.put("scaled", scaled);

		result.put("score", score);

		// can be changed according to the setting
		result.put("success", true);

    JSONObject statement = new JSONObject();
    statement.put("actor", actor);
    statement.put("verb", verb);
    statement.put("object", object);
    statement.put("result", result);
    statement.put("timestamp", moodleUserData.getMoodleUserGradeItem()
      .getGradedatesubmitted());
		statements.add(statements.toString());
    return statements; */
		return null;
	}

	private static JSONObject createObject(MoodleUserGradeItem gItem) {
/*		object.put("id", domainName + "/mod/" + gItem.getItemmodule()
				+ "/view.php?id=" + gItem.getId());

		JSONObject definition = new JSONObject();
		definition.put("type", domainName + "/course/view.php?id=" + moodleUserData.getCourseId());

		JSONObject name = new JSONObject();
		name.put("en-US", gItem.getItemname());
		definition.put("name", name);

		JSONObject description = new JSONObject();
		if (moodleUserData.getQuizSummary() != null) {
			description.put("en-US", "Course description: " + moodleUserData.getCourseSummary() + " \n Description: "
					+ moodleUserData.getQuizSummary());
		} else {
			description.put("en-US", "Course description: " + moodleUserData.getCourseSummary());
		}

		definition.put("description", description);

		// definition.interactionType -- new property based on the latest xAPI validation
		definition.put("interactionType", "other");

		object.put("definition", definition);
		object.put("objectType", "Activity"); */
		return null;
	}

	private static JSONObject createObject(MoodleDataPOJO moduleData) {
		// todo
		if (moduleData instanceof MoodlePost)
			return null;
		else if (moduleData instanceof MoodleDiscussion)
			return null;
		return null;
	}

/*	public static String createXAPIStatementGrades(MoodleUserData moodleUserData, MoodleUserGradeItem gItem,
			String domainName) {
		JSONObject actor = createActor(moodleUserData, domainName);

		// verb
		JSONObject verb = createVerb();

		// object
		JSONObject object = new JSONObject();
		object.put("id", domainName + "/mod/" + gItem.getItemmodule() + "/view.php?id=" + gItem.getId());

		JSONObject definition = new JSONObject();
		definition.put("type", domainName + "/course/view.php?id=" + gItem.getCourseid());

		JSONObject name = new JSONObject();
		name.put("en-US", gItem.getItemname());
		definition.put("name", name);

		JSONObject description = new JSONObject();
		if (gItem.getItemname() != null) {
			description.put("en-US", gItem.getItemname());
		} else {
			// TODO
			description.put("en-US", "");
		}

		definition.put("description", description);

		// definition.interactionType -- new property based on the latest xAPI validation
		definition.put("interactionType", "other");

		object.put("definition", definition);
		object.put("objectType", "Activity");

		// result
		JSONObject result = new JSONObject();
		result.put("completion", true);

		if (gItem.getFeedback() != null) {
			result.put("response", gItem.getFeedback());
		}

		try {
			long duration = gItem.getDuration();
			// needs to be updated
			result.put("duration", "P" + duration + "D");
		} catch (Exception e) {
			result.put("duration", "PT0S");
		}

		// Score -- new object based on the latest xAPI validation
		JSONObject score = new JSONObject();
		score.put("min", gItem.getGrademin());
		score.put("max", gItem.getGrademax());
		score.put("raw", gItem.getGraderaw());

		// Scale Calculation raw/max
		double scaled = gItem.getGrademax() != 0 ? (gItem.getGraderaw() / gItem.getGrademax()) : 0.0;
		score.put("scaled", scaled);

		result.put("score", score);

		// can be changed according to the setting
		result.put("success", true);

    JSONObject statement = new JSONObject();
    statement.put("actor", actor);
    statement.put("verb", verb);
    statement.put("object", object);
    statement.put("result", result);
    statement.put("timestamp", gItem.getGradedatesubmitted());
    return statement.toString();
	} */
}
