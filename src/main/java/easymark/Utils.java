package easymark;

import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.*;

import java.security.*;
import java.util.*;

public class Utils {
    public static final SecureRandom RANDOM = new SecureRandom();
    public static final PasswordEncoder PASSWORD_ENCODER;

    static {
        final String BCRYPT_PASSWORD_ENCODER = "bcrypt";
        PASSWORD_ENCODER = new DelegatingPasswordEncoder(
                BCRYPT_PASSWORD_ENCODER, Map.of(
                BCRYPT_PASSWORD_ENCODER, new BCryptPasswordEncoder()
        ));
    }
}
