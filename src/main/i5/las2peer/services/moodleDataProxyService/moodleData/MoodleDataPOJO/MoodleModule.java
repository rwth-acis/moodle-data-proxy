package i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO;

import org.json.JSONObject;
import java.util.Base64;

public class MoodleModule extends MoodleDataPOJO {

	private String modname;
	private int course;
	private int instance;

	public MoodleModule(JSONObject moduleData) {
		if (moduleData.isNull("id")) {
			logger.info("Cannot create MoodleModule: Missing id!");
			this.id = 0;
		}
		else {
			this.id = moduleData.getInt("id");
		}
		if (moduleData.isNull("modname")) {
			logger.severe("Cannot create MoodleModule " + this.id + ": Missing modname!");
			this.modname = "";
		}
		else {
			this.modname = moduleData.getString("modname");
		}
		if (moduleData.isNull("course")) {
			logger.warning("Missing expected field course for module " + this.id);
			this.course = 0;
		}
		else {
			this.course = moduleData.getInt("course");
		}
		if (moduleData.isNull("added")) {
			logger.warning("Missing expected field added for module " + this.id);
			this.created = 0;
		}
		else {
			this.created = moduleData.getLong("added");
		}
		if (moduleData.isNull("instance")) {
			logger.warning("Missing expected field instance for module " + this.id);
			this.instance = 0;
		}
		else {
			this.instance = moduleData.getInt("instance");
		}
  }

	public String getModname() {
		return this.modname;
	}

	public void setModname(String modname) {
		this.modname = modname;
	}

	public int getCourse() {
		return course;
	}

	public void setCourse(int course) {
		this.course = course;
	}

	public int getInstance() {
		return instance;
	}

	public void setInstance(int instance) {
		this.instance = instance;
	}
}
