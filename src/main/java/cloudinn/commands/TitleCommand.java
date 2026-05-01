package cloudinn.commands;

import cloudinn.CloudInnPlugin;
import cloudinn.title.TitleManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class TitleCommand implements CommandExecutor {

    private final CloudInnPlugin plugin;
    private final TitleManager titleManager;

    public TitleCommand(CloudInnPlugin plugin, TitleManager titleManager) {
        this.plugin = plugin;
        this.titleManager = titleManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c该命令只能由玩家执行！");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                return handleList(player);
            case "set":
                if (args.length < 2) {
                    player.sendMessage("§c用法: /title set <称号名>");
                    return true;
                }
                return handleSet(player, args[1]);
            case "remove":
                if (args.length < 2) {
                    player.sendMessage("§c用法: /title remove <称号名>");
                    return true;
                }
                return handleRemove(player, args[1]);
            case "shop":
                return handleShop(player);
            case "buy":
                if (args.length < 2) {
                    player.sendMessage("§c用法: /title buy <称号名>");
                    return true;
                }
                return handleBuy(player, args[1]);
            case "clear":
                return handleClear(player);
            default:
                sendHelp(player);
                return true;
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l✦═══════════════════════✦");
        player.sendMessage("§6§l    ✦ 称号系统 ✦");
        player.sendMessage("");
        player.sendMessage(" §e/title list §7- 查看我的称号");
        player.sendMessage(" §e/title set <名称> §7- 设置当前称号");
        player.sendMessage(" §e/title remove <名称> §7- 移除称号");
        player.sendMessage(" §e/title shop §7- 称号商店");
        player.sendMessage(" §e/title buy <名称> §7- 购买称号");
        player.sendMessage(" §e/title clear §7- 清除当前称号");
        player.sendMessage("§6§l✦═══════════════════════✦");
        player.sendMessage("");
    }

    private boolean handleList(Player player) {
        String uuid = player.getUniqueId().toString();
        List<String> titles = titleManager.getPlayerTitles(uuid);
        String activeTitle = titleManager.getActiveTitle(uuid);

        player.sendMessage("");
        player.sendMessage("§6§l✦═══════════════════════✦");
        player.sendMessage("§6§l     ✦ 我的称号 ✦");
        player.sendMessage("");

        if (titles.isEmpty()) {
            player.sendMessage(" §7暂无称号，前往称号商店购买吧！");
            player.sendMessage(" §7输入 §e/title shop §7查看");
        } else {
            for (String title : titles) {
                String display = ChatColor.translateAlternateColorCodes('&', title);
                if (title.equals(activeTitle)) {
                    player.sendMessage(" §a▶ " + display + " §7(当前)");
                } else {
                    player.sendMessage(" §7- " + display);
                }
            }
        }

        player.sendMessage("");
        player.sendMessage("§6§l✦═══════════════════════✦");
        player.sendMessage("");
        return true;
    }

    private boolean handleSet(Player player, String titleName) {
        String uuid = player.getUniqueId().toString();

        // Find the full title code from player's titles
        List<String> playerTitles = titleManager.getPlayerTitles(uuid);
        String matchedTitle = null;

        for (String t : playerTitles) {
            String cleanName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', t));
            if (cleanName.equalsIgnoreCase(titleName) || t.contains(titleName)) {
                matchedTitle = t;
                break;
            }
        }

        if (matchedTitle == null) {
            player.sendMessage("§c你没有这个称号！输入 §e/title list §c查看你的称号");
            return true;
        }

        titleManager.setActiveTitle(player, matchedTitle);
        return true;
    }

    private boolean handleRemove(Player player, String titleName) {
        String uuid = player.getUniqueId().toString();

        List<String> playerTitles = titleManager.getPlayerTitles(uuid);
        String matchedTitle = null;

        for (String t : playerTitles) {
            String cleanName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', t));
            if (cleanName.equalsIgnoreCase(titleName) || t.contains(titleName)) {
                matchedTitle = t;
                break;
            }
        }

        if (matchedTitle == null) {
            player.sendMessage("§c你没有这个称号！");
            return true;
        }

        // If it's the active title, clear it first
        String activeTitle = titleManager.getActiveTitle(uuid);
        if (matchedTitle.equals(activeTitle)) {
            player.sendMessage("§c请先切换称号再移除！");
            return true;
        }

        titleManager.removePlayerTitle(uuid, matchedTitle);
        player.sendMessage("§a✓ 已移除称号: " + ChatColor.translateAlternateColorCodes('&', matchedTitle));
        return true;
    }

    private boolean handleShop(Player player) {
        List<TitleManager.TitleItem> titles = titleManager.getAvailableTitles();

        player.sendMessage("");
        player.sendMessage("§6§l✦═══════════════════════════✦");
        player.sendMessage("§6§l       ✦ 称号商店 ✦");
        player.sendMessage("");
        player.sendMessage(" §7输入 §e/title buy <名称> §7购买称号");
        player.sendMessage("");

        String currentCategory = "";
        for (TitleManager.TitleItem item : titles) {
            String category = item.getCategory();
            String rarity = item.getRarity();

            // Category header
            if (!category.equals(currentCategory)) {
                currentCategory = category;
                String categoryDisplay = getCategoryDisplay(category);
                player.sendMessage(" §8┌── " + categoryDisplay + " §8──");
            }

            String displayName = ChatColor.translateAlternateColorCodes('&', item.getDisplayName());
            String rarityColor = getRarityColor(rarity);
            String priceStr = item.getPrice() <= 0 ? "§c不可购买" : "§e" + item.getPrice() + " 积分";

            player.sendMessage(" §8│ " + rarityColor + "▪ §r" + displayName + " §7- " + item.getDescription());
            player.sendMessage(" §8│   §7价格: " + priceStr);
        }

        player.sendMessage(" §8└─────────────────────");
        player.sendMessage("");
        player.sendMessage("§6§l✦═══════════════════════════✦");
        player.sendMessage("");
        return true;
    }

    private boolean handleBuy(Player player, String titleName) {
        // Find the full title code
        List<TitleManager.TitleItem> shopTitles = titleManager.getAvailableTitles();
        String matchedTitle = null;

        for (TitleManager.TitleItem item : shopTitles) {
            String cleanName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', item.getDisplayName()));
            if (cleanName.equalsIgnoreCase(titleName) || item.getTitle().contains(titleName)) {
                matchedTitle = item.getTitle();
                break;
            }
        }

        if (matchedTitle == null) {
            player.sendMessage("§c该称号不存在！输入 §e/title shop §c查看可购买称号");
            return true;
        }

        titleManager.purchaseTitle(player, matchedTitle);
        return true;
    }

    private boolean handleClear(Player player) {
        String defaultTitle = plugin.getConfig().getString("titles.default-title", "&7云际旅人");
        titleManager.setActiveTitle(player, defaultTitle);
        player.sendMessage("§a✓ 已清除当前称号");
        return true;
    }

    private String getCategoryDisplay(String category) {
        switch (category) {
            case "special": return "§6特殊";
            case "achievement": return "§b成就";
            case "building": return "§a建筑";
            case "redstone": return "§c红石";
            case "pvp": return "§4PVP";
            case "mining": return "§7采集";
            case "farming": return "§2农业";
            case "magic": return "§d魔法";
            case "explore": return "§3探险";
            default: return "§f" + category;
        }
    }

    private String getRarityColor(String rarity) {
        switch (rarity) {
            case "legendary": return "§6";
            case "rare": return "§b";
            case "common": return "§7";
            default: return "§f";
        }
    }
}