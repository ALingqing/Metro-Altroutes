# metro-altroutes — Metro 附属插件

[![Metro](https://img.shields.io/badge/Metro-%E2%86%91%20GitHub-blue?style=flat-square)](https://github.com/CubeX-MC/Metro)

> **线路运营状态管理 · 暂停公告 · 替代路线推荐 · 乘车拦截**

---

## 关于 Metro

本插件是 [Metro](https://github.com/CubeX-MC/Metro) 的附属插件。Metro 是一个高性能 Minecraft 轨道交通插件，支持 Paper 1.20+ 与 Folia。

🔗 Metro GitHub：https://github.com/CubeX-MC/Metro

---

## 功能特性

| 功能 | 说明 |
|------|------|
| 🚦 **线路运营状态** | 将线路设为正常 / 暂停运营 / 维护中 |
| 🚫 **自动拦截乘车** | 暂停运营的线路自动阻止玩家上车 |
| 📢 **暂停公告** | 暂停时向尝试上车的玩家显示自定义提示 |
| 🔀 **替代路线推荐** | 暂停时自动推荐可替代的路线 |
| ⚡ **缓存优化** | 启动时加载 + 按需刷新（TTL 30 秒），降低主线程开销 |
| 🧵 **Folia 兼容** | 自动适配 Paper / Folia 运行时 |

---

## 依赖

- **Paper** 1.20.4+ 或 **Folia**
- **Metro** (https://github.com/CubeX-MC/Metro)

---

## 构建
```bash
mvn clean package
```

生成的插件位于 `target/metro-altroutes.jar`，放入服务器的 `plugins/` 目录。

---

## 命令

所有命令以 `/m` 为前缀。

| 命令 | 说明 |
|------|------|
| `line setstatus <线路ID> <状态>` | 设置线路运营状态 |
| `line setsuspensionmsg <线路ID> <消息>` | 设置暂停运营公告（支持 § 颜色代码） |
| `line setaltroute <线路ID> <替代ID>` | 设置替代路线 |
| `line clearaltroute <线路ID>` | 清除替代路线 |
| `line status <线路ID>` | 查看线路运营状态 |
| `line info <线路ID>` | 查看线路完整快照信息 |
| `line list` | 列出全部线路运营状态 |
| `reload` | 刷新缓存（热重载） |

### 状态值

| 值 | 效果 | 玩家可上车 |
|----|------|-----------|
| `normal` | 正常运营（默认） | ✅ 是 |
| `suspended` | 暂停运营 | ❌ 否（拦截并提示） |
| `maintenance` | 维护中 | ✅ 是 |

---

## 权限

| 权限 | 说明 | 默认 |
|------|------|------|
| `metroaltroutes.admin` | 管理线路运营状态 | OP |
| `metroaltroutes.use` | 查看线路信息 | 所有人 |

---

## 使用示例

```bash
# 暂停 1 号线运营
/m line setstatus line1 suspended

# 设置暂停公告
/m line setsuspensionmsg line1 "&c1号线正在进行设备检修，预计2小时后恢复。"

# 设置 2 号线为替代路线
/m line setaltroute line1 line2

# 查看线路状态
/m line status line1

# 列出所有线路
/m line list

# 恢复运营
/m line setstatus line1 normal

# 清除替代路线
/m line clearaltroute line1
```

---

## 工作流程

1. 管理员将线路设为 `suspended`
2. 后台缓存 30 秒内自动更新
3. 玩家尝试乘坐该线路时，**自动拦截**
4. 被拦截的玩家看到：
   - 📢 暂停公告（如已设置）
   - 🔀 替代路线推荐（如已设置）
5. 管理员恢复为 `normal` 后，乘车恢复正常

---

## 项目结构

```
src/main/java/top/chenray/metroaltroutes/
├── MetroAltroutes.java       # 主插件类（连接 API、调度任务）
├── cache/
│   └── RouteCache.java       # 线路数据缓存（性能优化）
├── commands/
│   └── LineCommand.java      # /m line 命令执行器
└── listeners/
    └── BoardingListener.java # 乘车拦截与替代路线推荐
```
