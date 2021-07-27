package i5.las2peer.services.moodleDataProxyService.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class UserWhitelistHelper {

	private static final String TMP_DIR = "config";
	private static final String WHITELIST_FILENAME = "user_whitelist.csv";
	
	/**
	 * Separator for email addresses in the CSV file (if multiple items are stored in one line).
	 * If multiple lines of the CSV file are used and every line contains one email address, then
	 * the separator does not matter.
	 */
	private static final String COMMA = ",";

	/**
	 * Updates the user whitelist file stored in the {@value #TMP_DIR} directory.
	 * 
	 * @param whitelistInputStream InputStream of whitelist as CSV.
	 * @return List of email addresses from the given InputStream.
	 * @throws IOException if there is a IO problem
	 * @throws WhitelistParseException If the first line of the file is null.
	 */
	public static List<String> updateWhitelist(InputStream whitelistInputStream)
			throws IOException, WhitelistParseException {
		// create tmp dir if not existing and remove whitelist file if existing
		UserWhitelistHelper.createTmpDir();
		UserWhitelistHelper.removeWhitelistFile();

		// store given csv from input stream to file
		FileUtils.copyInputStreamToFile(whitelistInputStream, UserWhitelistHelper.getWhitelistFile());

		return UserWhitelistHelper.loadWhitelist();
	}

	/**
	 * Loads the user whitelist from file.
	 * There are two possible types of CSV files supported:
	 * 1) The first line contains the email addresses separated by {@value #COMMA}.
	 * 2) Every line of the file contains one email address.
	 * @return List of email addresses stored in the user whitelist file.
	 * @throws IOException if there is a IO problem
	 * @throws WhitelistParseException If the first line of the file is null.
	 */
	public static List<String> loadWhitelist() throws IOException, WhitelistParseException {
		// read email addresses from file
		BufferedReader br = new BufferedReader(new FileReader(UserWhitelistHelper.getWhitelistFile()));

		// read first line
		String line = br.readLine();
		if (line == null) {
			br.close();
			throw new WhitelistParseException("Error reading first line of the given CSV file.");
		}

		List<String> whitelist;
		// check if first line contains multiple items or just one
		if (line.contains(COMMA)) {
			// all email addresses should be contained in the first line
			whitelist = Arrays.asList(line.split(COMMA));
		} else {
			// first line only contains one email address
			// either there is only one item on the whitelist or
			// the other items can be found in the following lines of the file
			whitelist = new ArrayList<>();
			if(!line.isEmpty()) whitelist.add(line);
			while ((line = br.readLine()) != null) {
				if (!line.isEmpty())
					whitelist.add(line);
			}
		}

		br.close();
		return whitelist;
	}
	
	/**
	 * Checks if the file for the user whitelist exists.
	 * @return Whether the file for the user whitelist exists.
	 */
	public static boolean isWhitelistEnabled() {
		return UserWhitelistHelper.getWhitelistFile().exists();
	}
	
	/**
	 * Deletes the file containing the user whitelist in the tmp directory.
	 * @return Whether deleting the file was successful. Also returns true if the file did not exist.
	 */
	public static boolean removeWhitelistFile() {
		if(UserWhitelistHelper.getTmpDir().exists()) {
			if(UserWhitelistHelper.getWhitelistFile().exists()) {
				return UserWhitelistHelper.getWhitelistFile().delete();
			}
		}
		return true;
	}

	/**
	 * Creates the temporary folder where the user whitelist will be stored. Also
	 * cleans the directory if it already exists.
	 * 
	 * @throws IOException
	 */
	private static void createTmpDir() throws IOException {
		File tmpDir = UserWhitelistHelper.getTmpDir();
		if (!tmpDir.exists()) {
			FileUtils.forceMkdir(tmpDir.getAbsoluteFile());
		}
	}

	private static File getTmpDir() {
		return new File(TMP_DIR);
	}
	
	private static File getWhitelistFile() {
		return new File(UserWhitelistHelper.getTmpDir(), WHITELIST_FILENAME);
	}
}
