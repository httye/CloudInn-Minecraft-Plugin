package cloudinn.checkin;

import cloudinn.CloudInnPlugin;
import cloudinn.database.DatabaseManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class CheckInManager {

    private final CloudInnPlugin plugin;
    private final DatabaseManager databaseManager;

    public CheckInManager(CloudInnPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    /**
     * Execute check-in for a player
     * @param player the player to check in
     * @return result map with check-in details
     */
    public Map<String, Object> checkIn(Player player) {
        String uuid = player.getUniqueId().toString();

        // Check if already checked in today
        if (databaseManager.hasCheckedInToday(uuid)) {
            player.sendMessage("§e✦ 你今天已经签到过了！");
            player.sendMessage("§7明天再来吧，连续签到有额外奖励哦~");
            return Map.of("success", false, "reason", "already_checked_in");
        }

        // Execute check-in
        Map<String, Object> result = databaseManager.doCheckIn(uuid);

        if (result.containsKey("success") && (boolean) result.get("success")) {
            int points = (int) result.get("points");
            int streak = (int) result.get("streak");
            int streakBonus = (int) result.get("streak_bonus");

            // Send beautiful check-in message
            player.sendMessage("");
            player.sendMessage("§6§l✦═══════════════════════✦");
            player.sendMessage("§6§l  每日签到");
            player.sendMessage("");
            player.sendMessage(" §a✓ 签到成功！");
            player.sendMessage(" §e✦ 获得 §6" + points + " §e积分");
            
            if (streak > 0) {
                player.sendMessage(" §b✦ 连续签到 §f" + streak + " §b天");
            }
            
            if (streakBonus > 0) {
                player.sendMessage(" §d✦ 连续签到奖励 §f+" + streakBonus + " §d积分！");
            }

            int totalPoints = databaseManager.getPoints(uuid);
            player.sendMessage(" §7当前总积分: §e" + totalPoints);
            player.sendMessage("");
            player.sendMessage("§6§l✦═══════════════════════✦");
            player.sendMessage("");

            // Check streak achievements
            checkStreakAchievements(player, streak);
        }

        return result;
    }

    /**
     * Get check-in status for a player
     */
    public Map<String, Object> getStatus(Player player) {
        String uuid = player.getUniqueId().toString();
        Map<String, Object> data = databaseManager.getPlayerData(uuid);

        boolean checkedInToday = databaseManager.hasCheckedInToday(uuid);
        int streak = data.containsKey("streak_days") ? (int) data.get("streak_days") : 0;
        int totalCheckins = data.containsKey("total_checkins") ? (int) data.get("total_checkins") : 0;
        int points = data.containsKey("points") ? (int) data.get("points") : 0;

        return Map.of(
            "checked_in_today", checkedInToday,
            "streak", streak,
            "total_checkins", totalCheckins,
            "points", points
        );
    }

    /**
     * Check and reward streak achievements
     */
    private void checkStreakAchievements(Player player, int streak) {
        if (streak == 7) {
            player.sendMessage("§b§l✦ 成就解锁: §e连续签到7天！");
            databaseManager.addPoints(player.getUniqueId().toString(), 30);
            player.sendMessage("§a✦ 额外奖励 30 积分！");
        } else if (streak == 14) {
            player.sendMessage("§b§l✦ 成就解锁: §e连续签到14天！");
            databaseManager.addPoints(player.getUniqueId().toString(), 50);
            player.sendMessage("§a✦ 额外奖励 50 积分！");
        } else if (streak == 21) {
            player.sendMessage("§b§l✦ 成就解锁: §e连续签到21天！");
            databaseManager.addPoints(player.getUniqueId().toString(), 80);
            player.sendMessage("§a✦ 额外奖励 80 积分！");
        } else if (streak == 30) {
            player.sendMessage("§6§l✦ 传奇成就: §e连续签到30天！");
            databaseManager.addPoints(player.getUniqueId().toString(), 150);
            // Give special title
            databaseManager.addPlayerTitle(player.getUniqueId().toString(), "&b签到达人");
            player.sendMessage("§a✦ 获得称号「&b签到达人§a」！");
            player.sendMessage("§a✦ 额外奖励 150 积分！");
        }

        // Every 30 days after first month
        if (streak > 30 && streak % 30 == 0) {
            player.sendMessage("§6§l✦ 超级成就: §e连续签到 " + streak + " 天！");
            databaseManager.addPoints(player.getUniqueId().toString(), 200);
            player.sendMessage("§a✦ 额外奖励 200 积分！");
        }
    }
}