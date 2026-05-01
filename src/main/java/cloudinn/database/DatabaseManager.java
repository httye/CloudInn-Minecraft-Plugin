package cloudinn.database;

import cloudinn.CloudInnPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.*;

public class DatabaseManager {

    private final CloudInnPlugin plugin;
    private Connection connection;

    public DatabaseManager(CloudInnPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            FileConfiguration config = plugin.getConfig();
            String type = config.getString("database.type", "sqlite");

            if (type.equalsIgnoreCase("mysql")) {
                String host = config.getString("database.mysql.host", "localhost");
                int port = config.getInt("database.mysql.port", 3306);
                String db = config.getString("database.mysql.database", "cloudinn");
                String user = config.getString("database.mysql.username", "root");
                String pass = config.getString("database.mysql.password", "");

                Class.forName("com.mysql.jdbc.Driver");
                connection = DriverManager.getConnection(
                    "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&characterEncoding=utf8",
                    user, pass
                );
            } else {
                String file = config.getString("database.sqlite.file", "plugins/CloudInn/data.db");
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + file);
            }

            createTables();
            plugin.getLogger().info("✓ 数据库连接成功 (" + type + ")");
        } catch (Exception e) {
            plugin.getLogger().severe("✗ 数据库连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTables() throws SQLException {
        Statement stmt = connection.createStatement();

        // 玩家数据表
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS player_data (" +
            "  uuid VARCHAR(36) PRIMARY KEY," +
            "  player_name VARCHAR(32)," +
            "  points INT DEFAULT 0," +
            "  active_title VARCHAR(64) DEFAULT '&7云际旅人'," +
            "  last_checkin DATE," +
            "  streak_days INT DEFAULT 0," +
            "  total_checkins INT DEFAULT 0," +
            "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );

        // 称号表
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS player_titles (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  uuid VARCHAR(36)," +
            "  title VARCHAR(64)," +
            "  obtained_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "  FOREIGN KEY (uuid) REFERENCES player_data(uuid)" +
            ")"
        );

        // 签到日志表
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS checkin_log (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  uuid VARCHAR(36)," +
            "  checkin_date DATE," +
            "  points_earned INT," +
            "  streak_bonus INT," +
            "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "  FOREIGN KEY (uuid) REFERENCES player_data(uuid)" +
            ")"
        );

        // 称号商店表
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS title_shop (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  title VARCHAR(64) UNIQUE," +
            "  display_name VARCHAR(64)," +
            "  description VARCHAR(256)," +
            "  price INT," +
            "  category VARCHAR(32) DEFAULT 'common'," +
            "  rarity VARCHAR(16) DEFAULT 'common'," +
            "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );

        stmt.close();
    }

    // ===== 玩家数据操作 =====

    public void createPlayer(String uuid, String name) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO player_data (uuid, player_name, points, active_title) VALUES (?, ?, 0, '&7云际旅人')"
            );
            ps.setString(1, uuid);
            ps.setString(2, name);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("创建玩家数据失败: " + e.getMessage());
        }
    }

    public Map<String, Object> getPlayerData(String uuid) {
        Map<String, Object> data = new HashMap<>();
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM player_data WHERE uuid = ?");
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                data.put("points", rs.getInt("points"));
                data.put("active_title", rs.getString("active_title"));
                data.put("last_checkin", rs.getString("last_checkin"));
                data.put("streak_days", rs.getInt("streak_days"));
                data.put("total_checkins", rs.getInt("total_checkins"));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("获取玩家数据失败: " + e.getMessage());
        }
        return data;
    }

    public int getPoints(String uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT points FROM player_data WHERE uuid = ?");
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int points = rs.getInt("points");
                rs.close();
                ps.close();
                return points;
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("获取积分失败: " + e.getMessage());
        }
        return 0;
    }

    public void addPoints(String uuid, int amount) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "UPDATE player_data SET points = points + ?, updated_at = CURRENT_TIMESTAMP WHERE uuid = ?"
            );
            ps.setInt(1, amount);
            ps.setString(2, uuid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("添加积分失败: " + e.getMessage());
        }
    }

    public boolean deductPoints(String uuid, int amount) {
        try {
            // Check if player has enough points
            int current = getPoints(uuid);
            if (current < amount) return false;

            PreparedStatement ps = connection.prepareStatement(
                "UPDATE player_data SET points = points - ?, updated_at = CURRENT_TIMESTAMP WHERE uuid = ?"
            );
            ps.setInt(1, amount);
            ps.setString(2, uuid);
            ps.executeUpdate();
            ps.close();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("扣除积分失败: " + e.getMessage());
            return false;
        }
    }

    // ===== 称号操作 =====

    public List<String> getPlayerTitles(String uuid) {
        List<String> titles = new ArrayList<>();
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT title FROM player_titles WHERE uuid = ? ORDER BY obtained_at DESC");
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                titles.add(rs.getString("title"));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("获取称号列表失败: " + e.getMessage());
        }
        return titles;
    }

    public void addPlayerTitle(String uuid, String title) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO player_titles (uuid, title) VALUES (?, ?)"
            );
            ps.setString(1, uuid);
            ps.setString(2, title);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("添加称号失败: " + e.getMessage());
        }
    }

    public void removePlayerTitle(String uuid, String title) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM player_titles WHERE uuid = ? AND title = ?"
            );
            ps.setString(1, uuid);
            ps.setString(2, title);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("移除称号失败: " + e.getMessage());
        }
    }

    public String getActiveTitle(String uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT active_title FROM player_data WHERE uuid = ?");
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String title = rs.getString("active_title");
                rs.close();
                ps.close();
                return title;
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("获取当前称号失败: " + e.getMessage());
        }
        return "&7云际旅人";
    }

    public void setActiveTitle(String uuid, String title) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "UPDATE player_data SET active_title = ?, updated_at = CURRENT_TIMESTAMP WHERE uuid = ?"
            );
            ps.setString(1, title);
            ps.setString(2, uuid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("设置当前称号失败: " + e.getMessage());
        }
    }

    // ===== 签到操作 =====

    public boolean hasCheckedInToday(String uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "SELECT last_checkin FROM player_data WHERE uuid = ?"
            );
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String lastDate = rs.getString("last_checkin");
                rs.close();
                ps.close();
                String today = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
                return today.equals(lastDate);
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("检查签到状态失败: " + e.getMessage());
        }
        return false;
    }

    public Map<String, Object> doCheckIn(String uuid) {
        Map<String, Object> result = new HashMap<>();
        try {
            String today = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());

            // 检查昨天是否签到（连续签到）
            PreparedStatement ps = connection.prepareStatement(
                "SELECT last_checkin, streak_days FROM player_data WHERE uuid = ?"
            );
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            int streak = 0;
            if (rs.next()) {
                String lastDate = rs.getString("last_checkin");
                streak = rs.getInt("streak_days");

                // 如果有上次签到日期
                if (lastDate != null) {
                    java.util.Date last = java.sql.Date.valueOf(lastDate);
                    java.util.Date yesterday = new java.util.Date(System.currentTimeMillis() - 86400000L);
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");

                    if (sdf.format(last).equals(sdf.format(yesterday))) {
                        streak++;
                    } else if (!sdf.format(last).equals(today)) {
                        streak = 0;
                    }
                }
            }
            rs.close();
            ps.close();

            // 计算积分
            int basePoints = plugin.getConfig().getInt("checkin.points-per-day", 10);
            int bonusInterval = plugin.getConfig().getInt("checkin.streak-bonus-interval", 7);
            int bonusPoints = plugin.getConfig().getInt("checkin.streak-bonus", 5);
            int totalPoints = basePoints;
            int streakBonus = 0;

            if (streak > 0 && streak % bonusInterval == 0) {
                streakBonus = bonusPoints * (streak / bonusInterval);
                totalPoints += streakBonus;
            }

            // 更新签到数据
            PreparedStatement update = connection.prepareStatement(
                "UPDATE player_data SET last_checkin = ?, streak_days = ?, total_checkins = total_checkins + 1, " +
                "points = points + ?, updated_at = CURRENT_TIMESTAMP WHERE uuid = ?"
            );
            update.setString(1, today);
            update.setInt(2, streak);
            update.setInt(3, totalPoints);
            update.setString(4, uuid);
            update.executeUpdate();
            update.close();

            // 记录签到日志
            PreparedStatement log = connection.prepareStatement(
                "INSERT INTO checkin_log (uuid, checkin_date, points_earned, streak_bonus) VALUES (?, ?, ?, ?)"
            );
            log.setString(1, uuid);
            log.setString(2, today);
            log.setInt(3, totalPoints);
            log.setInt(4, streakBonus);
            log.executeUpdate();
            log.close();

            result.put("points", totalPoints);
            result.put("streak", streak);
            result.put("streak_bonus", streakBonus);
            result.put("success", true);
        } catch (SQLException e) {
            plugin.getLogger().warning("签到失败: " + e.getMessage());
            result.put("success", false);
        }
        return result;
    }

    // ===== 称号商店 =====

    public List<Map<String, Object>> getTitleShop() {
        List<Map<String, Object>> titles = new ArrayList<>();
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM title_shop ORDER BY price ASC");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> t = new HashMap<>();
                t.put("id", rs.getInt("id"));
                t.put("title", rs.getString("title"));
                t.put("display_name", rs.getString("display_name"));
                t.put("description", rs.getString("description"));
                t.put("price", rs.getInt("price"));
                t.put("category", rs.getString("category"));
                t.put("rarity", rs.getString("rarity"));
                titles.add(t);
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("获取称号商店失败: " + e.getMessage());
        }
        return titles;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("数据库关闭失败: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }
}