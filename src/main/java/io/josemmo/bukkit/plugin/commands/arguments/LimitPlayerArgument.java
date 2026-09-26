package io.josemmo.bukkit.plugin.commands.arguments;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.josemmo.bukkit.plugin.YamipaPlugin;
import io.josemmo.bukkit.plugin.renderer.ImageRenderer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("deprecation")
public class LimitPlayerArgument extends StringArgument {
    /**
     * Limit player argument constructor
     * @param name Argument name
     */
    public LimitPlayerArgument(@NotNull String name) {
        super(name);
    }

    @Override
    public @NotNull CompletableFuture<Suggestions> suggest(@NotNull CommandSender sender, @NotNull SuggestionsBuilder builder) {
        getAllowedValues().keySet().forEach(builder::suggest);
        return builder.buildFuture();
    }

    @Override
    public @NotNull Object parse(@NotNull CommandSender sender, @NotNull Object rawValue) {
        String raw = (String) rawValue;
        OfflinePlayer known = getAllowedValues().get(raw);
        return (known != null) ? known : Bukkit.getOfflinePlayer(raw);
    }

    private @NotNull Map<String, OfflinePlayer> getAllowedValues() {
        Map<String, OfflinePlayer> values = new HashMap<>();
        ImageRenderer renderer = YamipaPlugin.getInstance().getRenderer();
        for (OfflinePlayer player : renderer.getPlayersWithPlacedImages()) {
            values.put(player.getUniqueId().toString(), player);
            if (player.getName() != null) {
                values.put(player.getName(), player);
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            values.put(player.getUniqueId().toString(), player);
            values.put(player.getName(), player);
        }
        return values;
    }
}
