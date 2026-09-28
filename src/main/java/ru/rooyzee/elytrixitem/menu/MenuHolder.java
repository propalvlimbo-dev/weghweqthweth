package ru.rooyzee.elytrixitem.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

public final class MenuHolder implements InventoryHolder {

    private final int page;
    private final Map<Integer, String> itemSlots = new HashMap<>();
    private Inventory inventory;

    public MenuHolder(int page) {
        this.page = page;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public void setItemSlot(int slot, String itemId) {
        itemSlots.put(slot, itemId);
    }

    public String getItemId(int slot) {
        return itemSlots.get(slot);
    }

    public int getPage() {
        return page;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}