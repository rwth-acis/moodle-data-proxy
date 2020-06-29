package i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO;

public class MoodleUserGradeItem extends MoodleDataPOJO {

	private Integer id;
	private Integer courseid;
	private String itemname;
	private String itemtype;
	private String itemmodule;
	private Integer iteminstance;
	private Integer itemnumber;
	private Integer categoryid;
	private Integer outcomeid;
	private Integer scaleid;
	private Boolean locked;
	private Integer cmid;
	private Double weightraw;
	private String weightformatted;
	private String status;
	private Double graderaw;
	private Long gradedatesubmitted;
	private Long gradedategraded;
	private Boolean gradehiddenbydate;
	private Boolean gradeneedsupdate;
	private Boolean gradeishidden;
	private Boolean gradeislocked;
	private Boolean gradeisoverridden;
	private String gradeformatted;
	private Double grademin;
	private Double grademax;
	private String rangeformatted;
	private String percentageformatted;
	private String feedback;
	private Integer feedbackformat;
	private Long duration;

	public String getItemname() {
		return itemname;
	}

	public void setItemname(String itemname) {
		this.itemname = itemname;
	}

	public String getItemtype() {
		return itemtype;
	}

	public void setItemtype(String itemtype) {
		this.itemtype = itemtype;
	}

	public String getItemmodule() {
		return itemmodule;
	}

	public void setItemmodule(String itemmodule) {
		this.itemmodule = itemmodule;
	}

	public Integer getIteminstance() {
		return iteminstance;
	}

	public void setIteminstance(Integer iteminstance) {
		this.iteminstance = iteminstance;
	}

	public Integer getItemnumber() {
		return itemnumber;
	}

	public void setItemnumber(Integer itemnumber) {
		this.itemnumber = itemnumber;
	}

	public Integer getCategoryid() {
		return categoryid;
	}

	public void setCategoryid(Integer categoryid) {
		this.categoryid = categoryid;
	}

	public Integer getOutcomeid() {
		return outcomeid;
	}

	public void setOutcomeid(Integer outcomeid) {
		this.outcomeid = outcomeid;
	}

	public Integer getScaleid() {
		return scaleid;
	}

	public void setScaleid(Integer scaleid) {
		this.scaleid = scaleid;
	}

	public Boolean getLocked() {
		return locked;
	}

	public void setLocked(Boolean locked) {
		this.locked = locked;
	}

	public Integer getCmid() {
		return cmid;
	}

	public void setCmid(Integer cmid) {
		this.cmid = cmid;
	}

	public Double getWeightraw() {
		return weightraw;
	}

	public void setWeightraw(Double weightraw) {
		this.weightraw = weightraw;
	}

	public String getWeightformatted() {
		return weightformatted;
	}

	public void setWeightformatted(String weightformatted) {
		this.weightformatted = weightformatted;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Double getGraderaw() {
		return graderaw;
	}

	public void setGraderaw(Double graderaw) {
		this.graderaw = graderaw;
	}

	public Long getGradedatesubmitted() {
		return gradedatesubmitted;
	}

	public void setGradedatesubmitted(Long gradedatesubmitted) {
		this.gradedatesubmitted = gradedatesubmitted;
	}

	public Long getGradedategraded() {
		return gradedategraded;
	}

	public void setGradedategraded(Long gradedategraded) {
		this.gradedategraded = gradedategraded;
	}

	public Boolean getGradehiddenbydate() {
		return gradehiddenbydate;
	}

	public void setGradehiddenbydate(Boolean gradehiddenbydate) {
		this.gradehiddenbydate = gradehiddenbydate;
	}

	public Boolean getGradeneedsupdate() {
		return gradeneedsupdate;
	}

	public void setGradeneedsupdate(Boolean gradeneedsupdate) {
		this.gradeneedsupdate = gradeneedsupdate;
	}

	public Boolean getGradeishidden() {
		return gradeishidden;
	}

	public void setGradeishidden(Boolean gradeishidden) {
		this.gradeishidden = gradeishidden;
	}

	public Boolean getGradeislocked() {
		return gradeislocked;
	}

	public void setGradeislocked(Boolean gradeislocked) {
		this.gradeislocked = gradeislocked;
	}

	public Boolean getGradeisoverridden() {
		return gradeisoverridden;
	}

	public void setGradeisoverridden(Boolean gradeisoverridden) {
		this.gradeisoverridden = gradeisoverridden;
	}

	public String getGradeformatted() {
		return gradeformatted;
	}

	public void setGradeformatted(String gradeformatted) {
		this.gradeformatted = gradeformatted;
	}

	public Double getGrademin() {
		return grademin;
	}

	public void setGrademin(Double grademin) {
		this.grademin = grademin;
	}

	public Double getGrademax() {
		return grademax;
	}

	public void setGrademax(Double grademax) {
		this.grademax = grademax;
	}

	public String getRangeformatted() {
		return rangeformatted;
	}

	public void setRangeformatted(String rangeformatted) {
		this.rangeformatted = rangeformatted;
	}

	public String getPercentageformatted() {
		return percentageformatted;
	}

	public void setPercentageformatted(String percentageformatted) {
		this.percentageformatted = percentageformatted;
	}

	public String getFeedback() {
		return feedback;
	}

	public void setFeedback(String feedback) {
		this.feedback = feedback;
	}

	public Integer getFeedbackformat() {
		return feedbackformat;
	}

	public void setFeedbackformat(Integer feedbackformat) {
		this.feedbackformat = feedbackformat;
	}

	public Integer getCourseid() {
		return courseid;
	}

	public void setCourseid(Integer courseid) {
		this.courseid = courseid;
	}

	public Long getDuration() {
		return duration;
	}

	public void setDuration(Long duration) {
		this.duration = duration;
	}

}
