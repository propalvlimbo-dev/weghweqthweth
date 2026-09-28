package ru.rooyzee.elytrixitem.enchant.impl;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import ru.rooyzee.elytrixitem.Main;
import ru.rooyzee.elytrixitem.enchant.CustomEnchant;

import java.util.EnumSet;
import java.util.Set;

public final class ExplosiveArrowEnchant extends CustomEnchant {

    public static final String ID = "explosive_arrow";

    private static final Set<Material> BOWS = EnumSet.of(
            Material.BOW,
            Material.CROSSBOW
    );

    public ExplosiveArrowEnchant(Main plugin) {
        super(plugin, ID);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "&cВзрывная стрела I";
    }

    @Override
    public boolean canEnchant(ItemStack item) {
        return item != null && BOWS.contains(item.getType());
    }
}