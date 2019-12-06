package i5.las2peer.services.moodleDataProxyService.moodleData;

import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleUserData;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleUserGradeItem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import i5.las2peer.services.moodleDataProxyService.moodleData.xAPIStatements.xAPIStatements;

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
    MoodleUserData moodleUserData;
    MoodleUserGradeItem moodleUserGradeItem;

    for (int i = 0; i < jsonUserGrades.length(); i++) {

      moodleUserData = new MoodleUserData();
      moodleUserGradeItem = new MoodleUserGradeItem();

      courseId = null;
      userFullName = null;
      userId = null;
      email = null;
      courseName = null;
      courseSummary = null;

      JSONObject jsonUser = (JSONObject) jsonUserGrades.get(i);

      courseId = Integer.toString(jsonUser.getInt("courseid"));
      moodleUserData.setCourseId(courseId);

      courseSummary = getCourseSummaryById(courseId, courses);
      moodleUserData.setCourseSummary(courseSummary);

      userFullName = jsonUser.getString("userfullname");
      moodleUserData.setUserFullName(userFullName);

      userId = Integer.toString(jsonUser.getInt("userid"));
      moodleUserData.setUserId(userId);
      //get email
      email = statementGeneratorGetEmail(jsonUserInfo, userId);
      if(email == null) {
        email = userFullName.replace(" ", ".") + userId + "@example.com";
      }
      moodleUserData.setEmail(email);

      //get course name
      courseName = statementGeneratorGetCourseName(jsonUserInfo, userId, courseId);
      moodleUserData.setCourseName(courseName);

      JSONArray jsonGradeItems = (JSONArray) jsonUser.get("gradeitems");

      for(int j = 0; j < jsonGradeItems.length()-1; j++) {
        statements = statementGeneratorGetGrades(j, jsonGradeItems, jsonQuizzes, moodleUserData, statements, moodleUserGradeItem);
      } // end of loop jsonGradeItems

    } // end of loop jsonUserGrades

    return statements;
  }
    /*
     * Splitted previously implemented statementGenerator
     * This function retrieves the email of the student
     */
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

    /*
     * Splitted previously implemented statementGenerator
     * This function retrieves the course names of the student
     */
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

  /*
   * Splitted previously implemented statementGenerator
   * This function retrieves the summary of quizzes
   */
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

  /*
   * Splitted previously implemented statementGenerator
   * This function takes into account the grade items
   */
  private ArrayList<String> statementGeneratorGetGrades(int index, JSONArray jsonGradeItems, JSONArray jsonQuizzes,
                                                        MoodleUserData moodleUserData, ArrayList<String> statements, MoodleUserGradeItem moodleUserGradeItem) {
    String itemName = null;
    String itemId = null;
    String itemModule = null;
    String gradeDateSubmitted = null;
    Double percentageFormatted = null;
    String feedback = null;
    String quizSummary = null;

    JSONObject jsonItem = (JSONObject) jsonGradeItems.get(index);

    itemName = !jsonItem.isNull("itemname") ? jsonItem.getString("itemname") : "";
    moodleUserGradeItem.setItemName(itemName);

    itemId = Integer.toString(jsonItem.getInt("id"));
    moodleUserGradeItem.setItemId(itemId);

    quizSummary = statementGeneratorGetQuizzes(jsonQuizzes, moodleUserData.getCourseId(), itemId);
    moodleUserData.setQuizSummary(quizSummary);

    itemModule = !jsonItem.isNull("itemmodule") ? jsonItem.getString("itemmodule") : "";
    moodleUserGradeItem.setItemModule(itemModule);

    if (jsonItem.get("gradedatesubmitted") != JSONObject.NULL) {
      Date date = new Date();
      date.setTime((long) jsonItem.getInt("gradedatesubmitted") * 1000);

      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
      gradeDateSubmitted = sdf.format(date) + "Z";
    }
    moodleUserGradeItem.setGradeDateSubmitted(gradeDateSubmitted);

    //get rid of % and shift the format to 0.XXXX
    if (jsonItem.get("percentageformatted") != JSONObject.NULL && !jsonItem.getString("percentageformatted").equals("-")) {
      double d = Double.parseDouble(jsonItem.getString("percentageformatted").replaceAll("%", "").trim());
      d = d/100;
      percentageFormatted = d;
    }
    moodleUserGradeItem.setPercentageFormatted(percentageFormatted);

    //get rid of <p></p>
    if (jsonItem.get("feedback") != JSONObject.NULL) {
      feedback = jsonItem.getString("feedback").replaceAll("<p>", "").replaceAll("</p>", "");
    }
    moodleUserGradeItem.setFeedback(feedback);

    moodleUserData.setMoodleUserGradeItem(moodleUserGradeItem);

    //Creating xAPI Statements
    if (percentageFormatted != null) {
      statements = xAPIStatements.createXAPIStatements(moodleUserData, statements, domainName);
    }
    return statements;
  }
}