package ru.rooyzee.elytrixitem.enchant;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.rooyzee.elytrixitem.Main;
import ru.rooyzee.elytrixitem.enchant.impl.SunShacklesEnchant;

import java.util.Map;
import java.util.UUID;

public final class EnchantBookListener implements Listener {

    private final Main plugin;

    public EnchantBookListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAnvil(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        ItemStack target = inventory.getItem(0);
        ItemStack book = inventory.getItem(1);

        if (target == null || book == null) {
            return;
        }

        if (book.getType() != Material.ENCHANTED_BOOK || !book.hasItemMeta()) {
            return;
        }

        CustomEnchant matched = findApplicable(book, target);
        if (matched == null) {
            return;
        }

        ItemStack result = buildResult(target, matched);
        event.setResult(result);

        inventory.setRepairCost(1);
        inventory.setMaximumRepairCost(40);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();

        if (!(inventory instanceof AnvilInventory)) {
            return;
        }

        if (event.getRawSlot() != 2) {
            return;
        }

        AnvilInventory anvil = (AnvilInventory) inventory;
        ItemStack target = anvil.getItem(0);
        ItemStack book = anvil.getItem(1);

        if (target == null || book == null) {
            return;
        }

        if (book.getType() != Material.ENCHANTED_BOOK) {
            return;
        }

        CustomEnchant matched = findApplicable(book, target);
        if (matched == null) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true);

        ItemStack result = buildResult(target, matched);
        InventoryAction action = event.getAction();

        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            if (player.getInventory().addItem(result).isEmpty()) {
                clearAnvil(anvil, player);
            }
            return;
        }

        ItemStack cursor = event.getCursor();

        if (cursor == null || cursor.getType() == Material.AIR) {
            event.setCursor(result);
            clearAnvil(anvil, player);
            return;
        }

        if (cursor.isSimilar(result) && cursor.getAmount() < cursor.getMaxStackSize()) {
            cursor.setAmount(cursor.getAmount() + 1);
            event.setCursor(cursor);
            clearAnvil(anvil, player);
        }
    }

    private ItemStack buildResult(ItemStack target, CustomEnchant matched) {
        ItemStack base = target.clone();
        base.setAmount(1);

        if (matched instanceof SunShacklesEnchant) {
            base = convertToSunHelmet(base);
        }

        matched.apply(base);
        return base;
    }

    private ItemStack convertToSunHelmet(ItemStack source) {
        ItemStack golden = new ItemStack(Material.GOLDEN_HELMET, 1);

        ItemMeta sourceMeta = source.getItemMeta();
        ItemMeta goldenMeta = golden.getItemMeta();

        if (sourceMeta != null && goldenMeta != null) {
            goldenMeta.setDisplayName(ru.rooyzee.elytrixitem.util.ColorUtil.color("&6Золотой шлем"));

            if (sourceMeta.hasEnchants()) {
                for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : sourceMeta.getEnchants().entrySet()) {
                    goldenMeta.addEnchant(entry.getKey(), entry.getValue(), true);
                }
            }

            if (sourceMeta.hasLore()) {
                goldenMeta.setLore(sourceMeta.getLore());
            }

            goldenMeta.setUnbreakable(true);

            AttributeModifier armor = new AttributeModifier(
                    UUID.randomUUID(),
                    "elytrix.sun.armor",
                    3.0D,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlot.HEAD
            );

            AttributeModifier toughness = new AttributeModifier(
                    UUID.randomUUID(),
                    "elytrix.sun.toughness",
                    3.0D,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlot.HEAD
            );

            AttributeModifier knockback = new AttributeModifier(
                    UUID.randomUUID(),
                    "elytrix.sun.knockback",
                    0.1D,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlot.HEAD
            );

            goldenMeta.addAttributeModifier(Attribute.GENERIC_ARMOR, armor);
            goldenMeta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, toughness);
            goldenMeta.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, knockback);

            golden.setItemMeta(goldenMeta);
        }

        return golden;
    }

    private void clearAnvil(AnvilInventory anvil, Player player) {
        anvil.setItem(0, null);

        ItemStack book = anvil.getItem(1);
        if (book != null && book.getAmount() > 1) {
            book.setAmount(book.getAmount() - 1);
            anvil.setItem(1, book);
        } else {
            anvil.setItem(1, null);
        }

        anvil.setItem(2, null);
        player.updateInventory();
    }

    private CustomEnchant findApplicable(ItemStack book, ItemStack target) {
        for (CustomEnchant enchant : plugin.getEnchantRegistry().getAll()) {
            if (!enchant.has(book)) {
                continue;
            }

            if (!enchant.canEnchant(target)) {
                continue;
            }

            if (enchant.has(target)) {
                continue;
            }

            return enchant;
        }

        return null;
    }
}