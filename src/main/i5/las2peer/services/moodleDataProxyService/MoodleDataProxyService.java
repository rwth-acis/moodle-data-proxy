package i5.las2peer.services.moodleDataProxyService;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONArray;
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
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleUser;
import i5.las2peer.services.moodleDataProxyService.util.UserWhitelistHelper;
import i5.las2peer.services.moodleDataProxyService.util.WhitelistParseException;
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
				version = "1.3.0",
				description = "A proxy for requesting data from moodle",
				contact = @Contact(
						name = "Boris Jovanovic",
						email = "jovanovic.boris@rwth-aachen.de")))

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
	private static ScheduledExecutorService userUpdateThread = null;

	private final static L2pLogger logger = L2pLogger.getInstance(MoodleDataProxyService.class.getName());
	private static MoodleWebServiceConnection moodle = null;
	private static MoodleStatementGenerator statements = null;
	private static Context context = null;

	private final static int MOODLE_DATA_STREAM_PERIOD = 60; // Every minute
	private final static int MOODLE_USER_INFO_UPDATE_PERIOD = 3600; // Every hour
	private static long lastChecked = 0;
	private static String email = "";
	
	private static boolean userWhitelistEnabled = false;
	private static List<String> userWhitelist = new ArrayList<>();

	private final static Set<String> REQUIRED_MOODLE_FUNCTIONS = new HashSet<String>(Arrays.asList(
		"core_course_get_courses",
		"core_enrol_get_enrolled_users",
		"core_webservice_get_site_info",
		"core_user_get_users_by_field",
		"core_course_get_course_module",
		"core_course_get_updates_since",
		"gradereport_user_get_grade_items",
		"mod_quiz_get_user_attempts",
		"mod_forum_get_forum_discussions",
		"mod_forum_get_discussion_posts",
		"local_t4c_get_recent_course_activities"
	));
	private static Set<String> enabledMoodleFunctions = new HashSet<>();

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
			L2pLogger.setGlobalConsoleLevel(Level.INFO);
		}

		moodle = new MoodleWebServiceConnection(moodleToken, moodleDomain);
		statements = new MoodleStatementGenerator(moodle);

		JSONObject webserviceInfoResponse = new JSONObject();
		try {
			webserviceInfoResponse = moodle.core_webservice_get_site_info();
		} catch (IOException e) {
			logger.severe("Unable to call core_webservice_get_site_info Moodle function.");
			e.printStackTrace();
		}

		if (email.equals("")) {
			try {
				int userID = webserviceInfoResponse.getInt("userid");
				JSONObject user = moodle.core_user_get_users_by_field("id", userID);
				email = user.getString("email");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		updateCourseList();

		moodleFunctionSurvey(webserviceInfoResponse);

		// Change variable to false in orded to check proxy functionality independent
		// from the verification service.
		usesBlockchainVerification = true;
		
		// check if whitelist file exists and enable whitelist in that case
		this.userWhitelistEnabled = UserWhitelistHelper.isWhitelistEnabled();
		if(this.userWhitelistEnabled) {
			logger.info("Found user whitelist file, enabling whitelist...");
			try {
				this.userWhitelist = UserWhitelistHelper.loadWhitelist();
				logger.info("User whitelist is enabled and contains " + this.userWhitelist.size() + " items.");
			} catch (IOException | WhitelistParseException e) {
				logger.severe("An error occurred while loading the whitelist from file.");
				e.printStackTrace();
			}
		} else {
			logger.info("User whitelist is not enabled.");
		}
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

	/**
	 * Method that runs a check whether the given Moodle token has access to all
	 * the necessary Moodle API Web service functions. Results are then logged.
	 *
	 * @param webserviceInfoResponse The response JSON Object of the core_webservice_get_site_info
	 * Moodle function.
	 * 
	 * @return void
	 *
	 */
	private void moodleFunctionSurvey(JSONObject webserviceInfoResponse) {
		if (!webserviceInfoResponse.isEmpty()) {
			logger.info("Checking if required Moodle web service functions are enabled with token...");
			JSONArray functions = new JSONArray();
			try {
				functions = webserviceInfoResponse.getJSONArray("functions");				
			} catch (Exception e) {
				logger.severe("Error while parsing response from Moodle function core_webservice_get_site_info!");
				return;
			}
			Set<String> enabledFunctionSet = new HashSet<String>();
			logger.info("Enabled functions:");
			for (Object item : functions) {
				String webservice;
				try {
					webservice = ((JSONObject) item).getString("name");			
				} catch (Exception e) {
					logger.severe("Error while parsing response from Moodle function core_webservice_get_site_info!");
					return;
				}
				enabledFunctionSet.add(webservice);

				//Prints out the functions with ticks if required, else without them
				if (REQUIRED_MOODLE_FUNCTIONS.contains(webservice)) {
					logger.info(webservice + " " + (char) 10003);
				}
				else {
					logger.info(webservice);
				}
			}

			// Save enabled functions into the static set for reference
			enabledMoodleFunctions = enabledFunctionSet;

			if (enabledFunctionSet.containsAll(REQUIRED_MOODLE_FUNCTIONS)) {
				logger.info("All required Moodle functions enabled.");
			}
			else {
				Set<String> missingFunctions = new HashSet<String>(REQUIRED_MOODLE_FUNCTIONS);
				missingFunctions.removeAll(enabledFunctionSet);
				logger.warning("The following Moodle functions have not been enabled with the used token:");
				for (String item : missingFunctions) {
					logger.warning(item + " " + (char) 10008);
				}
			}

		}

	}

	/**
	 * Method that checks if a Moodle function is enabled.
	 * Uses the enabledMoodleFunctions set which is populated
	 * during the moodleFunctionSurvey.
	 * 
	 * @param functionName Full name of the moodle function,
	 * e.g. core_course_get_course_module.
	 * @return True if function is enabled, false otherwise.
	 */
	public static boolean isMoodleFunctionEnabled(String functionName) {
		return enabledMoodleFunctions.contains(functionName);
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

		if (!isMainAgentMoodleTokenOwner()) {
			return Response.status(Status.FORBIDDEN).entity("Access denied").build();
		}
		
		if (dataStreamThread == null) {
			context = Context.get();
			dataStreamThread = Executors.newSingleThreadScheduledExecutor();
			dataStreamThread.scheduleAtFixedRate(new DataStreamThread(), 0, MOODLE_DATA_STREAM_PERIOD,
					TimeUnit.SECONDS);
			userUpdateThread = Executors.newSingleThreadScheduledExecutor();
			userUpdateThread.scheduleAtFixedRate(new UserUpdateThread(), 0, MOODLE_USER_INFO_UPDATE_PERIOD,
					TimeUnit.SECONDS);
			return Response.status(Status.OK).entity("Thread started.").build();
		} else {
			return Response.status(Status.BAD_REQUEST).entity("Thread already running.").build();
		}
	}
	
	/**
	 * Method to set a user whitelist for the proxy.
	 * Takes a CSV file containing email addresses of users that should be on the whitelist.
	 * Only data of these users is sent to MobSOS.
	 * @param whitelistInputStream CSV file
	 * @return
	 */
	@POST
	@Path("/setWhitelist")
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@ApiResponses(
			value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Updated user whitelist."),
					  @ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "Authorization required."),
					  @ApiResponse(code = HttpURLConnection.HTTP_FORBIDDEN, message = "Access denied.") })
	public Response setUserWhitelist(@FormDataParam("whitelist") InputStream whitelistInputStream) {
		if (Context.getCurrent().getMainAgent() instanceof AnonymousAgent) {
			return Response.status(Status.UNAUTHORIZED).entity("Authorization required.").build();
		}
		
		if (!isMainAgentMoodleTokenOwner()) {
			return Response.status(Status.FORBIDDEN).entity("Access denied.").build();
		}
		
		try {			
			this.userWhitelist = UserWhitelistHelper.updateWhitelist(whitelistInputStream);
			this.userWhitelistEnabled = true;
			logger.info("Enabled whitelist containing " + this.userWhitelist.size() + " items.");
			return Response.status(200).entity("Enabled whitelist containing " + 
			    this.userWhitelist.size() + " items.").build();
		} catch (IOException | WhitelistParseException e) {
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(e.getMessage()).build();
		}
	}
	
	/**
	 * Method to disable the user whitelist.
	 * @return
	 */
	@POST
	@Path("/disableWhitelist")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(
			value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Disabled user whitelist."),
					  @ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "Authorization required."),
					  @ApiResponse(code = HttpURLConnection.HTTP_FORBIDDEN, message = "Access denied."),
					  @ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Unable to disable whitelist.")})
	public Response disableUserWhitelist() {
		if (Context.getCurrent().getMainAgent() instanceof AnonymousAgent) {
			return Response.status(Status.UNAUTHORIZED).entity("Authorization required.").build();
		}
		
		if (!isMainAgentMoodleTokenOwner()) {
			return Response.status(Status.FORBIDDEN).entity("Access denied.").build();
		}
		
		boolean success = UserWhitelistHelper.removeWhitelistFile();
		if(success) {
			this.userWhitelist = new ArrayList<>();
			this.userWhitelistEnabled = false;
			return Response.status(Status.OK).entity("Disabled whitelist.").build();
		} else {
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity("Unable to disable whitelist.").build();
		}
	}
	

	/**
	 * Thread which periodically checks all courses for events,
	 * creates xAPI statements of new events and sends them to Mobsos.
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

			for (int courseID : courses) {
				try {
					logger.info("Getting updates since " + lastChecked);
					ArrayList<String> updates = statements.courseUpdatesSince(courseID, lastChecked);
					for (String update : updates) {
						if (!checkUserConsent(update)) {
							// Skip this update if acting user did not consent to data extraction.
							continue;
						}

						// handle timestamps from the future next time
						if (checkXAPITimestamp(update) < now)
							context.monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_2, update);
						else {
							logger.warning("Update not being sent due to it happening in the future: " + update);
						}
					}
					logger.info("Sent " + updates.size() + " messages for course " + courseID);
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
			}
			
			String userEmail = statementJSON.getJSONObject("actor").getJSONObject("account").getString("name");
			
			if(usesBlockchainVerification) {
				String verb = statementJSON.getJSONObject("verb").getJSONObject("display").getString("en-US");
				
				logger.warning("Checking consent for email: " + userEmail + " and action: " + verb + " ...");
				boolean consentGiven = false;
				try {
					consentGiven = (boolean) context.invokeInternally("i5.las2peer.services.learningAnalyticsVerification.LearningAnalyticsVerificationService@1.0.1", "checkUserConsent", userEmail, verb);
					if (consentGiven) {
						// If consent for data extraction is given create log entry with included data
						context.invokeInternally("i5.las2peer.services.learningAnalyticsVerification.LearningAnalyticsVerificationService@1.0.1", "createLogEntry", message);
					}
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
				logger.warning("Consent given: " + consentGiven);
				return consentGiven;
			}
			
			if(userWhitelistEnabled) {
				return userWhitelist.contains(userEmail);
			}
			return true;
		}
	}

	/**
	 * Thread which periodically updates cached user info.
	 *
	 * @return void
	 *
	 */
	private class UserUpdateThread implements Runnable {

		@Override
		public void run() {
			Map<Integer, MoodleUser> tmpUserMap = new HashMap<>();

			for (int courseID : courses) {
				JSONArray usersJSON;
				try {
					usersJSON = moodle.core_enrol_get_enrolled_users(courseID);	
				} catch (Exception e) {
					logger.severe("Error while reaching core_enrol_get_enrolled_users while updating user info for course ID:"
						+ courseID);
					continue;
				}
				
				for (Object user : usersJSON) {
					JSONObject userJSON = (JSONObject) user;
					int userID;
					try {
						userID = userJSON.getInt("id");
					} catch (Exception e) {
						logger.severe("Could not get user ID from core_enrol_get_enrolled_users while updating user. "
						+ e.getStackTrace());
						continue;
					}
					MoodleUser muser;
					if (!tmpUserMap.containsKey(userID)) {
						muser = new MoodleUser(userJSON);
					}
					else {
						muser = tmpUserMap.get(userID);
					}
					// add roles
					JSONArray rolesJSON = null;
					try {
						rolesJSON = userJSON.getJSONArray("roles");
						muser.putCourseRoles(courseID, rolesJSON);
					} catch (Exception e) {
						logger.severe("Could not get user role from core_enrol_get_enrolled_users while updating user ID: "
						+ userID + " " + e.getStackTrace());
					}
					
					tmpUserMap.put(userID, muser);
				}
			}
			
			statements.setUserMap(tmpUserMap);
		}

	}
	
	/**
	 * Checks whether the main las2peer agent is the owner of the moodle token, i.e. whether the 
	 * email of the main las2peer agent is equal to the email of the moodle user who created the token.
	 * @return Whether the main las2peer agent is the owner of the moodle token.
	 */
	private boolean isMainAgentMoodleTokenOwner() {
		UserAgentImpl u = (UserAgentImpl) Context.getCurrent().getMainAgent();
		String uEmail = u.getEmail();
		return uEmail.equals(email);
	}
}
