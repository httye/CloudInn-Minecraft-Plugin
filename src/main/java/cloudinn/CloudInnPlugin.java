package cloudinn;

import cloudinn.commands.*;
import cloudinn.hologram.HologramManager;
import cloudinn.listener.PlayerListener;
import cloudinn.title.TitleManager;
import cloudinn.checkin.CheckInManager;
import cloudinn.websocket.WebSocketClient;
import cloudinn.metrics.ServerMetrics;
import cloudinn.database.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

public class CloudInnPlugin extends JavaPlugin {

    private static CloudInnPlugin instance;
    private DatabaseManager databaseManager;
    private HologramManager hologramManager;
    private TitleManager titleManager;
    private CheckInManager checkInManager;
    private WebSocketClient webSocketClient;
    private ServerMetrics serverMetrics;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        getLogger().info("✦ 云际驿站 CloudInn 插件加载中...");

        // 初始化数据库
        databaseManager = new DatabaseManager(this);
        databaseManager.init();

        // 初始化各模块
        serverMetrics = new ServerMetrics(this);
        hologramManager = new HologramManager(this);
        titleManager = new TitleManager(this, databaseManager);
        checkInManager = new CheckInManager(this, databaseManager);

        // 注册命令
        getCommand("cloudinn").setExecutor(new CloudInnCommand(this));
        getCommand("title").setExecutor(new TitleCommand(this, titleManager));
        getCommand("checkin").setExecutor(new CheckInCommand(this, checkInManager));
        getCommand("points").setExecutor(new PointsCommand(this));

        // 注册监听器
        getServer().getPluginManager().registerEvents(new PlayerListener(this, titleManager), this);

        // 连接 WebSocket
        String wsUrl = getConfig().getString("websocket.url", "ws://localhost:3000/ws/server");
        webSocketClient = new WebSocketClient(this, wsUrl);
        webSocketClient.connect();

        // 启动定时任务 - 更新悬浮字
        getServer().getScheduler().runTaskTimer(this, () -> {
            hologramManager.updateAll();
        }, 0L, 20L); // 每秒更新

        // 启动定时任务 - 发送服务器状态到 WebSocket
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (webSocketClient != null && webSocketClient.isConnected()) {
                webSocketClient.sendServerStatus(serverMetrics.getMetrics());
            }
        }, 0L, 40L); // 每2秒发送

        getLogger().info("✦ 云际驿站 CloudInn 插件加载完成！");
    }

    @Override
    public void onDisable() {
        if (webSocketClient != null) {
            webSocketClient.disconnect();
        }
        if (hologramManager != null) {
            hologramManager.removeAll();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("✦ 云际驿站 CloudInn 插件已卸载");
    }

    public static CloudInnPlugin getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public TitleManager getTitleManager() {
        return titleManager;
    }

    public CheckInManager getCheckInManager() {
        return checkInManager;
    }

    public WebSocketClient getWebSocketClient() {
        return webSocketClient;
    }

    public ServerMetrics getServerMetrics() {
        return serverMetrics;
    }
}