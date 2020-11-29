package i5.las2peer.services.moodleDataProxyService;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TimeZone;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import i5.las2peer.api.Context;
import i5.las2peer.api.ManualDeployment;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.api.security.AnonymousAgent;
import i5.las2peer.logging.L2pLogger;
import java.util.logging.Level;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleWebServiceConnection;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleStatementGenerator;
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
				version = "1.0.0",
				description = "A proxy for requesting data from moodle",
				contact = @Contact(
						name = "Alexander Tobias Neumann",
						email = "neumann@rwth-aachen.de")))

/**
 *
 * This service is for requesting moodle data and creating corresponding xAPI statement. It sends REST requests to
 * moodle on basis of implemented functions in MoodleWebServiceConnection.
 *
 */
@ManualDeployment
@ServicePath("moodle")
public class MoodleDataProxyService extends RESTService {

	private String moodleDomain;
	private String moodleToken;
	private String courseList;
	private boolean usesBlockchainVerification;

	private static HashSet<Integer> courses = new HashSet<Integer>();
	private static ScheduledExecutorService dataStreamThread = null;

	private final static L2pLogger logger = L2pLogger.getInstance(MoodleDataProxyService.class.getName());
	private static MoodleWebServiceConnection moodle = null;
	private static MoodleStatementGenerator statements = null;
	private static Context context = null;

	private final static int MOODLE_DATA_STREAM_PERIOD = 60; // Every minute
	private static long lastChecked = 0;
	private static String email = "";

	/**
	 *
	 * Constructor of the Service. Loads the database values from a property file
	 * and initiates values for a moodle connection.
	 *
	 */
	public MoodleDataProxyService() {
		setFieldValues(); // This sets the values of the configuration file
		if (lastChecked == 0) {
			// Get current time
			TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			Instant instant = timestamp.toInstant();
			lastChecked = instant.getEpochSecond();
			L2pLogger.setGlobalConsoleLevel(Level.WARNING);
		}

		moodle = new MoodleWebServiceConnection(moodleToken, moodleDomain);
		statements = new MoodleStatementGenerator(moodle);

		if (email.equals("")) {
			try {
				int userId = moodle.core_webservice_get_site_info().getInt("userid");
				JSONObject user = moodle.core_user_get_users_by_field("id", userId);
				email = user.getString("email");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		updateCourseList();
	}

	private void updateCourseList() {
		courses.clear();
		if (courseList != null && courseList.length() > 0) {
			try {
				logger.info("Reading courses from provided list.");
				String[] idStrings = courseList.split(",");
				for (String courseid : idStrings) {
					courses.add(Integer.parseInt(courseid));
				}
				logger.info("Updating course list was successful: " + courses);
				return;
			} catch (Exception e) {
				logger.severe("Reading course list failed");
				e.printStackTrace();
			}
		}
		try {
			logger.info("Getting courses from Moodle.");
			JSONArray coursesJSON = moodle.core_course_get_courses();
			for (Object course : coursesJSON) {
				courses.add(((JSONObject) course).getInt("id"));
			}
			logger.info("Updating course list was successful: " + courses);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.severe("Reading course list failed");
			e.printStackTrace();
		}
	}

	@POST
	@Path("/")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(
			value = { @ApiResponse(
					code = HttpURLConnection.HTTP_OK,
					message = "Moodle connection is initiaded") })
	@RolesAllowed("authenticated")
	public Response initMoodleProxy() {
		if (Context.getCurrent().getMainAgent() instanceof AnonymousAgent) {
			return Response.status(Status.UNAUTHORIZED).entity("Authorization required.").build();
		}

		// TODO: If flag is set, make sure the privacy control service is up and running before initiating.
		if (usesBlockchainVerification) {
			logger.warning("Proxy service uses blockchain verification and consent checks");
		}

		UserAgentImpl u = (UserAgentImpl) Context.getCurrent().getMainAgent();
		String uEmail = u.getEmail();

		if (!uEmail.equals(email)) {
			return Response.status(Status.FORBIDDEN).entity("Access denied").build();
		}
		if (dataStreamThread == null) {
			context = Context.get();
			dataStreamThread = Executors.newSingleThreadScheduledExecutor();
			dataStreamThread.scheduleAtFixedRate(new DataStreamThread(), 0, MOODLE_DATA_STREAM_PERIOD,
					TimeUnit.SECONDS);
			return Response.status(Status.OK).entity("Thread started.").build();
		} else {
			return Response.status(Status.BAD_REQUEST).entity("Thread already running.").build();
		}
	}

	/**
	 * Thread which periodically checks all courses for new quiz attempts,
	 * creates xAPI statements of new attempts and sends them to Mobsos.
	 *
	 * @return void
	 *
	 */
	private class DataStreamThread implements Runnable {
		@Override
		public void run() {
			TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

			// Get current time
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			long now = timestamp.toInstant().getEpochSecond();

			for (int courseId : courses) {
				try {
					logger.info("Getting updates since " + lastChecked);
					ArrayList<String> updates = statements.courseUpdatesSince(courseId, lastChecked);
					short updateCounter = 0;
					for (String update : updates) {
						if (usesBlockchainVerification && !checkUserConsent(update)) {
							// Skip this update if acting user did not consent to data extraction.
							continue;
						}

						// handle timestamps from the future next time
						if (checkXAPITimestamp(update) > now) {
							logger.warning("Current timestamp (" + now + ") smaller than " +
									"timestamp (" + checkXAPITimestamp(update) + ") of update: " + update);
						}
						context.monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_2, update);
						updateCounter++;
					}
					logger.info("Sent " + updateCounter + " messages for course " + courseId);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			lastChecked = now;
		}

		private long checkXAPITimestamp(String message) {
			String statement = message.split("\\*")[0];
			JSONObject statementJSON;
			try {
				statementJSON = new JSONObject(statement);
			} catch (Exception e) {
				logger.severe("Error pasing message to JSON: " + message);
				return 0;
			}
			if (statementJSON.isNull("timestamp")) {
			}
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
				Date dt = sdf.parse(statementJSON.getString("timestamp"));
				return dt.getTime();
			} catch (Exception e) {
				logger.severe("Couldn't parse timestamp of message: " + message + e);
				return 0;
			}
		}

		private boolean checkUserConsent(String message) {
			String statement = message.split("\\*")[0];
			JSONObject statementJSON;
			try {
				statementJSON = new JSONObject(statement);
			} catch (Exception e) {
				logger.severe("Error parsing message to JSON: " + message);
				return false;
			}

			if (statementJSON.isNull("actor")) {
				logger.warning("Message does not seem to contain personal data.");
				return true;
			} else {
				String userEmail = statementJSON.getJSONObject("actor").getJSONObject("account").getString("name");
				String verb = statementJSON.getJSONObject("verb").getJSONObject("display").getString("en-US");

				logger.warning("Checking consent for email: " + userEmail + " and action: " + verb + " ...");
				boolean consentGiven = false;
				try {
					consentGiven = (boolean) context.invokeInternally("i5.las2peer.services.learningAnalyticsVerification.LearningAnalyticsVerificationService@1.0.0", "checkUserConsent", userEmail, verb);
					if (consentGiven) {
						// If consent for data extraction is given create log entry with included data
						context.invokeInternally("i5.las2peer.services.learningAnalyticsVerification.LearningAnalyticsVerificationService@1.0.0", "createLogEntry", message);
					}
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
				logger.warning("Consent given: " + consentGiven);
				return consentGiven;
			}
		}
	}
}
