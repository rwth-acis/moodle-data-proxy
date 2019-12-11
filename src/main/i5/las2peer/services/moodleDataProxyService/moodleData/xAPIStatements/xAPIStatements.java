package i5.las2peer.services.moodleDataProxyService.moodleData.xAPIStatements;

import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleUserData;
import org.json.JSONObject;

import java.util.ArrayList;

public class xAPIStatements {

    public static ArrayList<String> createXAPIStatements(MoodleUserData moodleUserData, ArrayList<String> statements, String domainName) {
        JSONObject actor = new JSONObject();
        //Actor
        actor.put("objectType", "Agent");
        actor.put("mbox", "mailto:" + moodleUserData.getEmail());
        actor.put("name", moodleUserData.getUserFullName());

        //Account -- new object based on the latest xAPI validation
        JSONObject account = new JSONObject();
        account.put("name", moodleUserData.getUserFullName());
        account.put("homepage",domainName);
        actor.put("account", account);

        //verb
        JSONObject verb = new JSONObject();
        verb.put("id", "http://example.com/xapi/completed");

        JSONObject display = new JSONObject();
        display.put("en-US", "completed");

        verb.put("display", display);

        //object
        JSONObject object = new JSONObject();
        object.put("id", domainName + "/mod/" + moodleUserData.getMoodleUserGradeItem().getItemModule() + "/view.php?id=" + moodleUserData.getMoodleUserGradeItem().getItemId());

        JSONObject definition = new JSONObject();
        definition.put("type", domainName + "/course/view.php?id=" + moodleUserData.getCourseId());

        JSONObject name = new JSONObject();
        name.put("en-US", moodleUserData.getMoodleUserGradeItem().getItemName());
        definition.put("name", name);

        JSONObject description = new JSONObject();
        if (moodleUserData.getQuizSummary() != null) {
            description.put("en-US", "Course description: " + moodleUserData.getCourseSummary()
                    + " \n Description: " + moodleUserData.getQuizSummary());
        } else {
            description.put("en-US", "Course description: "+ moodleUserData.getCourseSummary());
        }

        definition.put("description", description);

        //definition.interactionType -- new property based on the latest xAPI validation
        definition.put("interactionType","other");

        object.put("definition", definition);
        object.put("objectType", "Activity");


        //result
        JSONObject result = new JSONObject();
        result.put("completion", true);

        if(moodleUserData.getMoodleUserGradeItem().getFeedback() != null) {
            result.put("response", moodleUserData.getMoodleUserGradeItem().getFeedback());
        }

        //needs to be updated
        result.put("duration","PT" + "S");


        JSONObject score = new JSONObject();

        score.put("scaled", moodleUserData.getMoodleUserGradeItem().getPercentageFormatted());
        score.put("min", moodleUserData.getMoodleUserGradeItem().getGradeMin());
        score.put("max", moodleUserData.getMoodleUserGradeItem().getGradeMax());
        score.put("raw", moodleUserData.getMoodleUserGradeItem().getGradeRaw());

        result.put("score", score);

        JSONObject statement = new JSONObject();
        statement.put("actor", actor);
        statement.put("verb", verb);
        statement.put("object", object);
        statement.put("result", result);
        statement.put("timestamp", moodleUserData.getMoodleUserGradeItem().getGradeDateSubmitted());

        statements.add(statement.toString());
        return statements;
    }
}
