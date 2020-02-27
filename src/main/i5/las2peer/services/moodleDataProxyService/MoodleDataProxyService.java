package i5.las2peer.services.moodleDataProxyService;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONException;
import org.json.JSONObject;

import i5.las2peer.api.Context;
import i5.las2peer.api.ManualDeployment;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleWebServiceConnection;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.SwaggerDefinition;




@Api
@SwaggerDefinition(
    info = @Info(
        title = "Moodle Data Proxy Service",
        version = "1.0",
        description = "A proxy for requesting data from moodle",
        contact = @Contact(
            name = "Philipp Roytburg",
            email = "philipp.roytburg@rwth-aachen.de")))

/**
 * 
 * This service is for requesting moodle data and creating corresponding xAPI statement. It sends REST requests to moodle  
 * on basis of implemented functions in MoodleWebServiceConnection.
 * 
 */
@ManualDeployment
@ServicePath("mc")
public class MoodleDataProxyService extends RESTService {
  
  private String moodleDomain;
  private String moodleToken;

  private MoodleWebServiceConnection moodle;
  
  private static Map<Integer, ArrayList<String>> oldCourseStatements = new HashMap<Integer, ArrayList<String>>();

  
  /**
   * 
   * Constructor of the Service. Loads the database values from a property file and initiates values for a moodle connection.
   * 
   */
  public MoodleDataProxyService() {
    setFieldValues(); // This sets the values of the configuration file
    moodle = new MoodleWebServiceConnection(moodleToken, moodleDomain);
  }
  
  
  /**
   * A function that is called by the user to send processed moodle to a mobsos data processing instance. 
   *
   * @param courseId an integer indicating the id of a moodle course
   * 
   * @return a response message if everything went ok
   * 
   */
  @POST
  @Path("/moodle-data/{courseId}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiResponses(
          value = { @ApiResponse(
                  code = HttpURLConnection.HTTP_OK,
                  message = "Moodle connection is initiated") })
  public Response initiateMoodleConnection(@PathParam("courseId") int courseId){

    String gradeReport = "";
    String userInfo = "";
    String quizzes = "";
    String courses = "";
    ArrayList<String> newStatements;

    // Getting the moodle data
    try {
      gradeReport = moodle.gradereport_user_get_grade_items(courseId);
      userInfo = moodle.core_enrol_get_enrolled_users(courseId);
      quizzes = moodle.mod_quiz_get_quizzes_by_courses(courseId);
      courses = moodle.core_course_get_courses();
      newStatements = moodle.statementGenerator(gradeReport, userInfo, quizzes, courses);

      MoodleDataProxyService.oldCourseStatements.put(courseId, newStatements);
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(500).entity("An error occurred with requesting Moodle data").build();
    } catch (JSONException e1) {
      e1.printStackTrace();
      return Response.status(500).entity("An error occurred with generating the xAPI statement").build();
    }

    for (String statement : newStatements) {
      Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_2, statement);
    }
    return Response.ok().entity("Moodle data was sent to MobSOS.").build();
  }

  /**
   * A function that is called by a bot to get the general information of a course
   *
   * @param courseId the id of the Moodle course
   * @return a response message with the course information
   *
   */
  @GET
  @Path("/course-summary/{courseId}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiResponses(
          value = { @ApiResponse(
                  code = HttpURLConnection.HTTP_OK,
                  message = "Connection works") })
  public Response getCourseSummary(@PathParam("courseId") int courseId) {
    String courses = "";
    String courseSummary = "";

    //Retrieving the moodle data
    try {
      courses = moodle.core_course_get_courses();
      courseSummary = moodle.getCourseSummaryById(Integer.toString(courseId), courses);
    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(500).entity("An error occurred with requesting moodle data").build();
    }

    //Creating a JSON Object
    JSONObject jsonObject = createJSONObject("getCourseSummary",
            Context.getCurrent().getMainAgent().getIdentifier(),courseId,courseSummary);

    Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_5, jsonObject.toString());
    initiateMoodleConnection(courseId);
    return Response.ok().entity(courseSummary).build();
  }

  /**
   * A function that is called by a bot to see if there is new student data for a course
   *
   * @param courseId an integer indicating the id of a moodle course
   * @return a response message with the students that have completed new assignments
   *
   */
  @GET
  @Path("/moodle-changes/{courseId}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiResponses(
          value = { @ApiResponse(
                  code = HttpURLConnection.HTTP_OK,
                  message = "Moodle connection is initiated") })
  public Response getChanges(@PathParam("courseId") int courseId){
    String gradeReport = "";
    String userInfo = "";
    String quizzes = "";
    String courses = "";
    String resultText = "";
    ArrayList<String> newStatements;
    ArrayList<String> oldStatements = new ArrayList<>();

    // Retrieving moodle data
    try {
      gradeReport = moodle.gradereport_user_get_grade_items(courseId);
      userInfo = moodle.core_enrol_get_enrolled_users(courseId);
      quizzes = moodle.mod_quiz_get_quizzes_by_courses(courseId);
      courses = moodle.core_course_get_courses();

      newStatements = moodle.statementGenerator(gradeReport, userInfo, quizzes, courses);

    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(500).entity("An error occured with requesting moodle data").build();
    } catch (JSONException e1) {
      e1.printStackTrace();
      return Response.status(500).entity("An error occured with generating the xAPI statement").build();
    }

    if (MoodleDataProxyService.oldCourseStatements.get(courseId) != null) {
      oldStatements = MoodleDataProxyService.oldCourseStatements.get(courseId);
    }

    // Prepare a union
    List<String> union = new ArrayList<>(oldStatements);
    union.addAll(newStatements);

    // Prepare an intersection
    List<String> intersection = new ArrayList<>(oldStatements);
    intersection.retainAll(newStatements);

    // Subtract the intersection from the union
    union.removeAll(intersection);

    ArrayList<String> students = new ArrayList<>();

    for (String n : union) {
      JSONObject changes = new JSONObject(n);
      JSONObject actor = (JSONObject) changes.get("actor");
      String name = actor.get("name").toString();
      if (!students.contains(name)) {
        students.add(name);
      }
    }

    if (students.size() == 0) {
      resultText = "There is no new student data for this course!";
    } else {
      for (String student: students) {
        resultText += student + " has completed a new assignment!\n";
      }
    }

    JSONObject jsonObject = createJSONObject("getChanges",
            Context.getCurrent().getMainAgent().getIdentifier(),courseId,resultText);

    Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_5, jsonObject.toString());

    return Response.ok().entity(resultText).build();
  }

  /*
   * This function creates json object from the parameters
   * functionName, agentIdentifier, courseId and resultText
   */
  private JSONObject createJSONObject(String functionName, String agentIdentifier, int courseId, String resultText){

    JSONObject jsonObject = new JSONObject();

    jsonObject.put("functionName", functionName);
    jsonObject.put("serviceAlias", "mc");
    jsonObject.put("uid", agentIdentifier);

    JSONObject attributes = new JSONObject();

    attributes.put("courseId", courseId);
    attributes.put("result", resultText);

    jsonObject.put("attributes", attributes);


    //return ok message
    return jsonObject;
  }

}
