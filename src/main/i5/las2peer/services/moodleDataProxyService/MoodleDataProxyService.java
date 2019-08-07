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
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;




// TODO Adjust the following configuration
@Api
@SwaggerDefinition(
    info = @Info(
        title = "las2peer Template Service",
        version = "1.0",
        description = "A las2peer Template Service for demonstration purposes.",
        termsOfService = "http://your-terms-of-service-url.com",
        contact = @Contact(
            name = "John Doe",
            url = "provider.com",
            email = "john.doe@provider.com"),
        license = @License(
            name = "your software license name",
            url = "http://your-software-license-url.com")))


@ManualDeployment
@ServicePath("mc")
public class MoodleDataProxyService extends RESTService {
  
  private String moodleDomain;
  private String moodleToken;

  private MoodleWebServiceConnection moodle;
  
  
  public MoodleDataProxyService() {
    setFieldValues();
    moodle = new MoodleWebServiceConnection(moodleToken, moodleDomain);
  }
  
  
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
    try {
      gradereport = moodle.gradereport_user_get_grade_items(courseId);
      userinfo = moodle.core_enrol_get_enrolled_users(courseId);
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    ArrayList<String> newstatements = new ArrayList<String>();
    try {
      newstatements = moodle.statementGenerator(gradereport, userinfo);
    } catch (JSONException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    
    for(int i = 0; i < newstatements.size(); i++) {
      String statement = newstatements.get(i);
      Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_2, statement);  
    }
    return Response.ok().entity("Moodle data was sent to MobSOS.").build();
  }
  
}
