package i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO;

import org.json.JSONObject;

public class MoodleExercise extends MoodleDataPOJO {
	private String name;
	private String modname;
	private int grade;
	private String gradepass;

	public MoodleExercise(JSONObject exerciseData) {
		if (exerciseData.isNull("id")) {
			logger.severe("Cannot create MoodleExercise: Missing id!");
			this.id = 0;
		}
		else {
			this.id = exerciseData.getInt("id");
		}
		if (exerciseData.isNull("name")) {
			logger.warning("Missing expected field name for exercise " + this.id);
			this.name = "";
		}
		else {
			this.name = exerciseData.getString("name");
		}
		if (exerciseData.isNull("modname")) {
			logger.warning("Missing expected field modname for exercise " + this.id);
			this.modname = "";
		}
		else {
			this.modname = exerciseData.getString("modname");
		}
		if (exerciseData.isNull("grade")) {
			logger.warning("Missing expected field grade for exercise " + this.id);
			this.grade = 0;
		}
		else {
			this.grade = exerciseData.getInt("grade");
		}
		if (exerciseData.isNull("added")) {
			logger.warning("Missing expected field added for exercise " + this.id);
			this.created = 0;
		}
		else {
			this.created = exerciseData.getLong("added");
		}
		if (exerciseData.isNull("gradepass")) {
			logger.warning("Missing expected field gradepass for exercise " + this.id);
			this.gradepass = "";
		}
		else {
			this.gradepass = exerciseData.getString("gradepass");
		}
  }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getModname() {
		return modname;
	}

	public void setModname(String modname) {
		this.modname = modname;
	}

	public String getGradepass() {
		return gradepass;
	}

	public void setGradepass(String gradepass) {
		this.gradepass = gradepass;
	}

	public int getGrade() {
		return grade;

	}
	public void setGrade(int grade) {
		this.grade = grade;
	}
}
