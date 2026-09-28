package ru.rooyzee.elytrixitem.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;
import ru.rooyzee.elytrixitem.Main;
import ru.rooyzee.elytrixitem.item.CustomItem;
import ru.rooyzee.elytrixitem.util.ItemNameUtil;
import ru.rooyzee.elytrixitem.util.ItemStackUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class ElytriXItemsCommand implements TabExecutor {

    private static final String PERMISSION = "elytrixitems.admin";

    private final Main plugin;

    public ElytriXItemsCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            plugin.getMessages().send(sender, "no-permission");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            plugin.getMessages().sendList(sender, "help");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPlugin();
            plugin.getMessages().send(sender, "reloaded");
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            return handleGive(sender, args);
        }

        if (args[0].equalsIgnoreCase("menu")) {
            return handleMenu(sender, args);
        }

        plugin.getMessages().sendList(sender, "help");
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (args.length < 4) {
            plugin.getMessages().send(sender, "usage-give");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.getMessages().send(sender, "player-not-found");
            return true;
        }

        CustomItem customItem = plugin.getItemRegistry().get(args[2]);
        if (customItem == null) {
            plugin.getMessages().send(sender, "item-not-found");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException exception) {
            plugin.getMessages().send(sender, "invalid-amount");
            return true;
        }

        if (amount <= 0) {
            plugin.getMessages().send(sender, "invalid-amount");
            return true;
        }

        ItemStack preview = customItem.createItem();
        String itemName = ItemNameUtil.getDisplayName(preview, customItem.getId());
        boolean dropped = false;

        if (customItem.isStackable()) {
            for (ItemStack stack : ItemStackUtil.split(preview, amount)) {
                Collection<ItemStack> leftovers = target.getInventory().addItem(stack).values();
                if (!leftovers.isEmpty()) {
                    dropped = true;
                    for (ItemStack leftover : leftovers) {
                        target.getWorld().dropItemNaturally(target.getLocation(), leftover);
                    }
                }
            }
        } else {
            for (int i = 0; i < amount; i++) {
                ItemStack item = customItem.createItem();
                Collection<ItemStack> leftovers = target.getInventory().addItem(item).values();
                if (!leftovers.isEmpty()) {
                    dropped = true;
                    for (ItemStack leftover : leftovers) {
                        target.getWorld().dropItemNaturally(target.getLocation(), leftover);
                    }
                }
            }
        }

        plugin.getMessages().send(sender, "given",
                "{item}", itemName,
                "{amount}", String.valueOf(amount),
                "{player}", target.getName());

        plugin.getMessages().send(target, "received",
                "{item}", itemName,
                "{amount}", String.valueOf(amount));

        if (dropped) {
            plugin.getMessages().send(sender, "dropped-nearby");
            plugin.getMessages().send(target, "dropped-nearby");
        }

        return true;
    }

    private boolean handleMenu(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessages().send(sender, "only-player");
            return true;
        }

        Player player = (Player) sender;
        int page = 0;

        if (args.length >= 2) {
            try {
                page = Math.max(0, Integer.parseInt(args[1]) - 1);
            } catch (NumberFormatException exception) {
                page = 0;
            }
        }

        plugin.getMenuManager().open(player, page);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return partial(args[0], Arrays.asList("help", "reload", "give", "menu"));
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (args.length == 2) {
                List<String> players = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    players.add(player.getName());
                }
                return partial(args[1], players);
            }

            if (args.length == 3) {
                return partial(args[2], plugin.getItemRegistry().getIds());
            }

            if (args.length == 4) {
                return partial(args[3], Arrays.asList("1", "16", "32", "64"));
            }
        }

        if (args[0].equalsIgnoreCase("menu") && args.length == 2) {
            return partial(args[1], Arrays.asList("1"));
        }

        return Collections.emptyList();
    }

    private List<String> partial(String token, List<String> values) {
        List<String> result = new ArrayList<>();
        StringUtil.copyPartialMatches(token, values, result);
        Collections.sort(result);
        return result;
    }
}