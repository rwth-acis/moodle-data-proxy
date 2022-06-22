package i5.las2peer.services.moodleDataProxyService;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.sql.SQLException;
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
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import i5.las2peer.api.security.Agent;
import i5.las2peer.api.security.AnonymousAgent;
import i5.las2peer.services.moodleDataProxyService.util.StoreManagementHelper;
import i5.las2peer.services.moodleDataProxyService.util.StoreManagementParseException;
import io.swagger.annotations.*;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import i5.las2peer.api.Context;
import i5.las2peer.api.ManualDeployment;
import i5.las2peer.api.execution.InternalServiceException;
import i5.las2peer.api.execution.ServiceAccessDeniedException;
import i5.las2peer.api.execution.ServiceInvocationFailedException;
import i5.las2peer.api.execution.ServiceMethodNotFoundException;
import i5.las2peer.api.execution.ServiceNotAuthorizedException;
import i5.las2peer.api.execution.ServiceNotAvailableException;
import i5.las2peer.api.execution.ServiceNotFoundException;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.logging.L2pLogger;
import java.util.logging.Level;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleWebServiceConnection;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO.MoodleUser;
import i5.las2peer.services.moodleDataProxyService.privacy_control_service_rmi.DataProcessingRequestResponse;
import i5.las2peer.services.moodleDataProxyService.privacy_control_service_rmi.StatementConsentHandler;
import i5.las2peer.services.moodleDataProxyService.util.UserWhitelistHelper;
import i5.las2peer.services.moodleDataProxyService.util.WhitelistParseException;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleStatementGenerator;

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

	private String operatorList;
	private static HashSet<String> operators = new HashSet<String>();

	private String moodleDomain;
	private String moodleToken;
	private String courseList;

	private static HashSet<Integer> courses = new HashSet<Integer>();
	private static ScheduledExecutorService dataStreamThread = null;
	private static ScheduledExecutorService userUpdateThread = null;

	private final static L2pLogger logger = L2pLogger.getInstance(MoodleDataProxyService.class.getName());
	private static MoodleWebServiceConnection moodle = null;
	private static MoodleStatementGenerator statements = null;
	private static Context context = null;

	private final static int MOODLE_DATA_STREAM_PERIOD = 60; // Every minute
	private final static int MOODLE_USER_INFO_UPDATE_PERIOD = 3600; // Every hour
	private final static int MOODLE_LAST_UPDATE_BUFFER_TIME = 3600; // Maximum
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

	@Override
	protected void initResources() {
		getResourceConfig().register(ProxyConfiguration.class);
		getResourceConfig().register(this);
	}

	/**
	 *
	 * Constructor of the Service. Loads the database values from a property file
	 * and initiates values for a moodle connection.
	 *
	 */
	public MoodleDataProxyService() {
		setFieldValues(); // This sets the values of the configuration file
		if (lastChecked == 0) {
			// Get time of last update
			lastChecked = getLastUpdateTime();
			L2pLogger.setGlobalConsoleLevel(Level.INFO);
		}
		// Ensure consistent form of URL in statements
		if (!moodleDomain.endsWith("/")) {
			moodleDomain = moodleDomain + "/";
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

		if (operatorList != null && operatorList.length() > 0) {
			try {
				logger.info("Reading operators from provided list.");
				String[] opStrings = operatorList.split(",");
				for (String op : opStrings) {
					operators.add(op.trim());
				}
				logger.info("Enabled operators: " + operators);
			} catch (Exception e) {
				logger.severe("Reading operators failed");
				e.printStackTrace();
			}
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

		// check if whitelist file exists and enable whitelist in that case
		userWhitelistEnabled = UserWhitelistHelper.isWhitelistEnabled();
		if(userWhitelistEnabled) {
			logger.info("Found user whitelist file, enabling whitelist...");
			try {
				userWhitelist = UserWhitelistHelper.loadWhitelist();
				logger.info("User whitelist is enabled and contains " + userWhitelist.size() + " items.");
			} catch (IOException | WhitelistParseException e) {
				logger.severe("An error occurred while loading the whitelist from file.");
				e.printStackTrace();
			}
		} else {
			logger.info("User whitelist is not enabled.");
		}

		// check if store assignment file exists and enable the assignment in that case
		if(StoreManagementHelper.isStoreAssignmentEnabled()) {
			logger.info("Found store assignment file, enabling assignment...");
			try {
				StoreManagementHelper.loadAssignments();
				logger.info("Store assignment is enabled.");
			} catch (IOException e) {
				logger.severe("An error occurred while loading the assignment from file.");
				e.printStackTrace();
			}
		} else {
			logger.info("Store assignment is not enabled.");
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
	
	/////////////////////////////////////////////////////////////////////////////////
	///                             Initialisation                                ///
	/////////////////////////////////////////////////////////////////////////////////
	

	@POST
	@Path("/")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(
			value = { @ApiResponse(
					code = HttpURLConnection.HTTP_OK,
					message = "Moodle connection is initiated") })
	@RolesAllowed("authenticated")
	public Response initMoodleProxy() {
		if (Context.getCurrent().getMainAgent() instanceof AnonymousAgent) {
			return Response.status(Status.UNAUTHORIZED).entity("Authorization required.").build();
		}

		if (!(isMainAgentMoodleTokenOwner() || isOperator())) {
			return Response.status(Status.FORBIDDEN).entity("Access denied").build();
		}

		monitorCall("moodle");

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
	
	/////////////////////////////////////////////////////////////////////////////////
	///                                 PCS RMI                                   ///
	/////////////////////////////////////////////////////////////////////////////////
	
	@GET
	@Path("/test")
	@Produces(MediaType.TEXT_PLAIN)
	public Response testRMI() {
		String retVal = null;
		logger.info("Got here");
		try {
			retVal = (String) Context.get().invokeInternally("i5.las2peer.services.privacy_control_service.PrivacyControlService@0.1.0", "dataProcessingRequest", "jovanovic.boris@rwth-aachen.de", "12");
		} catch (ServiceNotFoundException | ServiceNotAvailableException | InternalServiceException
				| ServiceMethodNotFoundException | ServiceInvocationFailedException | ServiceAccessDeniedException
				| ServiceNotAuthorizedException e) {
			// TODO Auto-generated catch block
			logger.info("fcl");
			e.printStackTrace();
			return Response.serverError().build();
		}
		return Response.ok(retVal).build();
	}
	
	
		
	/////////////////////////////////////////////////////////////////////////////////
	///                             Configuration                                 ///
	/////////////////////////////////////////////////////////////////////////////////
	
	@Api(
			value = "Proxy Configuration")
	@SwaggerDefinition(
			info = @Info(
					title = "Moodle Data-Proxy",
					version = "1.3.0",
					description = "A las2peer service for generating xAPI statements from Moodle updates.",
					termsOfService = "",
					contact = @Contact(
							name = "Leonardo Gomes da Matta e Silva",
							url = "",
							email = "leonardo.matta@rwth-aachen.de"),
					license = @License(
							name = "",
							url = "")))
	@Path("/config")
	public static class ProxyConfiguration {
		MoodleDataProxyService proxyservice = (MoodleDataProxyService) Context.get().getService();

		/**
		 * Method to set a user whitelist for the proxy.
		 * Takes a CSV file containing the email addresses of users that should be on the whitelist.
		 * Only data of these users is sent to MobSOS.
		 * @param whitelistInputStream Input stream of the passed CSV file.
		 * @return Status message
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

		if (!(proxyservice.isMainAgentMoodleTokenOwner() || proxyservice.isOperator())) {
			return Response.status(Status.FORBIDDEN).entity("Access denied.").build();
		}

			proxyservice.monitorCall("moodle/config/setWhitelist");

			try {
				userWhitelist = UserWhitelistHelper.updateWhitelist(whitelistInputStream);
				userWhitelistEnabled = true;
				logger.info("Enabled whitelist containing " + userWhitelist.size() + " items.");
				return Response.status(200).entity("Enabled whitelist containing " +
						userWhitelist.size() + " items.").build();
			} catch (IOException | WhitelistParseException e) {
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(e.getMessage()).build();
			}
		}

		/**
		 * Method to disable the user whitelist.
		 * @return Status message
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

		if (!(proxyservice.isMainAgentMoodleTokenOwner() || proxyservice.isOperator())) {
			return Response.status(Status.FORBIDDEN).entity("Access denied.").build();
		}

			proxyservice.monitorCall("moodle/config/disableWhitelist");

			boolean success = UserWhitelistHelper.removeWhitelistFile();
			if(success) {
				proxyservice.userWhitelist = new ArrayList<>();
				proxyservice.userWhitelistEnabled = false;
				return Response.status(Status.OK).entity("Disabled whitelist.").build();
			} else {
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity("Unable to disable whitelist.").build();
			}
		}

		/**
		 * Method for setting the assignments of Moodle courses to stores.
		 * Takes a properties file containing the course IDs and a comma-separated list of store client IDs as key-value
		 * pairs.
		 *
		 * @param storesInputStream Input stream of the passed properties file.
		 *
		 * @return Status message
		 */
		@POST
		@Path("/setStoreAssignment")
		@Produces(MediaType.TEXT_PLAIN)
		@Consumes(MediaType.MULTIPART_FORM_DATA)
		@ApiResponses(
				value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Updated store assignment."),
						@ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "Authorization required."),
						@ApiResponse(code = HttpURLConnection.HTTP_FORBIDDEN, message = "Access denied.") })
		public Response setStoreAssignment(@FormDataParam("storeAssignment") InputStream storesInputStream) {
		if (Context.getCurrent().getMainAgent() instanceof AnonymousAgent) {
			return Response.status(Status.UNAUTHORIZED).entity("Authorization required.").build();
		}

		if (!(proxyservice.isMainAgentMoodleTokenOwner() || proxyservice.isOperator())) {
			return Response.status(Status.FORBIDDEN).entity("Access denied.").build();
		}

			proxyservice.monitorCall("moodle/config/setStoreAssignment");

			try {
				StoreManagementHelper.updateAssignments(storesInputStream);
				logger.info("Added store assignment.");
				return Response.status(200).entity("Added store assignment with " +
						StoreManagementHelper.numberOfAssignments() + " assignments.").build();
			} catch (StoreManagementParseException e) {
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(e.getMessage()).build();
			}
		}

		/**
		 * Method to disable the store assignment.
		 *
		 * @return Status message
		 */
		@POST
		@Path("/disableStoreAssignment")
		@Produces(MediaType.TEXT_PLAIN)
		@ApiResponses(
				value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Disabled store assignment."),
						@ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "Authorization required."),
						@ApiResponse(code = HttpURLConnection.HTTP_FORBIDDEN, message = "Access denied."),
						@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Unable to disable store assignment.")})
		public Response disableStoreAssignment() {
		if (Context.getCurrent().getMainAgent() instanceof AnonymousAgent) {
			return Response.status(Status.UNAUTHORIZED).entity("Authorization required.").build();
		}

		if (!(proxyservice.isMainAgentMoodleTokenOwner() || proxyservice.isOperator())) {
			return Response.status(Status.FORBIDDEN).entity("Access denied.").build();
		}

			proxyservice.monitorCall("moodle/config/disableStoreAssignment");

			boolean success = StoreManagementHelper.removeAssignmentFile();
			if(success) {
				StoreManagementHelper.resetAssignment();
				return Response.status(Status.OK).entity("Disabled store assignment.").build();
			} else {
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity("Unable to disable store assignment.").build();
			}
		}
	}
	
	/////////////////////////////////////////////////////////////////////////////////
	///                                 Threads                                   ///
	/////////////////////////////////////////////////////////////////////////////////



	/**
	 * Thread which periodically checks all courses for events,
	 * creates xAPI statements of new events and sends them to Mobsos.
	 *
	 * @return void
	 *
	 */
	/**
	 * @author Boris
	 *
	 */
	private class DataStreamThread implements Runnable {
		@Override
		public void run() {
//			TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
//
//			// Get current time
//			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
//			long now = timestamp.toInstant().getEpochSecond();
			long lastTimestamp = 0;

			for (int courseID : courses) {
				try {
					logger.info("Getting updates since " + lastChecked);
					ArrayList<String> updates = statements.courseUpdatesSince(courseID, lastChecked);
					int numberOfUpdates = updates.size();
					for (String update : updates) {

						// Update time of last update
						long updateTimestamp = checkXAPITimestamp(update);
						if (lastTimestamp <= updateTimestamp) {
							lastTimestamp = updateTimestamp + 1;
						}
						
						logger.info("Message is: " + update);
						
						JSONObject messageObject = null;
						JSONObject statementJSON = null;
						try {
							messageObject = new JSONObject(update);
							statementJSON = (JSONObject) messageObject.get("statement");
						} catch (JSONException e) {
							logger.severe("Error parsing message to JSON: " + update);
							continue;
						}
						
						// Check if user has given consent to this statement's purpose
						JSONObject result = checkUserConsent(statementJSON, String.valueOf(courseID));
						if (result == null) {
							// Skip this update if acting user did not consent to data extraction.
							numberOfUpdates--;
							continue;
						}
						
						String pseudonym = result.getString("pseudonym");
						int purposeCode = result.getInt("purpose");						
						
						// If consent has been given, replace user info with received pseudonym before forwarding
						statementJSON = StatementConsentHandler.replaceUserInfoWithPseudonym(statementJSON, pseudonym);
						messageObject.put("statement", statementJSON);
						logger.info("Final statement is: " + statementJSON.toString());
						
						messageObject.put("purpose", purposeCode);
						messageObject.put("course", String.valueOf(courseID));
												
						update = messageObject.toString();
						
						boolean forwardStatementResult = false;
						try {
							logger.info("Attempting to contact Privacy Control Service for Forward Statement.");
							forwardStatementResult = (boolean) context.invokeInternally(
														"i5.las2peer.services.privacy_control_service.PrivacyControlService@0.1.0",
														"forwardStatement",
														update);
						} catch (ServiceNotFoundException | ServiceNotAvailableException | InternalServiceException
								| ServiceMethodNotFoundException | ServiceInvocationFailedException | ServiceAccessDeniedException
								| ServiceNotAuthorizedException e) {
							logger.severe("Error while contacting Privacy Control Service to Forward Statement.");
						}
						
						if (!forwardStatementResult) {
							logger.severe("Did not pass message correctly through PCS Forward Statement: " + update);
						} else {
							logger.info("Statement forwarded successfully!");
						}

						//OLD: context.monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_2, update);
						
					}
					logger.info("Sent " + numberOfUpdates + " messages for course " + courseID);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (lastTimestamp != 0) {
				lastChecked = lastTimestamp; // Changed to time of last update
			}
		}

		private long checkXAPITimestamp(String message) {
			JSONObject statementJSON = null;
			try {
				JSONObject msgObj = new JSONObject(message);
				statementJSON = (JSONObject) msgObj.get("statement");
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

		/**
		 * This method checks whether a user has given consent to the data contained
		 * in this statement being processed. It does this by making a RMI to the 
		 * Privacy Control Service. If the statement's processing purpose, which is 
		 * based on its verb, is among the purposes specified in the RMI response,
		 * it is considered that consent has been given, otherwise not.
		 * 
		 * @param statementJSON The statement JSON Object.
		 * @param courseID The ID of the course.
		 * @return A JSON object containing the user's pseudonym and the purpose code
		 * of the statement, if consent was given, otherwise null.
		 */
		private JSONObject checkUserConsent(JSONObject statementJSON, String courseID) {
			String userEmail = null;
			try {
				userEmail = statementJSON.getJSONObject("actor").getJSONObject("account").getString("name");
			} catch (JSONException e) {
				logger.severe("Error while retrieving actor details from statement: " + statementJSON.toString());
				return null;
			}
			
			DataProcessingRequestResponse pcsResponse = getUserConsentInfo(userEmail, courseID);
			if (pcsResponse == null) {
				// In this case the user is not registered in the course, or similar error
				return null;
			}
			
			Set<Integer> purposeCodes = pcsResponse.getPurposeCodes();
			String printout = "[";
			for (int code : purposeCodes) {
				printout += code + ",";
			}
			printout += "]";
			logger.info("Received codes: " + printout);
			
			// Determine which code the statement
			int statementPurpose = StatementConsentHandler.getStatementPurposeCode(statementJSON);
			logger.info("Statement purpose code is: " + statementPurpose);
						
			// See if user has given consent for that code
			if (!purposeCodes.contains(statementPurpose)) {
				return null;
			}
			
			if(userWhitelistEnabled) {
				if (!userWhitelist.contains(userEmail)) {
					logger.warning("Message not sent because user is not in the whitelist.");
					return null;
				}
			}
			
			String pseudonym = pcsResponse.getPseudonym();
			logger.info("User pseudonym is: " + pseudonym);
			
			JSONObject retVal =  new JSONObject();
			retVal.put("pseudonym", pseudonym);
			retVal.put("purpose", statementPurpose);
			return retVal;
		}
		
		private DataProcessingRequestResponse getUserConsentInfo(String userID, String courseID) {
			String response = null;
			try {
				logger.info("Attempting to contact Privacy Control Service for Data Processing Request"
						+ " through RMI.");
				response = (String) context
						.invokeInternally("i5.las2peer.services.privacy_control_service.PrivacyControlService@0.1.0",
										  "dataProcessingRequest",
										  userID,
										  courseID);
			} catch (ServiceNotFoundException | ServiceNotAvailableException | InternalServiceException
					| ServiceMethodNotFoundException | ServiceInvocationFailedException | ServiceAccessDeniedException
					| ServiceNotAuthorizedException e) {
				logger.severe("Error while contacting Privacy Control Service through RMI for: "
						+ "user:" + userID + " "
						+ "course:" + courseID);
				return null;
			}
			
			// Response is null if user is not enrolled in course or another other error has occurred
			if (response == null) {
				logger.info("DataProcessingRequestResponce returned null.");
				return null;
			}
			
			DataProcessingRequestResponse dprr = null;
			try {
				dprr = new DataProcessingRequestResponse(response);
			} catch (JSONException e) {
				logger.severe("Error while parsing Data Processing Request Response. "
						+ "Raw response was: " + response);
			}
			
			return dprr;
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
	
	/////////////////////////////////////////////////////////////////////////////////
	///                            Helper functions                               ///
	/////////////////////////////////////////////////////////////////////////////////
	

	/**
	 * Checks whether the main las2peer agent is in the list of authorized operators who are able to start or change
	 * settings of the proxy, e.g., set or disable the whitelist.
	 * @return Whether the main las2peer agent is an authorized operator.
	 */
	private boolean isOperator(){
		UserAgentImpl u = (UserAgentImpl) Context.getCurrent().getMainAgent();
		String uEmail = u.getEmail();
		return operators.contains(uEmail);
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

	/**
	 * Sends a monitoring message to the monitoring agent that logs the identifier of the caller of a method.
	 *
	 * @param method REST path of the Method called
	 */
	private void monitorCall(String method) {
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		Agent caller = Context.getCurrent().getMainAgent();

		JSONObject msgObj = new JSONObject();
		msgObj.put("timestamp", timestamp.toString());
		msgObj.put("agentId", caller.getIdentifier());
		msgObj.put("method", method);

		Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_1, msgObj.toString());
	}

	private long getLastUpdateTime() {
		// Get current time
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		Instant instant = timestamp.toInstant();
		long now = instant.getEpochSecond();
		long lastUpdate = 0;

		for (int courseid : courses) {
			try {
				JSONArray updates = moodle.local_t4c_get_recent_course_activities(courseid,
						now - MOODLE_LAST_UPDATE_BUFFER_TIME);
				if (!updates.isEmpty()) {
					JSONObject updateObj = (JSONObject) updates.get(updates.length()-1);
					long lastUpdateOfCourse = Long.parseLong(updateObj.getString("timecreated"));
					if (lastUpdate < lastUpdateOfCourse) {
						lastUpdate = lastUpdateOfCourse;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (lastUpdate == 0) {
			return now;
		} else {
			return lastUpdate + 1;
		}
	}

}
