package i5.las2peer.services.moodleDataProxyService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONException;

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
          message = "Moodle connection is initiaded") })
  public Response initMoodleConnection(@PathParam("courseId") int courseId) throws ProtocolException, IOException{
    String gradereport = "";
    String userinfo = "";
    String quizzes = "";
    String courses = "";
    try { // try getting the moodle data
      gradereport = moodle.gradereport_user_get_grade_items(courseId);
      userinfo = moodle.core_enrol_get_enrolled_users(courseId);
      quizzes = moodle.mod_quiz_get_quizzes_by_courses(courseId);
      courses = moodle.core_course_get_courses();

    } catch (IOException e) {
      e.printStackTrace();
      return Response.status(500).entity("An error occured with requesting moodle data").build();
    }
    
    ArrayList<String> newstatements = new ArrayList<String>();
    try { // try to create an xAPI statement out of the moodle data
      newstatements = moodle.statementGenerator(gradereport, userinfo, quizzes, courses);
    } catch (JSONException e1) {
      e1.printStackTrace();
      return Response.status(500).entity("An error occured with generating the xAPI statement").build();
    }
    
    // send all statements to mobsos
    for(int i = 0; i < newstatements.size(); i++) {
      String statement = newstatements.get(i);
      Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_2, statement);  
    }
    //return ok message
    return Response.ok().entity("Moodle data was sent to MobSOS.").build();
  }
  
}
