package easymark.database.models;

import java.util.*;

public class Course extends Entity implements Ordered {
    private String name;
    private int ordNum;
    private UUID adminId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int getOrdNum() {
        return ordNum;
    }

    @Override
    public void setOrdNum(int ordNum) {
        this.ordNum = ordNum;
    }

    public UUID getAdminId() {
        return adminId;
    }

    public void setAdminId(UUID adminId) {
        this.adminId = adminId;
    }
}
