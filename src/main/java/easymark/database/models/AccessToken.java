package easymark.database.models;

import easymark.*;

public class AccessToken {
    private String identifier;
    private String secret;

    public static String generateString() {
        // Generates alphanumeric random strings
        return Utils.RANDOM.ints(48, 123)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(24)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public static AccessToken fromString(String strToken) {
        AccessToken accessToken = new AccessToken();
        accessToken.setIdentifier(strToken.substring(0, 6));
        accessToken.setSecret(Utils.PASSWORD_ENCODER.encode(strToken.substring(6)));
        return accessToken;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
