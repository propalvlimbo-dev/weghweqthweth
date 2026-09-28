package ru.rooyzee.elytrixitem.item.aura.impl;

import org.bukkit.Material;
import ru.rooyzee.elytrixitem.Main;
import ru.rooyzee.elytrixitem.item.aura.AuraItem;

import java.util.Arrays;
import java.util.List;

public final class MinerAura extends AuraItem {

    public static final String ID = "miner_aura";

    public MinerAura(Main plugin) {
        super(plugin);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Material getMaterial() {
        return Material.YELLOW_DYE;
    }

    @Override
    public String getDisplayName() {
        return "&7« &#FFD98CАура шахтёра &7»";
    }

    @Override
    public String getActiveEffect() {
        return "&#F8BEFB20% на 100-1000 монет с руды";
    }

    @Override
    public List<String> getLoreTemplate() {
        return Arrays.asList(
                "&#F8BEFB&l┃ ",
                "&#F8BEFB&l┃ &fТип: &#FFD98CАура",
                "&#F8BEFB&l┃ &fЭффект: " + EFFECT_PLACEHOLDER,
                "&#F8BEFB&l┃ ",
                "&7● &fДержите в инвентаре для активации",
                "&7● &fТолько одна аура может быть активной"
        );
    }
}