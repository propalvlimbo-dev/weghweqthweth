package ru.rooyzee.elytrixitem.hook;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import ru.rooyzee.elytrixitem.Main;

public final class EconomyHook {

    private final Main plugin;
    private Economy economy;

    public EconomyHook(Main plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault не найден. Экономические функции отключены.");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("Не найден провайдер экономики Vault. Экономические функции отключены.");
            return false;
        }

        this.economy = rsp.getProvider();
        return economy != null;
    }

    public boolean isEnabled() {
        return economy != null;
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (economy == null) {
            return false;
        }

        return economy.depositPlayer(player, amount).transactionSuccess();
    }
}