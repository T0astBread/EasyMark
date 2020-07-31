package easymark.database.models;

public class Admin extends Entity {
    private AccessToken accessToken;
    private String iek;

    public AccessToken getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(AccessToken accessToken) {
        this.accessToken = accessToken;
    }

    public String getIek() {
        return iek;
    }

    public void setIek(String iek) {
        this.iek = iek;
    }
}
