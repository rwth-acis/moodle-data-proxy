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
    MoodleWebServiceConnection moodle = new MoodleWebServiceConnection("424f0b29c3d5944506ea9ca8b9dec502",
      "https://moodle.tech4comp.dbis.rwth-aachen.de");
    MoodleStatementGenerator statements = new MoodleStatementGenerator(moodle);
    System.out.println(statements.courseUpdatesSince(10, 1592325873));
  }
}
