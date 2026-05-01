package cloudinn.commands;

import cloudinn.CloudInnPlugin;
import cloudinn.checkin.CheckInManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class CheckInCommand implements CommandExecutor {

    private final CloudInnPlugin plugin;
    private final CheckInManager checkInManager;

    public CheckInCommand(CloudInnPlugin plugin, CheckInManager checkInManager) {
        this.plugin = plugin;
        this.checkInManager = checkInManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c该命令只能由玩家执行！");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Execute check-in
            checkInManager.checkIn(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "status":
                return handleStatus(player);
            case "help":
                sendHelp(player);
                return true;
            default:
                checkInManager.checkIn(player);
                return true;
        }
    }

    private boolean handleStatus(Player player) {
        Map<String, Object> status = checkInManager.getStatus(player);

        boolean checkedIn = (boolean) status.get("checked_in_today");
        int streak = (int) status.get("streak");
        int totalCheckins = (int) status.get("total_checkins");
        int points = (int) status.get("points");

        player.sendMessage("");
        player.sendMessage("§6§l✦═══════════════════════✦");
        player.sendMessage("§6§l    ✦ 签到状态 ✦");
        player.sendMessage("");
        player.sendMessage(" §7今日签到: " + (checkedIn ? "§a✓ 已签到" : "§c✗ 未签到"));
        player.sendMessage(" §7连续签到: §e" + streak + " §7天");
        player.sendMessage(" §7总签到: §e" + totalCheckins + " §7次");
        player.sendMessage(" §7当前积分: §e" + points);
        player.sendMessage("");
        player.sendMessage("§6§l✦═══════════════════════✦");
        player.sendMessage("");
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l✦═══════════════════════✦");
        player.sendMessage("§6§l    ✦ 签到系统 ✦");
        player.sendMessage("");
        player.sendMessage(" §e/checkin §7- 每日签到");
        player.sendMessage(" §e/checkin status §7- 查看签到状态");
        player.sendMessage(" §e/checkin help §7- 显示帮助");
        player.sendMessage("");
        player.sendMessage(" §7签到可获得积分，连续签到有额外奖励！");
        player.sendMessage("§6§l✦═══════════════════════✦");
        player.sendMessage("");
    }
}