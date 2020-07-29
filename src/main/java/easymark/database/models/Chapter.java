package easymark.database.models;

import java.time.*;
import java.util.*;

public class Chapter extends Entity {
    private UUID courseId;
    private int ordNum;
    private String name;
    private String description;
    private LocalDate dueDate;
    private boolean testRequired;

    public UUID getCourseId() {
        return courseId;
    }

    public void setCourseId(UUID courseId) {
        this.courseId = courseId;
    }

    public int getOrdNum() {
        return ordNum;
    }

    public void setOrdNum(int ordNum) {
        this.ordNum = ordNum;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public boolean isTestRequired() {
        return testRequired;
    }

    public void setTestRequired(boolean testRequired) {
        this.testRequired = testRequired;
    }
}
