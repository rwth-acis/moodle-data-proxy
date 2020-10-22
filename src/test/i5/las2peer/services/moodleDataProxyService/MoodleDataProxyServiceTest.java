package i5.las2peer.services.moodleDataProxyService;

import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleWebServiceConnection;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleStatementGenerator;
import i5.las2peer.logging.L2pLogger;

import java.io.IOException;
import org.junit.Test;
import i5.las2peer.testing.TestSuite;

public class MoodleDataProxyServiceTest {

  private final MOODLE_TOKEN = "";
  private final MOODLE_DOMAIN = "";
  private final COURSE_ID = 0;
  private final TIMESTAMP = 728632800;

  @Test
  public void testStatementGenerator() throws IOException {
    MoodleWebServiceConnection moodle = new MoodleWebServiceConnection(MOODLE_TOKEN,MOODLE_DOMAIN);
    MoodleStatementGenerator statements = new MoodleStatementGenerator(moodle);
    System.out.println(statements.courseUpdatesSince(COURSE_ID,TIMESTAMP));
  }
}
