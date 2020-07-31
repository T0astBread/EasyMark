package easymark.cryptography;

import easymark.*;
import easymark.database.models.*;
import org.springframework.security.crypto.encrypt.*;
import org.springframework.security.crypto.keygen.*;

public class Cryptography {
    public static final int ACCESS_TOKEN_LENGTH = 48;
    public static final int ACCESS_TOKEN_IDENTIFIER_LENGTH = 8;
    private static final StringKeyGenerator KEY_GENERATOR = KeyGenerators.string();

    public static String generateEncryptionSalt() {
        return KEY_GENERATOR.generateKey();
    }

    public static String generateUEK() {
        return KEY_GENERATOR.generateKey() + KEY_GENERATOR.generateKey() + KEY_GENERATOR.generateKey();
    }

    public static String generateSET() {
        return KEY_GENERATOR.generateKey() + KEY_GENERATOR.generateKey() + KEY_GENERATOR.generateKey();
    }

    public static String generateAccessToken() {
        return KEY_GENERATOR.generateKey() + KEY_GENERATOR.generateKey() + KEY_GENERATOR.generateKey();
    }

    public static AccessToken accessTokenFromString(String strToken) {
        AccessToken accessToken = new AccessToken();
        accessToken.setIdentifier(strToken.substring(0, ACCESS_TOKEN_IDENTIFIER_LENGTH));
        accessToken.setSecret(Utils.PASSWORD_ENCODER.encode(strToken.substring(ACCESS_TOKEN_IDENTIFIER_LENGTH)));
        return accessToken;
    }

    public static String encryptUEK(String password, String salt, String uekStr) {
        return Encryptors.delux(password, salt).encrypt(uekStr);
    }

    public static String decryptUEK(String password, String ekStr) {
        String ekKey = getEKKey(ekStr);
        String ekSalt = getEKSalt(ekStr);
        return Encryptors.delux(password, ekSalt).decrypt(ekKey);
    }

    public static String encryptData(String text, String uekStr) {
        String uekKey = getEKKey(uekStr);
        String uekSalt = getEKSalt(uekStr);
        return Encryptors.delux(uekKey, uekSalt).encrypt(text);
    }

    public static String decryptData(String cipherText, String uekStr) {
        String uekKey = getEKKey(uekStr);
        String uekSalt = getEKSalt(uekStr);
        return Encryptors.delux(uekKey, uekSalt).decrypt(cipherText);
    }

    private static String generateAlphanumString(int length) {
        return Utils.RANDOM.ints(48, 123)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private static String getEKKey(String ekStr) {
        return ekStr.substring(16);
    }

    private static String getEKSalt(String ekStr) {
        return ekStr.substring(0, 16);
    }
}
