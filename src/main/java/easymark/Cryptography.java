package easymark;

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

    public static String encryptUEK(String uek, String salt, String password) {
        return Encryptors.delux(password, salt).encrypt(uek);
    }

    public static String decryptUEK(String ek, String ekSalt, String password) {
        return Encryptors.delux(password, ekSalt).decrypt(ek);
    }

    public static String encryptData(String text, String salt, String uek) {
        return Encryptors.delux(uek, salt).encrypt(text);
    }

    public static String decryptData(String cipherText, String salt, String uek) {
        return Encryptors.delux(uek, salt).decrypt(cipherText);
    }
}
