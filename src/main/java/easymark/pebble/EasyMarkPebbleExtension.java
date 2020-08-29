package easymark.pebble;

import com.mitchellbosecke.pebble.extension.*;

import java.util.*;

public class EasyMarkPebbleExtension extends AbstractExtension {
    private final Map<String, Filter> FILTERS = Map.of(
            "easymarkup", new EasyMarkupFilter(),
            "shortuuid", new ShortUUIDFilter()
    );

    @Override
    public Map<String, Filter> getFilters() {
        return FILTERS;
    }
}
