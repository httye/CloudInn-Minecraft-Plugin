package cloudinn.hologram;

import cloudinn.CloudInnPlugin;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_20_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R4.util.CraftChatMessage;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager {

    private final CloudInnPlugin plugin;
    private final Map<String, List<HologramLine>> holograms = new ConcurrentHashMap<>();

    public HologramManager(CloudInnPlugin plugin) {
        this.plugin = plugin;
        loadHolograms();
    }

    private void loadHolograms() {
        if (!plugin.getConfig().getBoolean("holograms.enabled", true)) return;

        ConfigurationSection positions = plugin.getConfig().getConfigurationSection("holograms.positions");
        if (positions == null) return;

        for (String key : positions.getKeys(false)) {
            ConfigurationSection pos = positions.getConfigurationSection(key);
            if (pos == null) continue;

            String worldName = pos.getString("world", "world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("悬浮字所在世界 " + worldName + " 不存在");
                continue;
            }

            double x = pos.getDouble("x");
            double y = pos.getDouble("y");
            double z = pos.getDouble("z");
            List<String> lines = pos.getStringList("lines");

            Location loc = new Location(world, x, y, z);
            String id = key;
            createHologram(id, loc, lines);
        }
    }

    public void createHologram(String id, Location location, List<String> lines) {
        removeHologram(id);

        List<HologramLine> lineEntities = new ArrayList<>();
        double yOffset = 0;

        for (int i = lines.size() - 1; i >= 0; i--) {
            String text = lines.get(i);
            Location lineLoc = location.clone().add(0, yOffset, 0);
            
            EntityArmorStand armorStand = new EntityArmorStand(
                ((CraftWorld) location.getWorld()).getHandle(),
                lineLoc.getX(), lineLoc.getY(), lineLoc.getZ()
            );
            
            armorStand.setCustomName(CraftChatMessage.fromStringOrNull(convertColorCodes(text)));
            armorStand.setCustomNameVisible(true);
            armorStand.setInvisible(true);
            armorStand.setSmall(true);
            armorStand.setMarker(true);
            armorStand.setNoGravity(true);

            HologramLine line = new HologramLine(armorStand, text);
            lineEntities.add(line);
            yOffset += 0.3;
        }

        holograms.put(id, lineEntities);
        
        // 向所有在线玩家显示
        for (Player player : Bukkit.getOnlinePlayers()) {
            showToPlayer(player, lineEntities);
        }
    }

    public void removeHologram(String id) {
        List<HologramLine> lines = holograms.remove(id);
        if (lines != null) {
            for (HologramLine line : lines) {
                PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(line.getArmorStand().getId());
                sendPacketToAll(packet);
            }
        }
    }

    public void removeAll() {
        for (String id : holograms.keySet()) {
            removeHologram(id);
        }
    }

    public void updateAll() {
        String onlinePlayers = String.valueOf(Bukkit.getOnlinePlayers().size());
        String maxPlayers = String.valueOf(Bukkit.getMaxPlayers());
        String tps = String.format("%.1f", plugin.getServerMetrics().getTPS());
        String latency = String.valueOf(plugin.getServerMetrics().getLatency());

        for (Map.Entry<String, List<HologramLine>> entry : holograms.entrySet()) {
            for (HologramLine line : entry.getValue()) {
                String rawText = line.getRawText();
                String formatted = rawText
                    .replace("{online_players}", onlinePlayers)
                    .replace("{max_players}", maxPlayers)
                    .replace("{tps}", tps)
                    .replace("{latency}", latency);

                if (!line.getCurrentText().equals(formatted)) {
                    line.getArmorStand().setCustomName(CraftChatMessage.fromStringOrNull(convertColorCodes(formatted)));
                    line.setCurrentText(formatted);

                    // 更新实体数据包
                    PacketPlayOutEntityMetadata packet = new PacketPlayOutEntityMetadata(
                        line.getArmorStand().getId(),
                        line.getArmorStand().getDataWatcher(),
                        true
                    );
                    sendPacketToAll(packet);
                }
            }
        }
    }

    public void showToPlayer(Player player) {
        for (List<HologramLine> lines : holograms.values()) {
            showToPlayer(player, lines);
        }
    }

    private void showToPlayer(Player player, List<HologramLine> lines) {
        for (HologramLine line : lines) {
            PacketPlayOutSpawnEntityLiving spawnPacket = new PacketPlayOutSpawnEntityLiving(line.getArmorStand());
            sendPacket(player, spawnPacket);

            PacketPlayOutEntityMetadata metaPacket = new PacketPlayOutEntityMetadata(
                line.getArmorStand().getId(),
                line.getArmorStand().getDataWatcher(),
                true
            );
            sendPacket(player, metaPacket);
        }
    }

    private void sendPacketToAll(Packet<?> packet) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendPacket(player, packet);
        }
    }

    private void sendPacket(Player player, Packet<?> packet) {
        ((org.bukkit.craftbukkit.v1_20_R4.entity.CraftPlayer) player).getHandle().c.a(packet);
    }

    private String convertColorCodes(String text) {
        return text.replace('&', '§');
    }

    public static class HologramLine {
        private final EntityArmorStand armorStand;
        private final String rawText;
        private String currentText;

        public HologramLine(EntityArmorStand armorStand, String rawText) {
            this.armorStand = armorStand;
            this.rawText = rawText;
            this.currentText = "";
        }

        public EntityArmorStand getArmorStand() { return armorStand; }
        public String getRawText() { return rawText; }
        public String getCurrentText() { return currentText; }
        public void setCurrentText(String text) { this.currentText = text; }
    }
}