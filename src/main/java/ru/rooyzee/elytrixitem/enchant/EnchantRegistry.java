package ru.rooyzee.elytrixitem.enchant;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EnchantRegistry {

    private final Map<String, CustomEnchant> enchants = new LinkedHashMap<>();

    public void register(CustomEnchant enchant) {
        enchants.put(enchant.getId(), enchant);
    }

    public CustomEnchant get(String id) {
        return enchants.get(id);
    }

    public Collection<CustomEnchant> getAll() {
        return Collections.unmodifiableCollection(enchants.values());
    }
}