package cloudinn.listener;

import cloudinn.CloudInnPlugin;
import cloudinn.title.TitleManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final CloudInnPlugin plugin;
    private final TitleManager titleManager;

    public PlayerListener(CloudInnPlugin plugin, TitleManager titleManager) {
        this.plugin = plugin;
        this.titleManager = titleManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();

        // Create player data if not exists
        plugin.getDatabaseManager().createPlayer(uuid, player.getName());

        // Load player's active title
        titleManager.loadPlayerTitle(player);

        // Show holograms to player
        plugin.getHologramManager().showToPlayer(player);

        // Send welcome message
        player.sendMessage("");
        player.sendMessage("§6§l✦═══════════════════════✦");
        player.sendMessage("§6§l     ✦ 云际驿站 ✦");
        player.sendMessage("");
        player.sendMessage(" §e欢迎回来 §f" + player.getName());
        
        String activeTitle = titleManager.getActiveTitle(uuid);
        String displayTitle = ChatColor.translateAlternateColorCodes('&', activeTitle);
        player.sendMessage(" §7当前称号: " + displayTitle);
        
        int points = plugin.getDatabaseManager().getPoints(uuid);
        player.sendMessage(" §7当前积分: §e" + points);
        player.sendMessage("");
        player.sendMessage(" §7输入 §a/title list §7查看称号");
        player.sendMessage(" §7输入 §a/checkin §7每日签到");
        player.sendMessage("§6§l✦═══════════════════════✦");
        player.sendMessage("");

        // 通过 WebSocket 通知网页端
        if (plugin.getWebSocketClient() != null && plugin.getWebSocketClient().isConnected()) {
            com.google.gson.JsonObject data = new com.google.gson.JsonObject();
            data.addProperty("action", "player_join");
            data.addProperty("player_name", player.getName());
            data.addProperty("uuid", uuid);
            plugin.getWebSocketClient().sendMessage("player_event", data);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // 通过 WebSocket 通知网页端
        if (plugin.getWebSocketClient() != null && plugin.getWebSocketClient().isConnected()) {
            com.google.gson.JsonObject data = new com.google.gson.JsonObject();
            data.addProperty("action", "player_quit");
            data.addProperty("player_name", player.getName());
            data.addProperty("uuid", player.getUniqueId().toString());
            plugin.getWebSocketClient().sendMessage("player_event", data);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        String activeTitle = titleManager.getActiveTitle(uuid);

        if (activeTitle != null && !activeTitle.equals("&7云际旅人")) {
            String displayTitle = ChatColor.translateAlternateColorCodes('&', activeTitle);
            String formattedMessage = ChatColor.translateAlternateColorCodes('&', 
                "§7[" + displayTitle + "§7] §f" + player.getName() + "§7: §f" + event.getMessage()
            );
            event.setFormat("%2$s");
            event.setMessage(formattedMessage.replace(player.getName() + "§7: §f", ""));
        }
    }
}