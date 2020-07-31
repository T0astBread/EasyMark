package easymark.database.models;

import java.util.*;

public class Participant extends Entity {
    private UUID courseId;
    private String nameEnc;
    private AccessToken cat;

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

    public AccessToken getCat() {
        return cat;
    }

    public void setCat(AccessToken cat) {
        this.cat = cat;
    }
}
