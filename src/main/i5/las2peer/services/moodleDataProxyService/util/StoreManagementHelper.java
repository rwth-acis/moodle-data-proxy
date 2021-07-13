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
    private static final String TMP_DIR = "store_assignment";

    /**
     * Name of the file where the store properties will be stored. This file consists of key-value pairs, where the
     * key is a store name and the value its client ID.
     */
    private static final String STORES_FILENAME = "stores.properties";

    /**
     * Name of the file where the course assignments will be stored. This file consists of key-value pairs, where the
     * key is a course ID and the value is a comma-separated list of store names.
     */
    private static final String ASSIGNMENT_FILENAME = "store_assignment.properties";


    /**
     * Map containing the store names and their respective client tokens.
     */
    private static HashMap<String,String> storeMap = new HashMap<>();

    /**
     * Map containing the assignment of courses to stores.
     */
    private static HashMap<String,ArrayList<String>> assignmentMap = new HashMap<>();

    /**
     * Flag that is set when the assignment of courses to stores should be performed.
     */
    private static boolean storeAssignmentEnabled = false;

    /**
     * Updates the stores file and returns a Map object of the assignment.
     *
     * @param storesInputStream Input stream containing the property file for the stores
     * @throws IOException
     */
    public static void updateStores(InputStream storesInputStream)
            throws IOException {
        createTmpDir();
        FileUtils.copyInputStreamToFile(storesInputStream, new File(TMP_DIR, STORES_FILENAME));

        storeMap = loadStores();
    }

    /**
     * Updates the assignment file and returns a Map object of the assignment.
     *
     * @param assignmentsInputStream Input stream containing the property file for the course assignments
     * @throws IOException
     */
    public static void updateAssignments(InputStream assignmentsInputStream)
            throws IOException, StoreManagementParseException {
        createTmpDir();
        File temp_file = new File(TMP_DIR, "temp_" + ASSIGNMENT_FILENAME);
        FileUtils.copyInputStreamToFile(assignmentsInputStream, temp_file);

        FileInputStream tempStream = new FileInputStream(new File(TMP_DIR, "temp_" + ASSIGNMENT_FILENAME));
        Properties assignmentsProp = new Properties();
        assignmentsProp.load(tempStream);

        // Check if all stores are contained in the stores list
        for (Entry<Object,Object> entry : assignmentsProp.entrySet()) {
            String courseId = entry.getKey().toString();
            String value = entry.getValue().toString();
            System.out.println(storeMap);
            System.out.println(value);
            ArrayList<String> assignedStores = new ArrayList<>(Arrays.asList(value.split(",")));
            System.out.println(assignedStores);
            for (String store : assignedStores) {
                System.out.println(storeMap.get(store));
                if (storeMap.get(store) == null) {
                    temp_file.delete();
                    throw new StoreManagementParseException(courseId, store);
                }
            }
        }

        File assignmentFile = new File(TMP_DIR, ASSIGNMENT_FILENAME);
        if (assignmentFile.exists()) {
            assignmentFile.delete();
        }
        temp_file.renameTo(assignmentFile);
        assignmentMap = loadAssignments();
    }

    /**
     * Parses the stores property file and returns the store IDs and access tokens as key-value pairs in a HashMap.
     *
     * @return Assignment of courses to stores as a HasHMap.
     * @throws IOException
     */
    public static HashMap<String,String> loadStores() throws IOException{
        InputStream input = new FileInputStream(new File(TMP_DIR, STORES_FILENAME));
        Properties prop = new Properties();
        prop.load(input);

        HashMap<String,String> result = new HashMap<>();
        for (Entry<Object,Object> entry : prop.entrySet()) {
            String key = entry.getKey().toString();
            String value = entry.getValue().toString();

            result.put(key, value);
        }
        return result;
    }

    /**
     * Parses the assignment property file and returns the assignment as a HashMap.
     *
     * @return Assignment of courses to stores as a HasHMap.
     * @throws IOException
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
     * @throws IOException
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
     * Checks if the file for the store assignment exists.
     * @return Whether the file for the store assignment exists.
     */
    public static boolean isStoreListEnabled() {
        return (new File(TMP_DIR, STORES_FILENAME)).exists();
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

    public static String getClientId(String storeId) {
        return storeMap.get(storeId);
    }

    public static void resetAssignment() {
        assignmentMap = new HashMap<>();
    }

    public static void enableStoreAssignment() {storeAssignmentEnabled = true;}

    public static void disableStoreAssignment() {storeAssignmentEnabled = false;}

    public static int numberOfStores() {
        return storeMap.size();
    }

    public static int numberOfAssignments() {
        return assignmentMap.size();
    }

}
