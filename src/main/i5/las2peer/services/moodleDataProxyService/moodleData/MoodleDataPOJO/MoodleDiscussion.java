package i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO;

import org.json.JSONObject;

public class MoodleDiscussion extends MoodleDataPOJO {
	private int userid;
	private int discussion;
	private long timemodified;
	private String subject;
	private String message;

  public MoodleDiscussion(JSONObject discussionData) {
		if (discussionData.isNull("id")) {
			logger.severe("Cannot create MoodleDiscussion: Missing id!");
			this.id = 0;
		}
		else {
			this.id = discussionData.getInt("id");
		}
		if (discussionData.isNull("discussion")) {
			logger.severe("Cannot create MoodleDiscussion " + this.id + ": Missing discussion!");
			this.discussion = 0;
		}
		else {
			this.discussion = discussionData.getInt("discussion");
		}
		if (discussionData.isNull("userid")) {
			logger.warning("Missing expected field userid for discussion " + this.id);
			this.userid = 0;
		}
		else {
			this.userid = discussionData.getInt("userid");
		}
		if (discussionData.isNull("created")) {
			logger.warning("Missing expected field created for discussion " + this.id);
			this.created = 0;
		}
		else {
			this.created = discussionData.getLong("created");
		}
		if (discussionData.isNull("timemodified")) {
			logger.warning("Missing expected field timemodified for discussion " + this.id);
			this.timemodified = 0;
		}
		else {
			this.timemodified = discussionData.getLong("timemodified");
		}
		if (discussionData.isNull("subject")) {
			logger.warning("Missing expected field subject for discussion " + this.id);
			this.subject = "";
		}
		else {
			this.subject = discussionData.getString("subject");
		}
		if (discussionData.isNull("message")) {
			logger.warning("Missing expected field message for discussion " + this.id);
			this.message = "";
		}
		else {
			this.message = discussionData.getString("message");
		}
  }

	public int getUserid() {
		return userid;
	}

	public void setUserid(int userid) {
		this.userid = userid;
	}

	public int getDiscussion() {
		return discussion;
	}

	public void setDiscussion(int discussion) {
		this.discussion = discussion;
	}

	public long getCreated() {
		return created;
	}

	public void setCreated(long created) {
		this.created = created;
	}

	public long getTimemodified() {
		return timemodified;
	}

	public void setTimemodified(long timemodified) {
		this.timemodified = timemodified;
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
