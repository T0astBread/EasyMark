package easymark.webserver.sessions;

import easymark.database.models.*;
import easymark.webserver.*;

import java.awt.*;
import java.time.*;
import java.util.*;
import java.util.List;

public class Session {
    private final UUID id;
    private final UUID userId;
    private final Set<UserRole> roles;
    private final Color color;
    private final LocalDateTime creationTime;
    private final String creationIPAddress;
    private LocalDateTime lastSessionAction;
    private final String sek;
    private final String sekSalt;
    private String nameDisplay;
    private String atDisplay;
    private UUID adminIdDisplay;

    public static Session forAdmin(Admin user, String creationIPAddress, String sek, String sekSalt) {
        return new Session(user.getId(), creationIPAddress, Set.of(UserRole.ADMIN), sek, sekSalt);
    }

    public static Session forParticipant(Participant user, String creationIPAddress) {
        return new Session(user.getId(), creationIPAddress, Set.of(UserRole.PARTICIPANT), null, null);
    }

    private Session(UUID userId, String creationIPAddress, Set<UserRole> roles, String sek, String sekSalt) {
        this.id = UUID.randomUUID();
        this.color = new Color(randomColorVal(), randomColorVal(), randomColorVal());
        this.userId = userId;
        this.creationIPAddress = creationIPAddress;
        this.roles = roles;
        this.creationTime = LocalDateTime.now();
        this.lastSessionAction = LocalDateTime.now();
        this.sek = sek;
        this.sekSalt = sekSalt;
    }

    public UUID getId() {
        return id;
    }

    public Color getColor() {
        return color;
    }

    public UUID getUserId() {
        return userId;
    }

    public Set<UserRole> getRoles() {
        return roles;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public String getCreationIPAddress() {
        return creationIPAddress;
    }

    public String getSek() {
        return sek;
    }

    public String getSekSalt() {
        return sekSalt;
    }

    public LocalDateTime getLastSessionAction() {
        return lastSessionAction;
    }

    public void setLastSessionAction(LocalDateTime lastSessionAction) {
        this.lastSessionAction = lastSessionAction;
    }

    public String getNameDisplay() {
        return nameDisplay;
    }

    public void setNameDisplay(String nameDisplay) {
        this.nameDisplay = nameDisplay;
    }

    public String getAtDisplay() {
        return atDisplay;
    }

    public void setAtDisplay(String atDisplay) {
        this.atDisplay = atDisplay;
    }

    public UUID getAdminIdDisplay() {
        return adminIdDisplay;
    }

    public void setAdminIdDisplay(UUID adminIdDisplay) {
        this.adminIdDisplay = adminIdDisplay;
    }

    private static int randomColorVal() {
        return (int) (Math.random() * 255);
    }
}
