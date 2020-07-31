package easymark.database.models;

import java.util.*;

public class Participant extends Entity {
    private UUID courseId;
    private String name;
    private String nameSalt;
    private AccessToken cat;

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
}
