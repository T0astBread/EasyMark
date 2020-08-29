package easymark.database.models;

import java.time.*;
import java.util.*;

public class ActivityLogItem extends Entity {
    private UUID adminId;
    private LocalDateTime timestamp;
    private UUID originatingSessionId;
    private int originatingSessionColorR, originatingSessionColorG, originatingSessionColorB;
    private String text;

    public UUID getAdminId() {
        return adminId;
    }

    public void setAdminId(UUID adminId) {
        this.adminId = adminId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public UUID getOriginatingSessionId() {
        return originatingSessionId;
    }

    public void setOriginatingSessionId(UUID originatingSessionId) {
        this.originatingSessionId = originatingSessionId;
    }

    public int getOriginatingSessionColorR() {
        return originatingSessionColorR;
    }

    public void setOriginatingSessionColorR(int originatingSessionColorR) {
        this.originatingSessionColorR = originatingSessionColorR;
    }

    public int getOriginatingSessionColorG() {
        return originatingSessionColorG;
    }

    public void setOriginatingSessionColorG(int originatingSessionColorG) {
        this.originatingSessionColorG = originatingSessionColorG;
    }

    public int getOriginatingSessionColorB() {
        return originatingSessionColorB;
    }

    public void setOriginatingSessionColorB(int originatingSessionColorB) {
        this.originatingSessionColorB = originatingSessionColorB;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
