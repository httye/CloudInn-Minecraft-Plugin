package cloudinn.commands;

import cloudinn.CloudInnPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CloudInnCommand implements CommandExecutor {

    private final CloudInnPlugin plugin;

    public CloudInnCommand(CloudInnPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendInfo(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                return handleReload(sender);
            case "info":
                sendInfo(sender);
                return true;
            default:
                sender.sendMessage("§c用法: /cloudinn <reload|info>");
                return true;
        }
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("cloudinn.admin")) {
            sender.sendMessage("§c你没有权限执行此操作！");
            return true;
        }

        plugin.reloadConfig();
        sender.sendMessage("§a✦ 配置已重新加载！");
        return true;
    }

    private void sendInfo(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage("§6§l✦═══════════════════════✦");
        sender.sendMessage("§6§l    ✦ 云际驿站 CloudInn ✦");
        sender.sendMessage("");
        sender.sendMessage(" §7版本: §f1.0.0");
        sender.sendMessage(" §7在线: §a" + plugin.getServer().getOnlinePlayers().size() + "§7/§a" + plugin.getServer().getMaxPlayers());
        
        double tps = plugin.getServerMetrics().getTPS();
        String tpsColor = tps > 18 ? "§a" : tps > 15 ? "§e" : "§c";
        sender.sendMessage(" §7TPS: " + tpsColor + String.format("%.1f", tps));
        
        long latency = plugin.getServerMetrics().getLatency();
        sender.sendMessage(" §7延迟: §e" + latency + "ms");

        if (plugin.getWebSocketClient().isConnected()) {
            sender.sendMessage(" §7WebSocket: §a已连接");
        } else {
            sender.sendMessage(" §7WebSocket: §c未连接");
        }
        
        sender.sendMessage(" §7Web面板: §fhttp://localhost:3000");
        sender.sendMessage("");
        sender.sendMessage("§6§l✦═══════════════════════✦");
        sender.sendMessage("");
    }
}