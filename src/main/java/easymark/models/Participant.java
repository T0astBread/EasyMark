package easymark.models;

import java.util.*;

public class Participant extends Entity {
    private UUID courseId;
    private String nameEnc;
    private String cat;

    public UUID getCourseId() {
        return courseId;
    }

    public void setCourseId(UUID courseId) {
        this.courseId = courseId;
    }

    public String getNameEnc() {
        return nameEnc;
    }

    public void setNameEnc(String nameEnc) {
        this.nameEnc = nameEnc;
    }

    public String getCat() {
        return cat;
    }

    public void setCat(String cat) {
        this.cat = cat;
    }
}
