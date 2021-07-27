package i5.las2peer.services.moodleDataProxyService.util;

public class StoreManagementParseException extends Exception {

    public StoreManagementParseException(String message) {
        super(message);
    }

    public StoreManagementParseException(String courseId, String storeName) {
        super("The store " + storeName + " assigned to course " + courseId +
                " does not belong to the list of available stores.");
    }

}
