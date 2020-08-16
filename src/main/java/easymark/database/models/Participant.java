package easymark.database.models;

import java.util.*;

public class Participant extends Entity {
    private UUID courseId;
    private String name;
    private String nameSalt;
    private AccessToken cat;
    private String warning;
    private String group;
    private String notes;

    public UUID getCourseId() {
        return courseId;
    }

    public void setCourseId(UUID courseId) {
        this.courseId = courseId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameSalt() {
        return nameSalt;
    }

    public void setNameSalt(String nameSalt) {
        this.nameSalt = nameSalt;
    }

    public AccessToken getCat() {
        return cat;
    }

    public void setCat(AccessToken cat) {
        this.cat = cat;
    }

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
