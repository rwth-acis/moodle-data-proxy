package i5.las2peer.services.moodleDataProxyService.moodleData;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MoodleWebServiceConnection {
  private static String token = null;
  private static String domainName = null;
  private static String restFormat ="&moodlewsrestformat=json";

  /**
   */
  public MoodleWebServiceConnection(String token, String domainName) {
    MoodleWebServiceConnection.token = token;
    MoodleWebServiceConnection.domainName = domainName;
  }


  /**
   * This function requests a Rest function to the initiated moodle web server.
   * @param functionName This the function name for the moodle rest request.
   * @param urlParameters These are the parameters in one String for the moodle rest request.
   * @return Returns the output of the moodle rest request.
   */
  private String restRequest(String functionName, String urlParameters) throws ProtocolException, IOException{
    // Send request
    String serverUrl = domainName + "/webservice/rest/server.php" + "?wstoken=" + token + "&wsfunction=" + functionName + restFormat;

    HttpURLConnection con = (HttpURLConnection) new URL(serverUrl).openConnection();

    con.setRequestMethod("POST");
    con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    con.setRequestProperty("Content-Language", "en-US");
    con.setDoOutput(true);
    con.setUseCaches (false);
    con.setDoInput(true);

    DataOutputStream wr = new DataOutputStream (con.getOutputStream ());

    if (urlParameters != null) {
      wr.writeBytes (urlParameters);
    }
    wr.flush ();
    wr.close ();

    //Get Response
    InputStream is =con.getInputStream();
    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
    String line;
    StringBuilder response = new StringBuilder();
    while((line = rd.readLine()) != null) {
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
    String urlParameters = "courseid=" + URLEncoder.encode(Integer.toString(courseId), "UTF-8") +
            "&userid=" + URLEncoder.encode(Integer.toString(userId), "UTF-8");
    return restRequest("gradereport_user_get_grade_items", urlParameters);
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
  public String getCourseSummaryById(String courseId, String courses) {
    JSONArray jsonCourses = new JSONArray(courses);
    String courseSummary = null;
    //course summary
    for(Object ob: jsonCourses) {
      JSONObject jsonCourse = (JSONObject) ob;
      if(Integer.toString(jsonCourse.getInt("id")).equals(courseId)
              && jsonCourse.get("summary") != JSONObject.NULL) {
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
  public ArrayList<String> statementGenerator(String gradereport, String userinfo, String quizzes, String courses) throws JSONException {
    ArrayList<String> statements = new ArrayList<>();

    JSONObject jsonGradeReport = new JSONObject(gradereport);
    JSONArray jsonUserGrades = (JSONArray) jsonGradeReport.get("usergrades");
    JSONArray jsonUserInfo = new JSONArray(userinfo);
    JSONObject jsonModQuiz = new JSONObject(quizzes);
    JSONArray jsonQuizzes = (JSONArray) jsonModQuiz.get("quizzes");

    String courseId;
    String userFullName;
    String userId;
    String email;
    String courseName;
    String courseSummary;

    for (int i = 0; i < jsonUserGrades.length(); i++) {

      courseId = null;
      userFullName = null;
      userId = null;
      email = null;
      courseName = null;
      courseSummary = null;

      JSONObject jsonUser = (JSONObject) jsonUserGrades.get(i);

      courseId = Integer.toString(jsonUser.getInt("courseid"));

      courseSummary = getCourseSummaryById(courseId, courses);

      userFullName = jsonUser.getString("userfullname");
      userId = Integer.toString(jsonUser.getInt("userid"));

      //get email
      email = statementGeneratorGetEmail(jsonUserInfo, userId);
      if(email == null) {
        email = userFullName.replace(" ", ".") + userId + "@example.com";
      }

      //get course name
      courseName = statementGeneratorGetCourseName(jsonUserInfo, userId, courseId);

      JSONArray jsonGradeItems = (JSONArray) jsonUser.get("gradeitems");

      for(int j = 0; j < jsonGradeItems.length()-1; j++) {
        statements = statementGeneratorGetGrades(j, jsonGradeItems, jsonQuizzes, courseId, userFullName, email, courseSummary, statements);
      } // end of loop jsonGradeItems

    } // end of loop jsonUserGrades

    return statements;
  }

  private String statementGeneratorGetEmail(JSONArray jsonUserInfo, String userId){
    String email = "";
    for (int k = 0 ; k < jsonUserInfo.length(); k++) {
      JSONObject jsonInfo = (JSONObject) jsonUserInfo.get(k);
      if(Integer.toString(jsonInfo.getInt("id")).equals(userId)) {
        if (jsonInfo.get("email") != JSONObject.NULL)
          email = jsonInfo.getString("email");
        break;
      }
    }
    return email;
  }

  private String statementGeneratorGetCourseName(JSONArray jsonUserInfo, String userId, String courseId){
    String courseName = null;

    for (int k = 0 ; k < jsonUserInfo.length(); k++) {
      JSONObject jsonInfo = (JSONObject) jsonUserInfo.get(k);
      if(Integer.toString(jsonInfo.getInt("id")).equals(userId)) {
        JSONArray jsonEnrolled = (JSONArray) jsonInfo.get("enrolledcourses");
        for (int l = 0 ; l < jsonUserInfo.length(); l++) {
          JSONObject jsonEnrolledCourse = (JSONObject) jsonEnrolled.get(l);
          if(Integer.toString(jsonEnrolledCourse.getInt("id")).equals(courseId)) {
            courseName = jsonEnrolledCourse.getString("fullname");
            break;
          }
        }

      }
    }
    return courseName;
  }

  private String statementGeneratorGetQuizzes(JSONArray jsonQuizzes, String courseId, String itemId){
    String quizSummary = null;
    for(Object ob: jsonQuizzes) {
      JSONObject jsonQuiz = (JSONObject) ob;
      if(Integer.toString(jsonQuiz.getInt("course")).equals(courseId)
              && Integer.toString(jsonQuiz.getInt("coursemodule")).equals(itemId)
              && jsonQuiz.get("intro") != JSONObject.NULL) {
        quizSummary = jsonQuiz.getString("intro");
      }
    }
    return quizSummary;
  }

  private ArrayList<String> statementGeneratorGetGrades(int index, JSONArray jsonGradeItems, JSONArray jsonQuizzes,
                                                        String courseId, String userFullName, String email,
                                                        String courseSummary, ArrayList<String> statements) {
    String itemName = null;
    String itemId = null;
    String itemModule = null;
    String gradeDateSubmitted = null;
    Double percentageFormatted = null;
    String feedback = null;
    String quizSummary = null;

    JSONObject jsonItem = (JSONObject) jsonGradeItems.get(index);

    itemName = !jsonItem.isNull("itemname") ? jsonItem.getString("itemname") : "";

    itemId = Integer.toString(jsonItem.getInt("id"));

    quizSummary = statementGeneratorGetQuizzes(jsonQuizzes, courseId, itemId);

    itemModule = !jsonItem.isNull("itemmodule") ? jsonItem.getString("itemmodule") : "";

    if (jsonItem.get("gradedatesubmitted") != JSONObject.NULL) {
      Date date = new Date();
      date.setTime((long)jsonItem.getInt("gradedatesubmitted")*1000);

      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
      gradeDateSubmitted = sdf.format(date) + "Z";
    }

    //get rid of % and shift the format to 0.XXXX
    if (jsonItem.get("percentageformatted") != JSONObject.NULL && !jsonItem.getString("percentageformatted").equals("-")) {
      double d = Double.parseDouble(jsonItem.getString("percentageformatted").replaceAll("%", "").trim());
      d = d/100;
      percentageFormatted = d;
    }

    //get rid of <p></p>
    if (jsonItem.get("feedback") != JSONObject.NULL) {
      feedback = jsonItem.getString("feedback").replaceAll("<p>", "").replaceAll("</p>", "");
    }

    //Creating xAPI Statements
    if (percentageFormatted != null) {
      statements = createXAPIstatements(percentageFormatted, email, userFullName, itemModule, itemId, courseId, itemName, quizSummary,
              courseSummary, feedback, gradeDateSubmitted, statements);
    }
    return statements;
  }

  private ArrayList<String> createXAPIstatements(Double percentageFormatted, String email, String userFullName,
                                                 String itemModule, String itemId, String courseId, String itemName, String quizSummary,
                                                 String courseSummary, String feedback, String gradeDateSubmitted, ArrayList<String> statements) {


    JSONObject actor = new JSONObject();
    actor.put("objectType", "Agent");
    actor.put("mbox", "mailto:" + email);
    actor.put("name", userFullName);

    JSONObject verb = new JSONObject();
    verb.put("id", "http://example.com/xapi/completed");

    JSONObject display = new JSONObject();
    display.put("en-US", "completed");

    verb.put("display", display);

    JSONObject object = new JSONObject();
    object.put("id", domainName + "/mod/" + itemModule + "/view.php?id=" + itemId);

    JSONObject definition = new JSONObject();
    definition.put("type", domainName + "/course/view.php?id=" + courseId);

    JSONObject name = new JSONObject();
    name.put("en-US", itemName);

    definition.put("name", name);

    JSONObject description = new JSONObject();

    if (quizSummary != null) {
      description.put("en-US", "Course description: " + courseSummary
              + " \n Description: " + quizSummary);
    } else {
      description.put("en-US", "Course description: "+ courseSummary);
    }

    definition.put("description", description);

    object.put("definition", definition);
    object.put("objectType", "Activity");

    JSONObject result = new JSONObject();
    result.put("completion", true);

    if(feedback != null) {
      result.put("response", feedback);
    }

    JSONObject score = new JSONObject();
    score.put("scaled", percentageFormatted);

    result.put("score", score);

    JSONObject statement = new JSONObject();
    statement.put("actor", actor);
    statement.put("verb", verb);
    statement.put("object", object);
    statement.put("result", result);
    statement.put("timestamp", gradeDateSubmitted);

    statements.add(statement.toString());
    return statements;
  }
}
