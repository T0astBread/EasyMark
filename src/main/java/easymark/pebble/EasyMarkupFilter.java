package easymark.pebble;

import com.mitchellbosecke.pebble.error.*;
import com.mitchellbosecke.pebble.extension.*;
import com.mitchellbosecke.pebble.template.*;

import java.util.*;

public class EasyMarkupFilter implements Filter {
    @Override
    public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) throws PebbleException {
        return input.toString()
                .replaceAll("\\[b](.*?)(?<!\\\\)\\[/b]", "<strong>$1</strong>")
                .replaceAll("\\[session(\\(\\d{1,3},\\d{1,3},\\d{1,3}\\))](.*?)\\[/session]", "<span style=\"background-color: rgb$1\">$2</span>");
    }

    @Override
    public List<String> getArgumentNames() {
        return null;
    }
}
