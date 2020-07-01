package i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO;

import i5.las2peer.logging.L2pLogger;

public abstract class MoodleDataPOJO {
  protected int id;
  protected long created;
  protected final static L2pLogger logger = L2pLogger.getInstance(MoodleDataPOJO.class.getName());

  public int getId() {
    return this.id;
  }

  public long getCreated() {
    return this.created;
  }
}
