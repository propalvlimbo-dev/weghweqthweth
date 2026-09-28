package ru.rooyzee.elytrixitem;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.rooyzee.elytrixitem.command.ElytriXItemsCommand;
import ru.rooyzee.elytrixitem.enchant.EnchantBookListener;
import ru.rooyzee.elytrixitem.enchant.EnchantRegistry;
import ru.rooyzee.elytrixitem.enchant.impl.DrillBook;
import ru.rooyzee.elytrixitem.enchant.impl.DrillEnchant;
import ru.rooyzee.elytrixitem.enchant.impl.DrillListener;
import ru.rooyzee.elytrixitem.enchant.impl.ExplosiveArrowBook;
import ru.rooyzee.elytrixitem.enchant.impl.ExplosiveArrowEnchant;
import ru.rooyzee.elytrixitem.enchant.impl.ExplosiveArrowListener;
import ru.rooyzee.elytrixitem.enchant.impl.SunShacklesBook;
import ru.rooyzee.elytrixitem.enchant.impl.SunShacklesEnchant;
import ru.rooyzee.elytrixitem.hook.EconomyHook;
import ru.rooyzee.elytrixitem.item.ItemRegistry;
import ru.rooyzee.elytrixitem.item.SpawnItemListener;
import ru.rooyzee.elytrixitem.item.aura.AuraListener;
import ru.rooyzee.elytrixitem.item.aura.AuraManager;
import ru.rooyzee.elytrixitem.item.aura.impl.CrystalProtectionAura;
import ru.rooyzee.elytrixitem.item.aura.impl.FallProtectionAura;
import ru.rooyzee.elytrixitem.item.aura.impl.MinerAura;
import ru.rooyzee.elytrixitem.item.aura.impl.MinerAuraListener;
import ru.rooyzee.elytrixitem.item.impl.AntiPearl;
import ru.rooyzee.elytrixitem.item.impl.AntiPearlListener;
import ru.rooyzee.elytrixitem.item.impl.CollectorSword;
import ru.rooyzee.elytrixitem.item.impl.CollectorSwordListener;
import ru.rooyzee.elytrixitem.item.impl.ExperiencePot;
import ru.rooyzee.elytrixitem.item.impl.ExperiencePotListener;
import ru.rooyzee.elytrixitem.item.impl.ExplosiveTrap;
import ru.rooyzee.elytrixitem.item.impl.ExplosiveTrapListener;
import ru.rooyzee.elytrixitem.item.impl.InfiniteFirework;
import ru.rooyzee.elytrixitem.item.impl.InfiniteFireworkListener;
import ru.rooyzee.elytrixitem.item.impl.Tyagun;
import ru.rooyzee.elytrixitem.item.impl.TyagunListener;
import ru.rooyzee.elytrixitem.menu.MenuListener;
import ru.rooyzee.elytrixitem.menu.MenuManager;
import ru.rooyzee.elytrixitem.message.MessageService;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class Main extends JavaPlugin {

    private MessageService messages;
    private FileConfiguration menuConfig;
    private FileConfiguration settings;
    private ItemRegistry itemRegistry;
    private MenuManager menuManager;
    private AuraManager auraManager;
    private EconomyHook economyHook;
    private EnchantRegistry enchantRegistry;

    @Override
    public void onEnable() {
        createResource("messages.yml");
        createResource("menu.yml");
        createResource("config.yml");

        loadPluginData();

        economyHook = new EconomyHook(this);
        economyHook.setup();

        ElytriXItemsCommand command = new ElytriXItemsCommand(this);
        PluginCommand pluginCommand = Objects.requireNonNull(getCommand("elytrixitems"));
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);

        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new SpawnItemListener(this), this);
        getServer().getPluginManager().registerEvents(new CollectorSwordListener(this), this);
        getServer().getPluginManager().registerEvents(new InfiniteFireworkListener(this), this);
        getServer().getPluginManager().registerEvents(new ExplosiveTrapListener(this), this);
        getServer().getPluginManager().registerEvents(new TyagunListener(this), this);
        getServer().getPluginManager().registerEvents(new ExperiencePotListener(this), this);
        getServer().getPluginManager().registerEvents(new AntiPearlListener(this), this);
        getServer().getPluginManager().registerEvents(new AuraListener(this, auraManager), this);
        getServer().getPluginManager().registerEvents(new MinerAuraListener(this, auraManager), this);
        getServer().getPluginManager().registerEvents(new EnchantBookListener(this), this);
        getServer().getPluginManager().registerEvents(new DrillListener(this), this);
        getServer().getPluginManager().registerEvents(new ExplosiveArrowListener(this), this);
    }

    @Override
    public void onDisable() {
    }

    public void reloadPlugin() {
        loadPluginData();
    }

    private void loadPluginData() {
        this.messages = new MessageService(this, "messages.yml");
        this.settings = loadYamlWithDefaults("config.yml");
        this.menuConfig = loadYamlWithDefaults("menu.yml");
        this.itemRegistry = new ItemRegistry();
        this.enchantRegistry = new EnchantRegistry();
        registerEnchants();
        registerItems();
        this.menuManager = new MenuManager(this);
        this.auraManager = new AuraManager(this);
    }

    private void registerEnchants() {
        enchantRegistry.register(new DrillEnchant(this));
        enchantRegistry.register(new ExplosiveArrowEnchant(this));
        enchantRegistry.register(new SunShacklesEnchant(this));
    }

    private void registerItems() {
        itemRegistry.register(new CollectorSword(this));
        itemRegistry.register(new InfiniteFirework(this));
        itemRegistry.register(new Tyagun(this));
        itemRegistry.register(new ExperiencePot(this));
        itemRegistry.register(new AntiPearl(this));
        itemRegistry.register(new ExplosiveTrap(this));
        itemRegistry.register(new FallProtectionAura(this));
        itemRegistry.register(new CrystalProtectionAura(this));
        itemRegistry.register(new MinerAura(this));
        itemRegistry.register(new DrillBook(this));
        itemRegistry.register(new ExplosiveArrowBook(this));
        itemRegistry.register(new SunShacklesBook(this));
    }

    private void createResource(String fileName) {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        File file = new File(getDataFolder(), fileName);
        if (!file.exists()) {
            saveResource(fileName, false);
        }
    }

    private FileConfiguration loadYamlWithDefaults(String fileName) {
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(new File(getDataFolder(), fileName));

        // Подтягиваем дефолты из jar: свежие ключи появятся даже у тех,
        // у кого на сервере уже лежит старая версия файла.
        InputStream defaults = getResource(fileName);
        if (defaults != null) {
            configuration.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaults, StandardCharsets.UTF_8)));
        }

        return configuration;
    }

    public MessageService getMessages() {
        return messages;
    }

    public FileConfiguration getMenuConfig() {
        return menuConfig;
    }

    public FileConfiguration getSettings() {
        return settings;
    }

    public ItemRegistry getItemRegistry() {
        return itemRegistry;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    public AuraManager getAuraManager() {
        return auraManager;
    }

    public EconomyHook getEconomyHook() {
        return economyHook;
    }

    public EnchantRegistry getEnchantRegistry() {
        return enchantRegistry;
    }
}