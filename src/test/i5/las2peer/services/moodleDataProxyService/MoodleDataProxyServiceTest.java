package i5.las2peer.services.moodleDataProxyService;

import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleWebServiceConnection;
import i5.las2peer.services.moodleDataProxyService.moodleData.MoodleStatementGenerator;
import i5.las2peer.logging.L2pLogger;

import java.io.IOException;
import org.junit.Test;
import i5.las2peer.testing.TestSuite;

public class MoodleDataProxyServiceTest {

  @Test
  public void testStatementGenerator() throws IOException {
    MoodleWebServiceConnection moodle = new MoodleWebServiceConnection("0f04baf3cdd5c82f55eb1a96e48eec53",
      "https://moodle.tech4comp.dbis.rwth-aachen.de");
    MoodleStatementGenerator statements = new MoodleStatementGenerator(moodle);
    System.out.println(statements.courseUpdatesSince(10, 1602086139));
  }
}
