package easymark.webserver;

import easymark.*;

import java.time.*;

public class CSRFToken {
    private final String value;
    private final LocalDateTime timeout;

    private CSRFToken(String value, LocalDateTime timeout) {
        this.value = value;
        this.timeout = timeout;
    }

    public static CSRFToken random() {
        return new CSRFToken(
                Long.toString(Utils.RANDOM.nextLong()),
                LocalDateTime.now().plusHours(5));
    }

    public String getValue() {
        return value;
    }

    public boolean isValid() {
        return !LocalDateTime.now().isAfter(this.timeout);
    }
}
