package i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO;

import org.json.JSONObject;

public class MoodleGrade extends MoodleDataPOJO {
	private float graderaw;
	private int cmid;
	private int grademin;
	private int grademax;
	private String feedback;
	private long timestart = 0;
	private long timefinish = 0;

	public MoodleGrade(JSONObject gradeData) {
		if (gradeData.isNull("id")) {
			logger.severe("Cannot create MoodleGrade: Missing id!");
			this.id = 0;
		}
		else {
			this.id = gradeData.getInt("id");
		}
		if (gradeData.isNull("cmid")) {
			logger.warning("Missing expected field cmid for grade " + this.id);
			this.cmid = 0;
		}
		else {
			this.cmid = gradeData.getInt("cmid");
		}
		if (gradeData.isNull("graderaw")) {
			logger.warning("Missing expected field graderaw for grade " + this.id);
			this.graderaw = 0;
		}
		else {
			this.graderaw = gradeData.getFloat("graderaw");
		}
		if (gradeData.isNull("grademin")) {
			logger.warning("Missing expected field grademin for grade " + this.id);
			this.grademin = 0;
		}
		else {
			this.grademin = gradeData.getInt("grademin");
		}
		if (gradeData.isNull("grademax")) {
			logger.warning("Missing expected field grademax for grade " + this.id);
			this.grademax = 0;
		}
		else {
			this.grademax = gradeData.getInt("grademax");
		}
		if (gradeData.isNull("gradedategraded")) {
			logger.warning("Missing expected field gradedategraded for grade " + this.id);
			this.created = 0;
		}
		else {
			this.created = gradeData.getLong("gradedategraded");
		}
		if (gradeData.isNull("feedback")) {
			logger.warning("Missing expected field feedback for grade " + this.id);
			this.feedback = "";
		}
		else {
			this.feedback = gradeData.getString("feedback");
		}
  }

	public int getCmid() {
		return cmid;
	}

	public void setCmid(int cmid) {
		this.cmid = cmid;
	}

	public int getGrademin() {
		return grademin;
	}

	public void setGrademin(int grademin) {
		this.grademin = grademin;
	}

	public int getGrademax() {
		return grademax;
	}

	public void setGrademax(int grademax) {
		this.grademax = grademax;
	}

	public float getGraderaw() {
		return graderaw;
	}

	public void setGraderaw(float graderaw) {
		this.graderaw = graderaw;
	}

	public String getFeedback() {
		return feedback;
	}

	public void setFedback(String feedback) {
		this.feedback = feedback;
	}

	public long getTimestart() {
		return timestart;
	}

	public void setTimestart(long timestart) {
		if (timestart != null)
			this.timestart = timestart;
	}

	public long getTimefinish() {
		return timefinish;
	}

	public void setTimefinish(long timefinish) {
		if (timefinish != null)
			this.timefinish = timefinish;
	}
}
