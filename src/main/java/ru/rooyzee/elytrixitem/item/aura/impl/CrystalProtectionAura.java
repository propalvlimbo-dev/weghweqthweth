package ru.rooyzee.elytrixitem.item.aura.impl;

import org.bukkit.Material;
import ru.rooyzee.elytrixitem.Main;
import ru.rooyzee.elytrixitem.item.aura.AuraItem;

import java.util.Arrays;
import java.util.List;

public final class CrystalProtectionAura extends AuraItem {

    public static final String ID = "crystal_protection_aura";

    public CrystalProtectionAura(Main plugin) {
        super(plugin);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Material getMaterial() {
        return Material.BLACK_DYE;
    }

    @Override
    public String getDisplayName() {
        return "&7« &#5B2C6FАура защиты от кристалла &7»";
    }

    @Override
    public String getActiveEffect() {
        return "&#F8BEFB-10% урона от кристалла";
    }

    @Override
    public List<String> getLoreTemplate() {
        return Arrays.asList(
                "&#F8BEFB&l┃ ",
                "&#F8BEFB&l┃ &fТип: &#5B2C6FАура",
                "&#F8BEFB&l┃ &fЭффект: " + EFFECT_PLACEHOLDER,
                "&#F8BEFB&l┃ ",
                "&7● &fДержите в инвентаре для активации",
                "&7● &fТолько одна аура может быть активной"
        );
    }
}