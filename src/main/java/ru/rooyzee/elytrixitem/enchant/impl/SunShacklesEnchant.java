package ru.rooyzee.elytrixitem.enchant.impl;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import ru.rooyzee.elytrixitem.Main;
import ru.rooyzee.elytrixitem.enchant.CustomEnchant;

import java.util.EnumSet;
import java.util.Set;

public final class SunShacklesEnchant extends CustomEnchant {

    public static final String ID = "sun_shackles";

    private static final Set<Material> HELMETS = EnumSet.of(
            Material.DIAMOND_HELMET,
            Material.NETHERITE_HELMET
    );

    public SunShacklesEnchant(Main plugin) {
        super(plugin, ID);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "&6Солнечные оковы I";
    }

    @Override
    public boolean canEnchant(ItemStack item) {
        return item != null && HELMETS.contains(item.getType());
    }
}