package easymark.database.models;

import java.util.*;

public class Database {
    private List<Admin> admins;
    private List<Course> courses;
    private List<Participant> participants;
    private List<Chapter> chapters;
    private List<Assignment> assignments;
    private List<AssignmentResult> assignmentResults;
    private List<TestRequest> testRequests;
    private List<ActivityLogItem> activityLogItems;

    public Database() {
        this.admins = new ArrayList<>();
        this.courses = new ArrayList<>();
        this.participants = new ArrayList<>();
        this.chapters = new ArrayList<>();
        this.assignments = new ArrayList<>();
        this.assignmentResults = new ArrayList<>();
        this.testRequests = new ArrayList<>();
        this.activityLogItems = new ArrayList<>();
    }

    public List<Admin> getAdmins() {
        return admins;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public List<Participant> getParticipants() {
        return participants;
    }

    public List<Chapter> getChapters() {
        return chapters;
    }

    public List<Assignment> getAssignments() {
        return assignments;
    }

    public List<AssignmentResult> getAssignmentResults() {
        return assignmentResults;
    }

    public List<TestRequest> getTestRequests() {
        return testRequests;
    }

    public List<ActivityLogItem> getActivityLogItems() {
        return activityLogItems;
    }
}
