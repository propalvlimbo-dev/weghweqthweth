package ru.rooyzee.elytrixitem.enchant.impl;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import ru.rooyzee.elytrixitem.Main;
import ru.rooyzee.elytrixitem.enchant.CustomEnchant;

import java.util.EnumSet;
import java.util.Set;

public final class DrillEnchant extends CustomEnchant {

    public static final String ID = "byr";

    private static final Set<Material> PICKAXES = EnumSet.of(
            Material.WOODEN_PICKAXE,
            Material.STONE_PICKAXE,
            Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE,
            Material.DIAMOND_PICKAXE,
            Material.NETHERITE_PICKAXE
    );

    public DrillEnchant(Main plugin) {
        super(plugin, ID);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "&aБур I";
    }

    @Override
    public boolean canEnchant(ItemStack item) {
        return item != null && PICKAXES.contains(item.getType());
    }
}