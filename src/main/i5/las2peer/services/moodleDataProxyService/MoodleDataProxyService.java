package i5.las2peer.services.moodleDataProxyService;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TimeZone;
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
import jdk.nashorn.internal.ir.ThrowNode;

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
//			L2pLogger.setGlobalConsoleLevel(Level.WARNING);
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
					for (String update : updates) {
						// handle timestamps from the future next time
						if (checkXAPITimestamp(update) < now)
							context.monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_2, update);
						else {
							logger.warning("Update not being sent due to it happening in the future: " + update);
						}
					}
					logger.info("Sent " + updates.size() + " messages for course " + courseId);
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
				logger.severe("Couldn't get timestamp of message: " + message);
				return 0;
			}

			Object timestampObject = statementJSON.get("timestamp");
			if (timestampObject instanceof Integer) {
				return (long) timestampObject;
			}
			else if (timestampObject instanceof String) {
				try {
					OffsetDateTime odt = OffsetDateTime.parse((String) timestampObject);
					return odt.toEpochSecond();
				} catch (Exception e) {
					logger.severe("Could not parse DateTime format: " + message);
				}
			}
			else {
				logger.severe("Unkown timestamp format: " + message);
			}
			return 0;
		}
	}
}
