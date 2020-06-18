package i5.las2peer.services.moodleDataProxyService.moodleData.xAPIStatements;

import java.util.ArrayList;
import org.json.JSONObject;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleUserData;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleUserGradeItem;

public class xAPIStatements {

	public static JSONObject createActor(MoodleUserData moodleUserData, String domainName) {
		JSONObject actor = new JSONObject();
		// Actor
		actor.put("objectType", "Agent");
		actor.put("mbox", "mailto:" + moodleUserData.getEmail());
		actor.put("name", moodleUserData.getUserFullName());

		// Account -- new object based on the latest xAPI validation
		JSONObject account = new JSONObject();
		account.put("name", moodleUserData.getUserFullName());
		account.put("homepage", domainName);
		actor.put("account", account);
		return actor;
	}

	public static JSONObject createVerb() {
		// verb
		JSONObject verb = new JSONObject();
		verb.put("id", "http://activitystrea.ms/schema/1.0/complete");
		JSONObject display = new JSONObject();
		display.put("en-US", "completed");
		verb.put("display", display);
		return verb;
	}

	public static ArrayList<String> createXAPIStatements(MoodleUserData moodleUserData, ArrayList<String> statements,
			String domainName, String userToken) {
		JSONObject actor = createActor(moodleUserData, domainName);

		// verb
		JSONObject verb = createVerb();

		// object
		JSONObject object = new JSONObject();
		object.put("id", domainName + "/mod/" + moodleUserData.getMoodleUserGradeItem().getItemmodule()
				+ "/view.php?id=" + moodleUserData.getMoodleUserGradeItem().getId());

		JSONObject definition = new JSONObject();
		definition.put("type", domainName + "/course/view.php?id=" + moodleUserData.getCourseId());

		JSONObject name = new JSONObject();
		name.put("en-US", moodleUserData.getMoodleUserGradeItem().getItemname());
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
		object.put("objectType", "Activity");

		// result
		JSONObject result = new JSONObject();
		result.put("completion", true);

		if (moodleUserData.getMoodleUserGradeItem().getFeedback() != null) {
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
    statements.add(statement.toString() + "*" + userToken + "*" );
    return statements;
	}

	public static String createXAPIStatementGrades(MoodleUserData moodleUserData, MoodleUserGradeItem gItem,
			String domainName, String userToken) {
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
			result.put("duration", "undefined");
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
    return (statement.toString() + "*" + userToken + "*") ;
	}
}
