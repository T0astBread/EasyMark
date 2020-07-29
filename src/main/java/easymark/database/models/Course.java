package easymark.database.models;

import java.util.*;

public class Course extends Entity {
    private String name;
    private UUID adminId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getAdminId() {
        return adminId;
    }

    public void setAdminId(UUID adminId) {
        this.adminId = adminId;
    }
}
