package i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO;

import i5.las2peer.logging.L2pLogger;

public abstract class MoodleDataPOJO {
  protected int id;
  protected static L2pLogger logger = L2pLogger.getInstance("Logger");

  public int getId() {
    return this.id;
  }
}
