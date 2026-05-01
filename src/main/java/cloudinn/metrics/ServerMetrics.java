package cloudinn.metrics;

import cloudinn.CloudInnPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Server;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;

public class ServerMetrics {

    private final CloudInnPlugin plugin;
    private final Server server;
    private long lastTick = 0;
    private double tps = 20.0;

    public ServerMetrics(CloudInnPlugin plugin) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        
        // Initialize TPS tracking
        lastTick = System.currentTimeMillis();
        
        // Schedule TPS calculation
        server.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            long diff = now - lastTick;
            
            // Calculate TPS based on time between ticks (50ms per tick theoretically)
            if (diff > 0) {
                double calculatedTps = 1000.0 / (diff / 20.0); // Average over 20 ticks
                if (calculatedTps > 20.0) calculatedTps = 20.0;
                tps = calculatedTps;
            }
            
            lastTick = now;
        }, 0L, 1L);
    }

    public double getTPS() {
        return tps;
    }

    public long getLatency() {
        // Average latency of online players
        return Bukkit.getOnlinePlayers().stream()
            .mapToLong(p -> {
                try {
                    return p.getPing();
                } catch (Exception e) {
                    return 0;
                }
            })
            .average()
            .orElse(0)
            .longValue();
    }

    public int getOnlinePlayers() {
        return server.getOnlinePlayers().size();
    }

    public int getMaxPlayers() {
        return server.getMaxPlayers();
    }

    public double getCPUUsage() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                return ((com.sun.management.OperatingSystemMXBean) osBean).getCpuLoad() * 100;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public long getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    public long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("server_id", plugin.getConfig().getInt("server-id", 1));
        metrics.put("server_name", plugin.getConfig().getString("server-name", "云际服务器"));
        metrics.put("server_version", plugin.getConfig().getString("server-version", "1.20.4"));
        metrics.put("online_players", getOnlinePlayers());
        metrics.put("max_players", getMaxPlayers());
        metrics.put("tps", Math.round(getTPS() * 100.0) / 100.0);
        metrics.put("latency", getLatency());
        metrics.put("cpu_usage", Math.round(getCPUUsage() * 100.0) / 100.0);
        metrics.put("memory_usage", getMemoryUsage());
        metrics.put("max_memory", getMaxMemory());
        metrics.put("timestamp", System.currentTimeMillis());
        return metrics;
    }
}