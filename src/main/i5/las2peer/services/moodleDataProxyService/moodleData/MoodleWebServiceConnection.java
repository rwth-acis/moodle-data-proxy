package i5.las2peer.services.moodleDataProxyService.moodleData;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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
   * @param isJsonFormat If you want the output in json format than set true.
   */
  public MoodleWebServiceConnection(String token, String domainName) {
    MoodleWebServiceConnection.token = token;
    MoodleWebServiceConnection.domainName = domainName;
  }
  
  
  /**
   * This function requests a Rest function to the initiated moodle web server.
   * @param functionName This the function name for the moodle rest request.
   * @param assignmentNumber These are the parameters in one String for the moodle rest request.
   * @return Returns the output of the moodle rest request.
   */
  private String restRequest(String functionName, String urlParameters) throws ProtocolException, IOException{
    // Send request
    String serverurl = domainName + "/webservice/rest/server.php" + "?wstoken=" + token + "&wsfunction=" + functionName + restFormat;
    HttpURLConnection con = (HttpURLConnection) new URL(serverurl).openConnection();
    con.setRequestMethod("POST");
    con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    con.setRequestProperty("Content-Language", "en-US");
    con.setDoOutput(true);
    con.setUseCaches (false);
    con.setDoInput(true);
    DataOutputStream wr = new DataOutputStream (con.getOutputStream ());
    if (urlParameters != null) wr.writeBytes (urlParameters);
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
    String result = restRequest("core_course_get_courses", null);
    return result;
  }
  
  /**
   * @param courseId This is Id of the course you want to have enrolled users of
   * @return Returns enrolled users for specified course 
   */
  public String core_enrol_get_enrolled_users(int courseId) throws ProtocolException, IOException {
    String urlParameters = "courseid=" + URLEncoder.encode(Integer.toString(courseId), "UTF-8");
    String result = restRequest("core_enrol_get_enrolled_users", urlParameters);
    return result;
  }
  
  /**
   * @param userId This is Id of the user you want to have the courses of
   * @return Returns courses where the specified user is enrolled in
   */
  public String core_enrol_get_users_courses(int userId) throws ProtocolException, IOException {
    String urlParameters = "userid=" + URLEncoder.encode(Integer.toString(userId), "UTF-8");
    String result = restRequest("core_enrol_get_users_courses", urlParameters);
    return result;
  }
  

  /**
   * @param courseId This is Id of the course you want to have grades of
   * @return Returns grades for all users, who are enrolled in the specified course 
   */
  public String gradereport_user_get_grade_items(int courseId) throws ProtocolException, IOException {
    
    String urlParameters = "courseid=" + URLEncoder.encode(Integer.toString(courseId), "UTF-8");
    String result = restRequest("gradereport_user_get_grade_items", urlParameters);
    return result;
  }
  
  /**
   * @param courseId This is Id of the course you want to have grades of
   * @param userId This is Id of the user you want to have grades of 
   * @return Returns grades for the specified course and user
   */
  public String gradereport_user_get_grade_items(int courseId, int userId) throws ProtocolException, IOException {
    
    String urlParameters = "courseid=" + URLEncoder.encode(Integer.toString(courseId), "UTF-8") + 
        "&userid=" + URLEncoder.encode(Integer.toString(userId), "UTF-8");
    String result = restRequest("gradereport_user_get_grade_items", urlParameters);
    return result;
  }
  
  /**
   * @param gradereport This is moodle data in json format for the grades
   * @param userinfo This is moodle data in json format for the user information
   * @return Returns an ArrayList of statements
   */
  public ArrayList<String> statementGenerator(String gradereport, String userinfo) throws JSONException {
    ArrayList<String> statements = new ArrayList<String>();
    
    //json parser
    JSONObject jsonGradeReport = new JSONObject(gradereport);
    JSONArray jsonUserGrades = (JSONArray) jsonGradeReport.get("usergrades");
    JSONArray jsonUserInfo = new JSONArray(userinfo);
    
    
    //for all users
    for (int i = 0; i < jsonUserGrades.length(); i++) {

      //the relevant user data
      String courseid;
      String userfullname;
      String userid;
      String email = null;
    
      JSONObject jsonUser = (JSONObject) jsonUserGrades.get(i);
      
      courseid = Integer.toString(jsonUser.getInt("courseid"));
      userfullname = jsonUser.getString("userfullname");
      userid = Integer.toString(jsonUser.getInt("userid"));
      
      for (int k = 0 ; k < jsonUserInfo.length(); k++) {
        JSONObject jsonInfo = (JSONObject) jsonUserInfo.get(k);
        if(Integer.toString(jsonInfo.getInt("id")).equals(userid)) {
          if (jsonInfo.get("email") != JSONObject.NULL)
            email = jsonInfo.getString("email");
          break;
        }
      }
      
      if(email == null) 
        email = userfullname.replace(" ", ".") + userid + "@example.com";
      
      JSONArray jsonGradeItems = (JSONArray) jsonUser.get("gradeitems");
      
      //for all gradeitems
      for(int j = 0; j < jsonGradeItems.length()-1; j++) {
        
        //relevant item data
        String itemname;
        String itemid;
        String itemmodule;
        String gradedatesubmitted = null;
        String percentageformatted = null;
        String feedback = null;

        JSONObject jsonItem = (JSONObject) jsonGradeItems.get(j);

        itemname = jsonItem.getString("itemname");
        
        itemid = Integer.toString(jsonItem.getInt("id"));
        
        itemmodule = jsonItem.getString("itemmodule");
        
        if (jsonItem.get("gradedatesubmitted") != JSONObject.NULL) {
          Date date = new Date();
          date.setTime((long)jsonItem.getInt("gradedatesubmitted")*1000);
          
          SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
          gradedatesubmitted = sdf.format(date) + "Z";
        }
        
        //get rid of % and shift it so it's 0.XXXX
        if (jsonItem.get("percentageformatted") != JSONObject.NULL && !jsonItem.getString("percentageformatted").equals("-")) {
          double d = Double.parseDouble(jsonItem.getString("percentageformatted").replaceAll("%", "").trim());
          d = d/100;
          percentageformatted = Double.toString(d);
        }
        
        //get rid of <p></p>
        if (jsonItem.get("feedback") != JSONObject.NULL)
          feedback = jsonItem.getString("feedback").replaceAll("<p>", "").replaceAll("</p>", "");
        
        // create statement 
        if (percentageformatted != null) {
          String statement = "{"
              + "\"actor\": {"
                + "\"objectType\": \"Agent\","
                + "\"mbox\": \"mailto:" + email + "\","
                + "\"name\": \"" + userfullname + "\""
              + "},"
                + "\"verb\": {"
                + "\"id\": \"http://example.com/xapi/completed\","
                + "\"display\": {\"en-US\": \"completed\"}"
              + "},"
              + "\"object\": {"
                + "\"id\": \"" + domainName + "/mod/" + itemmodule + "/view.php?id=" + itemid +"\","
                + "\"definition\": {"
                  + "\"type\": \"" + domainName + "/course/view.php?id=" + courseid +"\","
                  + "\"name\": {\"en-US\": \"" + itemname + "\"},"
                  + "\"description\": {\"en-US\": \"This is a test\"}"
                + "},"
                + "\"objectType\": \"Activity\""
              + "},"
              + "\"result\": {"
                + "\"completion\": true,"
                +"\"score\": {"
                  + "\"scaled\": " + percentageformatted
                  + "}";
            if(feedback != null)
              statement = statement + ","
                  + "\"response\": \"" + feedback + "\"";
            
            statement = statement + "}";
            
          if(gradedatesubmitted != null) {
            statement = statement + ","
                + "\"timestamp\": \"" + gradedatesubmitted + "\"";
          }
          
          statement = statement + "}";
          statements.add(statement);
        }
      }
      
    }
    
    return statements;
  }
  
  
}
