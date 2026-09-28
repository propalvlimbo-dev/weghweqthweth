package ru.rooyzee.elytrixitem.message;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.rooyzee.elytrixitem.util.ColorUtil;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class MessageService {

    private final JavaPlugin plugin;
    private final String fileName;
    private FileConfiguration config;

    public MessageService(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        reload();
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), fileName));

        // Дефолты из jar: новые ключи сообщений работают и на серверах со старым messages.yml
        InputStream defaults = plugin.getResource(fileName);
        if (defaults != null) {
            config.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaults, StandardCharsets.UTF_8)));
        }
    }

    public String get(String path, String... replacements) {
        String text = config.getString(path, "");
        return ColorUtil.color(apply(text, replacements));
    }

    public List<String> getList(String path, String... replacements) {
        List<String> source = config.getStringList(path);
        List<String> result = new ArrayList<>();

        for (String line : source) {
            result.add(ColorUtil.color(apply(line, replacements)));
        }

        return result;
    }

    public void send(CommandSender sender, String path, String... replacements) {
        String message = get(path, replacements);
        if (message.isEmpty()) {
            return;
        }

        // Одиночные сообщения показываем над инвентарём (action bar), а не в чат.
        // Консоль/не-игроки получают обычное сообщение.
        if (sender instanceof Player) {
            ((Player) sender).spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
            return;
        }

        sender.sendMessage(message);
    }

    public void sendList(CommandSender sender, String path, String... replacements) {
        for (String line : getList(path, replacements)) {
            sender.sendMessage(line);
        }
    }

    private String apply(String text, String... replacements) {
        String prefix = config.getString("prefix", "");
        String result = text.replace("{prefix}", prefix);

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            result = result.replace(replacements[i], replacements[i + 1]);
        }

        return result;
    }
}