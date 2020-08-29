package easymark.pebble;

import com.mitchellbosecke.pebble.error.*;
import com.mitchellbosecke.pebble.extension.*;
import com.mitchellbosecke.pebble.template.*;

import java.util.*;

public class ShortUUIDFilter implements Filter {
    @Override
    public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) throws PebbleException {
        return input.toString().substring(0, 8);
    }

    @Override
    public List<String> getArgumentNames() {
        return null;
    }

    public static String apply(UUID uuid) {
        return uuid.toString().substring(0, 8);
    }
}
