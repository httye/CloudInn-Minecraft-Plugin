package cloudinn.websocket;

import cloudinn.CloudInnPlugin;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public class WebSocketClient {

    private final CloudInnPlugin plugin;
    private final String url;
    private WebSocket webSocket;
    private final Gson gson = new Gson();
    private boolean connected = false;
    private HttpClient httpClient;

    public WebSocketClient(CloudInnPlugin plugin, String url) {
        this.plugin = plugin;
        this.url = url;
        this.httpClient = HttpClient.newHttpClient();
    }

    public void connect() {
        try {
            httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(url), new WebSocketListener())
                .thenAccept(ws -> {
                    this.webSocket = ws;
                    plugin.getLogger().info("✦ WebSocket 已连接到 " + url);
                })
                .exceptionally(e -> {
                    plugin.getLogger().warning("✗ WebSocket 连接失败: " + e.getMessage());
                    scheduleReconnect();
                    return null;
                });
        } catch (Exception e) {
            plugin.getLogger().warning("✗ WebSocket 连接异常: " + e.getMessage());
            scheduleReconnect();
        }
    }

    public void disconnect() {
        connected = false;
        if (webSocket != null) {
            webSocket.sendClose(1000, "插件卸载");
            webSocket = null;
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void sendServerStatus(Map<String, Object> metrics) {
        if (!connected || webSocket == null) return;

        try {
            JsonObject message = new JsonObject();
            message.addProperty("type", "server_status");
            message.add("data", JsonParser.parseString(gson.toJson(metrics)));
            webSocket.sendText(gson.toJson(message), true);
        } catch (Exception e) {
            plugin.getLogger().warning("发送服务器状态失败: " + e.getMessage());
        }
    }

    public void sendMessage(String type, JsonObject data) {
        if (!connected || webSocket == null) return;

        try {
            JsonObject message = new JsonObject();
            message.addProperty("type", type);
            message.add("data", data);
            webSocket.sendText(gson.toJson(message), true);
        } catch (Exception e) {
            plugin.getLogger().warning("发送消息失败: " + e.getMessage());
        }
    }

    private void scheduleReconnect() {
        int interval = plugin.getConfig().getInt("websocket.reconnect-interval", 10);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getLogger().info("尝试重新连接 WebSocket...");
            connect();
        }, interval * 20L);
    }

    private class WebSocketListener implements WebSocket.Listener {

        @Override
        public void onOpen(WebSocket webSocket) {
            connected = true;
            plugin.getLogger().info("✓ WebSocket 已连接");

            // 发送身份验证
            JsonObject auth = new JsonObject();
            auth.addProperty("type", "auth");
            JsonObject data = new JsonObject();
            data.addProperty("server_id", plugin.getConfig().getInt("server-id", 1));
            data.addProperty("server_name", plugin.getConfig().getString("server-name", "云际服务器"));
            auth.add("data", data);
            webSocket.sendText(gson.toJson(auth), true);

            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            try {
                JsonObject message = JsonParser.parseString(data.toString()).getAsJsonObject();
                String type = message.get("type").getAsString();

                switch (type) {
                    case "command":
                        handleCommand(message.getAsJsonObject("data"));
                        break;
                    case "broadcast":
                        String content = message.get("data").getAsString();
                        plugin.getServer().broadcastMessage(content);
                        break;
                    case "update_hologram":
                        plugin.getHologramManager().updateAll();
                        break;
                    case "ping":
                        sendMessage("pong", new JsonObject());
                        break;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("处理WebSocket消息失败: " + e.getMessage());
            }

            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            connected = false;
            plugin.getLogger().warning("WebSocket 已断开 (" + statusCode + "): " + reason);
            scheduleReconnect();
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            connected = false;
            plugin.getLogger().warning("WebSocket 错误: " + error.getMessage());
            scheduleReconnect();
        }

        private void handleCommand(JsonObject data) {
            String command = data.get("command").getAsString();
            plugin.getLogger().info("收到远程命令: " + command);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getServer().dispatchCommand(
                    plugin.getServer().getConsoleSender(),
                    command
                );
            });
        }
    }
}