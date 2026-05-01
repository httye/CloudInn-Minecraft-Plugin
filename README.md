# 🧩 CloudInn — 云际驿站 Minecraft 插件

![Build Status](https://github.com/httye/CloudInn-Minecraft-Plugin/actions/workflows/build.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-17-brightgreen)
![Paper](https://img.shields.io/badge/Paper-1.20.x-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

> **云际驿站** Minecraft 服务端联动插件，实现与网站的双向数据同步。

---

## ✨ 功能特色

| 功能 | 说明 |
|------|------|
| 🏷️ **称号系统** | 玩家自由设置/切换称号，支持 list / set / remove |
| 📅 **签到积分** | 每日签到获取积分，提升玩家活跃度 |
| 🌀 **悬浮字联动** | 全息文字展示，每秒动态更新 |
| 📊 **服务器状态上报** | 实时上报在线人数、TPS 等指标到网站 |
| 🔌 **WebSocket 联动** | 与 CloudInn-Web 保持长连接，即时通信 |
| 👤 **玩家行为监听** | 记录玩家进出、在线时长等数据 |
| 🗄️ **数据库管理** | 集成 MySQL / SQLite，数据自动持久化 |

---

## 🔧 命令列表

| 命令 | 别名 | 说明 |
|------|------|------|
| `/cloudinn reload\|info` | `/ci`, `/inn` | 插件主命令：重载配置 / 查看信息 |
| `/title list\|set\|remove` | `/titles`, `/tag` | 称号管理 |
| `/checkin` | `/sign`, `/daily` | 每日签到 |
| `/points [player]` | `/point`, `/score`, `/coins` | 查看积分（可查看他人） |

## 🔐 权限节点

| 权限 | 说明 | 默认 |
|------|------|------|
| `cloudinn.admin` | 管理员权限 | OP |
| `cloudinn.title.admin` | 称号管理权限 | OP |
| `cloudinn.checkin.bypass` | 跳过签到限制 | OP |

---

## 📥 安装方式

### 方法一：从 Releases 下载（推荐）

1. 前往 [Releases](https://github.com/httye/CloudInn-Minecraft-Plugin/releases) 页面
2. 下载最新版本的 `CloudInn-1.0.0.jar`
3. 放入服务端 `plugins/` 目录
4. 重启服务器或 `/reload`

### 方法二：自行编译

```bash
# 克隆仓库
git clone https://github.com/httye/CloudInn-Minecraft-Plugin.git

# 进入目录
cd CloudInn-Minecraft-Plugin

# Maven 编译
mvn clean package

# 编译后的文件在 target/CloudInn-1.0.0.jar
```

### 方法三：GitHub Actions 下载

1. 打开仓库 → 点击 [Actions](https://github.com/httye/CloudInn-Minecraft-Plugin/actions) 标签
2. 选择最新的成功运行结果
3. 在底部 **Artifacts** 区域下载 `CloudInn-Plugin.zip`
4. 解压获取 `.jar` 文件

---

## ⚙️ 配置说明

编辑 `plugins/CloudInn/config.yml`：

```yaml
# MySQL 数据库配置
database:
  host: localhost
  port: 3306
  user: root
  password: ""
  name: cloudinn

# WebSocket 连接地址
websocket:
  url: "ws://localhost:3000/ws/server"

# 签到设置
checkin:
  daily_points: 10       # 每日签到积分
  streak_bonus: 5        # 连续签到额外奖励
```

---

## 🏗️ 技术栈

- **语言：** Java 17
- **框架：** Spigot / Paper API 1.20+
- **构建：** Maven
- **数据库：** MySQL / SQLite
- **通信：** WebSocket
- **持续集成：** GitHub Actions

---

## 🔗 关联项目

| 项目 | 仓库 | 说明 |
|------|------|------|
| 🌐 **CloudInn-Web** | [查看仓库](https://github.com/httye/CloudInn-Web) | 云际驿站网站前端（Express + EJS） |

> 💡 CloudInn 插件需搭配 CloudInn-Web 网站使用，二者通过 WebSocket 实时通信。

---

## 📄 许可证

本项目仅供学习交流使用。