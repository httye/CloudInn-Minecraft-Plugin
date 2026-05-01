package cloudinn.commands;

import cloudinn.CloudInnPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class PointsCommand implements CommandExecutor {

    private final CloudInnPlugin plugin;

    public PointsCommand(CloudInnPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c该命令只能由玩家执行！");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            return handleCheck(player);
        }

        switch (args[0].toLowerCase()) {
            case "check":
                return handleCheck(player);
            case "top":
                return handleTop(player, args);
            case "help":
                sendHelp(player);
                return true;
            default:
                handleCheck(player);
                return true;
        }
    }

    private boolean handleCheck(Player player) {
        String uuid = player.getUniqueId().toString();
        int points = plugin.getDatabaseManager().getPoints(uuid);

        player.sendMessage("");
        player.sendMessage("§6§l✦═══════════════════════✦");
        player.sendMessage("§6§l    ✦ 积分查询 ✦");
        player.sendMessage("");
        player.sendMessage(" §7玩家: §f" + player.getName());
        player.sendMessage(" §7当前积分: §e" + points);
        player.sendMessage(" §7输入 §e/points top §7查看积分排行");
        player.sendMessage(" §7签到获取积分: §e/checkin");
        player.sendMessage("§6§l✦═══════════════════════✦");
        player.sendMessage("");
        return true;
    }

    private boolean handleTop(Player player, String[] args) {
        int page = 1;
        if (args.length > 1) {
            try {
                page = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException ignored) {}
        }

        int offset = (page - 1) * 10;

        player.sendMessage("");
        player.sendMessage("§6§l✦═══════════════════════✦");
        player.sendMessage("§6§l    ✦ 积分排行榜 ✦");
        if (page > 1) {
            player.sendMessage(" §7第 §e" + page + " §7页");
        }
        player.sendMessage("");

        try {
            PreparedStatement ps = plugin.getDatabaseManager().getConnection().prepareStatement(
                "SELECT player_name, points FROM player_data ORDER BY points DESC LIMIT 10 OFFSET ?"
            );
            ps.setInt(1, offset);
            ResultSet rs = ps.executeQuery();

            int rank = offset + 1;
            boolean hasData = false;
            while (rs.next()) {
                hasData = true;
                String name = rs.getString("player_name");
                int pts = rs.getInt("points");
                
                String prefix;
                if (rank == 1) prefix = "§6#1";
                else if (rank == 2) prefix = "§b#2";
                else if (rank == 3) prefix = "§a#3";
                else prefix = "§7#" + rank;

                player.sendMessage(" " + prefix + " §f" + name + " §7- §e" + pts + " §7积分");
                rank++;
            }
            rs.close();
            ps.close();

            if (!hasData) {
                player.sendMessage(" §7暂无排行数据");
            }

            player.sendMessage("");
            player.sendMessage(" §7输入 §e/points top " + (page + 1) + " §7下一页");
            player.sendMessage("§6§l✦═══════════════════════✦");
            player.sendMessage("");

        } catch (Exception e) {
            player.sendMessage("§c查询排行失败");
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l✦═══════════════════════✦");
        player.sendMessage("§6§l    ✦ 积分系统 ✦");
        player.sendMessage("");
        player.sendMessage(" §e/points §7- 查看我的积分");
        player.sendMessage(" §e/points top [页数] §7- 积分排行榜");
        player.sendMessage("");
        player.sendMessage(" §7获取积分方式:");
        player.sendMessage(" §e✦ §7每日签到 §e/checkin");
        player.sendMessage("§6§l✦═══════════════════════✦");
        player.sendMessage("");
    }
}