package i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO;

import org.json.JSONObject;

public class MoodlePost extends MoodleDataPOJO {
	private int discussionid;
	private int userid;
	private long timecreated;
	private String subject;
	private String message;

  public MoodlePost(JSONObject postData) {
		if (postData.isNull("id")) {
			logger.severe("Cannot create MoodlePost: Missing id!");
			this.id = 0;
		}
		else {
			this.id = postData.getInt("id");
		}
		if (postData.isNull("discussionid")) {
			logger.severe("Cannot create MoodlePost " + this.id + ": Missing discussionid!");
			this.discussionid = 0;
		}
		else {
			this.discussionid = postData.getInt("discussionid");
		}
		if (postData.isNull("author") || postData.getJSONObject("author").isNull("id")) {
			logger.warning("Missing expected field userid for post " + this.id);
			this.userid = 0;
		}
		else {
			this.userid = postData.getJSONObject("author").getInt("id");
		}
		if (postData.isNull("timecreated")) {
			logger.warning("Missing expected field timecreated for post " + this.id);
			this.timecreated = 0;
		}
		else {
			this.timecreated = postData.getLong("timecreated");
		}
		if (postData.isNull("subject")) {
			logger.warning("Missing expected field subject for post " + this.id);
			this.subject = "";
		}
		else {
			this.subject = postData.getString("subject");
		}
		if (postData.isNull("message")) {
			logger.warning("Missing expected field message for post " + this.id);
			this.message = "";
		}
		else {
			this.message = postData.getString("message");
		}
  }

	public int getUserid() {
		return userid;
	}

	public void setUserid(int userid) {
		this.userid = userid;
	}

	public int getDiscussionid() {
		return userid;
	}

	public void setDiscussionid(int discussionid) {
		this.discussionid = discussionid;
	}

	public long getTimecreated() {
		return timecreated;
	}

	public void setTimecreated(long timecreated) {
		this.timecreated = timecreated;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
