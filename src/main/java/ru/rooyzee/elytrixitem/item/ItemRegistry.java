package ru.rooyzee.elytrixitem.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ItemRegistry {

    private final Map<String, CustomItem> items = new LinkedHashMap<>();

    public void register(CustomItem item) {
        items.put(item.getId().toLowerCase(Locale.ROOT), item);
    }

    public CustomItem get(String id) {
        if (id == null) {
            return null;
        }
        return items.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<CustomItem> getAll() {
        return Collections.unmodifiableCollection(items.values());
    }

    public List<String> getIds() {
        return new ArrayList<>(items.keySet());
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}