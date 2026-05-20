# metro-altroutes — Metro 附属插件

![Minecraft](https://img.shields.io/badge/Minecraft-1.21-blue)
![Paper](https://img.shields.io/badge/Paper-1.21-brightgreen)
![Folia](https://img.shields.io/badge/Folia-%E2%9C%93-supported)

metro-altroutes 是一款面向高端 Minecraft 服务器的 Metro 扩展插件，专注于轨道交通运营管理、乘车行为控制和备用线路调度。它基于 Metro API 进行深度集成，提供稳定、可扩展且符合大型服务器运维需求的运营逻辑。

---

## 产品定位

metro-altroutes 的设计目标是为 Metro 生态提供一套可预测的线路运营控制方案，适用于商业服务器、城市交通网络和高并发玩家环境。通过对线路状态、乘车权限和暂停公告的集中控制，插件将复杂的轨道运营调度转化为易于管理的指令集。

---

## 关键能力

| 能力 | 说明 |
|------|------|
| 运营状态策略 | 支持正常、暂停、维护三种线路状态，并确保状态切换即时生效 |
| 自动乘车拦截 | 暂停线路自动阻止玩家上车，避免异常运营行为产生 |
| 统一公告系统 | 允许为暂停线路配置多语言提示和自定义提示文本 |
| 智能替代路线 | 为暂停线路推荐可用替代线路，提高玩家出行体验 |
| 缓存与性能 | 基于缓存策略优化状态读取，降低主线程占用并提升响应速度 |
| 运行时兼容 | 原生兼容 Paper 与 Folia，支持多线程和异步调度 |

---

## 适用场景

- 公共交通服务器：实现地铁与轻轨线路的精细化运营管理
- 城市模拟服务器：控制停运通知、备用路线和线路维护状态
- 运营型服务器：在高并发环境下保证暂停线路行为的稳定性
- Metro 生态集成：为 Metro 提供线路调度与用户提示扩展

---

## 依赖信息

- Paper 1.20.4 及以上
- Folia 运行时
- Metro 1.1.7 及以上版本

Metro 依赖源仓库：

https://github.com/CubeX-MC/Metro

插件使用的 Metro Maven 坐标：

`org.cubexmc:metro:[1.1.7,)`

注意：Metro 官方并未发布到 Maven Central，推荐通过源代码构建或使用现有 Metro.jar。

---

## 安装说明

1. 确保服务器已安装 Metro 插件。
2. 将 `metro-altroutes.jar` 放入 `plugins/` 目录。
3. 重启服务器或执行插件热加载。
4. 使用 `/m` 命令集进行线路管理与调度。

---

## 命令与操作

| 指令 | 描述 |
|------|------|
| `line setstatus <线路ID> <状态>` | 设置线路当前运营状态 |
| `line setsuspensionmsg <线路ID> <消息>` | 设置暂停线路公告文本 |
| `line setaltroute <线路ID> <替代ID>` | 为当前线路指定备用路线 |
| `line clearaltroute <线路ID>` | 清除当前线路的备用路线配置 |
| `line status <线路ID>` | 查询线路当前状态 |
| `line info <线路ID>` | 查看线路详细运营信息 |
| `line list` | 列出所有线路状态与配置 |
| `reload` | 重新加载缓存和插件配置 |

### 状态说明

| 状态 | 含义 | 是否允许上车 |
|------|------|------|
| `normal` | 正常运营 | 是 |
| `suspended` | 暂停运营 | 否 |
| `maintenance` | 维护中 | 是 |

---

## 权限说明

| 权限 | 作用 | 默认值 |
|------|------|------|
| `metroaltroutes.admin` | 管理线路运营与公告 | OP |
| `metroaltroutes.use` | 查看线路状态与信息 | 所有人 |

---

## 使用示例

```bash
# 将 1 号线路设置为暂停
/m line setstatus line1 suspended

# 为暂停线路设置通知文本
/m line setsuspensionmsg line1 "&c线路临时暂停，预计 2 小时后恢复。"

# 设置备用线路
/m line setaltroute line1 line2

# 查询线路状态
/m line status line1

# 列出所有线路状态
/m line list

# 将线路恢复为正常运营
/m line setstatus line1 normal

# 移除备用线路
/m line clearaltroute line1
```

---

## 项目架构

```
src/main/java/top/chenray/metroaltroutes/
├── MetroAltroutes.java       # 插件入口类，负责与 Metro API 的集成与调度
├── cache/
│   └── RouteCache.java       # 运营状态缓存模块，优化调用性能
├── commands/
│   └── LineCommand.java      # 命令解析与业务逻辑入口
└── listeners/
    └── BoardingListener.java # 乘车拦截与备用路线推荐逻辑
```

---

## 版本与维护

本项目面向 Metro 生态持续迭代，后续版本将进一步增强线路管理策略、公告配置和兼容性支持。如果希望定制化功能，请在仓库提交 issues 或 PR。
