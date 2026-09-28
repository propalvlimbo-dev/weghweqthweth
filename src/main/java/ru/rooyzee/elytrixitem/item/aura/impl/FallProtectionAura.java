package ru.rooyzee.elytrixitem.item.aura.impl;

import org.bukkit.Material;
import ru.rooyzee.elytrixitem.Main;
import ru.rooyzee.elytrixitem.item.aura.AuraItem;

import java.util.Arrays;
import java.util.List;

public final class FallProtectionAura extends AuraItem {

    public static final String ID = "fall_protection_aura";

    public FallProtectionAura(Main plugin) {
        super(plugin);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Material getMaterial() {
        return Material.LIGHT_GRAY_DYE;
    }

    @Override
    public String getDisplayName() {
        return "&7« &#EAEAEAАура защиты от падения &7»";
    }

    @Override
    public String getActiveEffect() {
        return "&#F8BEFBПолная защита от падения";
    }

    @Override
    public List<String> getLoreTemplate() {
        return Arrays.asList(
                "&#F8BEFB&l┃ ",
                "&#F8BEFB&l┃ &fТип: &#EAEAEAАура",
                "&#F8BEFB&l┃ &fЭффект: " + EFFECT_PLACEHOLDER,
                "&#F8BEFB&l┃ ",
                "&7● &fДержите в инвентаре для активации",
                "&7● &fТолько одна аура может быть активной"
        );
    }
}