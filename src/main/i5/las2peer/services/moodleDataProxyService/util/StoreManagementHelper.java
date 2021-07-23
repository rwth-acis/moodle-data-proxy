package i5.las2peer.services.moodleDataProxyService.util;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * Helper class for storing given .properties files and updating the stores and assignments of courses to stores.
 */
public class StoreManagementHelper {

    /**
     * Name of the directory where the property files will be stored.
     */
    private static final String TMP_DIR = "config";

    /**
     * Name of the file where the course assignments will be stored. This file consists of key-value pairs, where the
     * key is a course ID and the value is a comma-separated list of store names.
     */
    private static final String ASSIGNMENT_FILENAME = "store_assignment.properties";

    /**
     * Map containing the assignment of courses to stores.
     */
    private static HashMap<String,ArrayList<String>> assignmentMap = new HashMap<>();

    /**
     * Updates the assignment file and returns a Map object of the assignment.
     *
     * @param assignmentsInputStream Input stream containing the property file for the course assignments
     */
    public static void updateAssignments(InputStream assignmentsInputStream)
            throws StoreManagementParseException {

        try {
            // Check if input is a valid .properties file (if it is not, IOException is raised)
            Properties assignmentsProp = new Properties();
            assignmentsProp.load(assignmentsInputStream);


            // If valid, replace existing file and load it
            createTmpDir();
            File assignmentFile = new File(TMP_DIR, ASSIGNMENT_FILENAME);
            if (assignmentFile.exists()) {
                assignmentFile.delete();
            }
            FileOutputStream os = new FileOutputStream(assignmentFile);
            assignmentsProp.store(os, "");
            assignmentMap = loadAssignments();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses the assignment property file and returns the assignment as a HashMap.
     *
     * @return Assignment of courses to stores as a HasHMap.
     * @throws IOException if there is a IO problem
     */
    public static HashMap<String,ArrayList<String>> loadAssignments() throws IOException{
        InputStream assignmentsInput = new FileInputStream(new File(TMP_DIR, ASSIGNMENT_FILENAME));
        Properties assignmentsProp = new Properties();
        assignmentsProp.load(assignmentsInput);

        HashMap<String,ArrayList<String>> result = new HashMap<>();
        for (Entry<Object,Object> entry : assignmentsProp.entrySet()) {
            String key = entry.getKey().toString();
            String value = entry.getValue().toString();

            // Find store IDs separated by comma
            ArrayList<String> matches = new ArrayList<>(Arrays.asList(value.split(",")));
            result.put(key, matches);
        }
        return result;
    }

    /**
     * Creates a temporary folder (if it doesn't already exist).
     * This folder is used for persisting the assignment of courses to stores.
     *
     * @throws IOException if there is a IO problem
     */
    private static void createTmpDir() throws IOException {
        File tmpDir = new File(TMP_DIR);
        if (!tmpDir.exists()) {
            FileUtils.forceMkdir(tmpDir.getAbsoluteFile());
        }
    }

    /**
     * Checks if the file for the store assignment exists.
     * @return Whether the file for the store assignment exists.
     */
    public static boolean isStoreAssignmentEnabled() {
        return (new File(TMP_DIR, ASSIGNMENT_FILENAME)).exists();
    }

    /**
     * Deletes the file containing the user whitelist in the tmp directory.
     * @return Whether deleting the file was successful. Also returns true if the file did not exist.
     */
    public static boolean removeAssignmentFile() {
        File file = new File(TMP_DIR, ASSIGNMENT_FILENAME);
        if(file.exists()) {
                return file.delete();
        }
        return true;
    }

    public static ArrayList<String> getAssignment(String courseId) {
        return assignmentMap.get(courseId);
    }

    public static void resetAssignment() {
        assignmentMap = new HashMap<>();
    }

    public static int numberOfAssignments() {
        return assignmentMap.size();
    }

}
