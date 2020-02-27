package i5.las2peer.services.moodleDataProxyService.moodleData.MoodleDataPOJO;

public class MoodleUserData {

	private String email;
	private String userFullName;
	private Integer courseId;
	private String quizSummary;
	private String courseSummary;
	private Integer userId;
	private String courseName;
	private MoodleUserGradeItem moodleUserGradeItem;
	private MoodleCourse moodleCourse;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getUserFullName() {
		return userFullName;
	}

	public void setUserFullName(String userFullName) {
		this.userFullName = userFullName;
	}

	public Integer getCourseId() {
		return courseId;
	}

	public void setCourseId(Integer courseId) {
		this.courseId = courseId;
	}

	public String getQuizSummary() {
		return quizSummary;
	}

	public void setQuizSummary(String quizSummary) {
		this.quizSummary = quizSummary;
	}

	public String getCourseSummary() {
		return courseSummary;
	}

	public void setCourseSummary(String courseSummary) {
		this.courseSummary = courseSummary;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(int i) {
		this.userId = i;
	}

	public void setCourseName(String courseName) {
		this.courseName = courseName;
	}

	public String getCourseName() {
		return courseName;
	}

	public MoodleUserGradeItem getMoodleUserGradeItem() {
		return moodleUserGradeItem;
	}

	public void setMoodleUserGradeItem(MoodleUserGradeItem moodleUserGradeItem) {
		this.moodleUserGradeItem = moodleUserGradeItem;
	}

	public MoodleCourse getMoodleCourse() {
		return moodleCourse;
	}

	public void setMoodleCourse(MoodleCourse moodleCourse) {
		this.moodleCourse = moodleCourse;
	}
}
