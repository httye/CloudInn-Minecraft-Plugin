package cloudinn.title;

import cloudinn.CloudInnPlugin;
import cloudinn.database.DatabaseManager;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public class TitleManager {

    private final CloudInnPlugin plugin;
    private final DatabaseManager databaseManager;
    private final Map<String, String> playerTitleCache = new HashMap<>();

    // 预定义的称号商店
    private final List<TitleItem> defaultTitles = Arrays.asList(
        new TitleItem("&6云际开拓者", "&6云际开拓者", "最初的探险者", 0, "special", "legendary"),
        new TitleItem("&b签到达人", "&b签到达人", "连续签到30天获得", 500, "achievement", "rare"),
        new TitleItem("&a建筑大师", "&a建筑大师", "建筑领域的佼佼者", 300, "building", "rare"),
        new TitleItem("&d红石科技", "&d红石科技", "精通红石机械", 300, "redstone", "rare"),
        new TitleItem("&cPVP王者", "&cPVP王者", "战斗中的王者", 400, "pvp", "rare"),
        new TitleItem("&e勤劳矿工", "&e勤劳矿工", "挖矿达人", 200, "mining", "common"),
        new TitleItem("&2农场主", "&2农场主", "农业种植专家", 200, "farming", "common"),
        new TitleItem("&5魔法师", "&5魔法师", "探索魔法的奥秘", 350, "magic", "rare"),
        new TitleItem("&3探险家", "&3探险家", "探索世界的每一个角落", 250, "explore", "common"),
        new TitleItem("&c&l传说勇士", "&c&l传说勇士", "传说中的勇者", 1000, "special", "legendary"),
        new TitleItem("&b&l星辰旅者", "&b&l星辰旅者", "穿越星辰的旅者", 800, "special", "legendary"),
        new TitleItem("&e&l云际元老", "&e&l云际元老", "服务器创立之初的成员", 0, "special", "legendary")
    );

    public TitleManager(CloudInnPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        initDefaultTitles();
    }

    private void initDefaultTitles() {
        // Insert default titles into shop
        for (TitleItem item : defaultTitles) {
            try {
                java.sql.PreparedStatement ps = databaseManager.getConnection().prepareStatement(
                    "INSERT OR IGNORE INTO title_shop (title, display_name, description, price, category, rarity) VALUES (?, ?, ?, ?, ?, ?)"
                );
                ps.setString(1, item.title);
                ps.setString(2, item.displayName);
                ps.setString(3, item.description);
                ps.setInt(4, item.price);
                ps.setString(5, item.category);
                ps.setString(6, item.rarity);
                ps.executeUpdate();
                ps.close();
            } catch (Exception e) {
                plugin.getLogger().warning("初始化默认称号失败: " + e.getMessage());
            }
        }
    }

    public List<TitleItem> getAvailableTitles() {
        List<TitleItem> titles = new ArrayList<>();
        try {
            java.sql.PreparedStatement ps = databaseManager.getConnection().prepareStatement(
                "SELECT * FROM title_shop ORDER BY price ASC"
            );
            java.sql.ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                titles.add(new TitleItem(
                    rs.getString("title"),
                    rs.getString("display_name"),
                    rs.getString("description"),
                    rs.getInt("price"),
                    rs.getString("category"),
                    rs.getString("rarity")
                ));
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            plugin.getLogger().warning("获取称号列表失败: " + e.getMessage());
        }
        return titles;
    }

    public List<String> getPlayerTitles(String uuid) {
        return databaseManager.getPlayerTitles(uuid);
    }

    public void addPlayerTitle(String uuid, String title) {
        databaseManager.addPlayerTitle(uuid, title);
    }

    public void removePlayerTitle(String uuid, String title) {
        databaseManager.removePlayerTitle(uuid, title);
    }

    public boolean hasTitle(String uuid, String title) {
        return getPlayerTitles(uuid).contains(title);
    }

    public boolean purchaseTitle(Player player, String title) {
        String uuid = player.getUniqueId().toString();

        // Check if already owned
        if (hasTitle(uuid, title)) {
            player.sendMessage("§c你已经拥有这个称号了！");
            return false;
        }

        // Find title in shop
        TitleItem item = getAvailableTitles().stream()
            .filter(t -> t.title.equals(title))
            .findFirst().orElse(null);

        if (item == null) {
            player.sendMessage("§c该称号不存在！");
            return false;
        }

        if (item.price <= 0) {
            player.sendMessage("§c该称号无法购买！");
            return false;
        }

        // Deduct points
        if (databaseManager.deductPoints(uuid, item.price)) {
            databaseManager.addPlayerTitle(uuid, title);
            player.sendMessage("§a✦ 成功购买称号: " + ChatColor.translateAlternateColorCodes('&', item.displayName));
            return true;
        } else {
            player.sendMessage("§c积分不足！需要 " + item.price + " 积分。");
            return false;
        }
    }

    public boolean setActiveTitle(Player player, String title) {
        String uuid = player.getUniqueId().toString();

        if (!hasTitle(uuid, title)) {
            player.sendMessage("§c你没有这个称号！");
            return false;
        }

        databaseManager.setActiveTitle(uuid, title);
        playerTitleCache.put(uuid, title);
        player.sendMessage("§a✓ 已设置称号为: " + ChatColor.translateAlternateColorCodes('&', title));
        return true;
    }

    public String getActiveTitle(String uuid) {
        return playerTitleCache.getOrDefault(uuid, databaseManager.getActiveTitle(uuid));
    }

    public void loadPlayerTitle(Player player) {
        String uuid = player.getUniqueId().toString();
        String title = databaseManager.getActiveTitle(uuid);
        playerTitleCache.put(uuid, title);
    }

    public static class TitleItem {
        private final String title;
        private final String displayName;
        private final String description;
        private final int price;
        private final String category;
        private final String rarity;

        public TitleItem(String title, String displayName, String description, int price, String category, String rarity) {
            this.title = title;
            this.displayName = displayName;
            this.description = description;
            this.price = price;
            this.category = category;
            this.rarity = rarity;
        }

        public String getTitle() { return title; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public int getPrice() { return price; }
        public String getCategory() { return category; }
        public String getRarity() { return rarity; }
    }
}