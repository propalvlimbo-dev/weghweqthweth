package ru.rooyzee.elytrixitem.item;

import org.bukkit.inventory.ItemStack;

public interface CustomItem {

    String getId();

    ItemStack createItem();

    default boolean isStackable() {
        return true;
    }

    default ItemStack getMenuItem() {
        return createItem().clone();
    }
}