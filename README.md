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
| 智能替代路线 | 为暂停线路推荐可用替代线路，支持优先级排序 |
| 自动恢复调度 | 设置倒计时自动恢复正常运营，无需人工干预 |
| 计划维护时段 | 按设定时间自动暂停/恢复，适用于定期维护场景 |
| 运行统计 | 记录暂停次数、拦截人数和推荐替代路线次数 |
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
- PlaceholderAPI（可选，用于占位符支持）

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
| `line setaltroute <线路ID> <替代ID> [优先级]` | 为当前线路指定备用路线（可选优先级） |
| `line clearaltroute <线路ID> [替代ID]` | 清除备用路线（可指定 ID 单条删除） | 
| `line setautoresume <线路ID> <分钟>` | 设置自动恢复倒计时，到期自动恢复正常 |
| `line cancelautoresume <线路ID>` | 取消自动恢复设置 |
| `line setschedule <线路ID> <开始>-<结束>` | 设置计划维护时段（HH:mm-HH:mm） |
| `line clearschedule <线路ID>` | 取消计划维护时段 |
| `line stats <线路ID>` | 查看线路运行统计数据 |
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

## PlaceholderAPI 占位符

如果服务器安装了 PlaceholderAPI，可以使用以下占位符（将 `<line>` 替换为实际线路 ID）：

| 占位符 | 返回值 |
|--------|--------|
| `%metroaltroutes_line_status_<line>%` | 线路状态：`normal` / `suspended` / `maintenance` |
| `%metroaltroutes_line_status_display_<line>%` | 带颜色图标的运营状态文本 |
| `%metroaltroutes_line_boardable_<line>%` | 是否可乘坐：`true` / `false` |
| `%metroaltroutes_line_suspended_<line>%` | 是否暂停：`yes` / `no` |
| `%metroaltroutes_line_suspension_msg_<line>%` | 暂停公告文本 |
| `%metroaltroutes_line_altroutes_<line>%` | 替代路线列表（逗号分隔） |
| `%metroaltroutes_line_altroute_count_<line>%` | 替代路线数量 |
| `%metroaltroutes_line_name_<line>%` | 线路名称 |
| `%metroaltroutes_line_terminus_<line>%` | 线路终点站 |
| `%metroaltroutes_line_stop_count_<line>%` | 站点数量 |
| `%metroaltroutes_line_owner_<line>%` | 线路拥有者 |
| `%metroaltroutes_line_color_<line>%` | 线路颜色代码 |
| `%metroaltroutes_line_stats_suspend_<line>%` | 暂停次数统计 |
| `%metroaltroutes_line_stats_intercept_<line>%` | 拦截人数统计 |
| `%metroaltroutes_line_stats_alt_recommend_<line>%` | 推荐替代路线次数 |
| `%metroaltroutes_line_autoresume_<line>%` | 自动恢复剩余分钟数（0=未设置） |
| `%metroaltroutes_line_schedule_<line>%` | 计划维护时段（如 `02:00-04:00`） |

示例计分板配置：

```yaml
# 在计分板或插件中显示线路状态
%metroaltroutes_line_status_display_line1%
%metroaltroutes_line_altroutes_line1%
```

---

## 使用示例

```bash
# 将 1 号线路设置为暂停
/m line setstatus line1 suspended

# 为暂停线路设置通知文本
/m line setsuspensionmsg line1 "&c线路临时暂停，预计 2 小时后恢复。"

# 设置备用线路（指定优先级）
/m line setaltroute line1 line2 50

# 查询线路状态
/m line status line1

# 列出所有线路状态
/m line list

# 查询线路统计数据
/m line stats line1

# 设置自动恢复（30 分钟后自动恢复正常）
/m line setautoresume line1 30

# 设置计划维护时段（每天 02:00-04:00 自动暂停）
/m line setschedule line1 02:00-04:00

# 取消计划维护时段
/m line clearschedule line1

# 将线路恢复为正常运营
/m line setstatus line1 normal

# 移除某条备用线路
/m line clearaltroute line1 line2
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
├── data/
│   └── LineDataManager.java  # 数据管理层：替代路线优先级、自动恢复、计划调度、运行统计
├── hooks/
│   └── PlaceholderHook.java  # PlaceholderAPI 扩展，暴露线路数据为占位符
└── listeners/
    └── BoardingListener.java # 乘车拦截与备用路线推荐逻辑
```

---

## 版本与维护

本项目面向 Metro 生态持续迭代，后续版本将进一步增强线路管理策略、公告配置和兼容性支持。如果希望定制化功能，请在仓库提交 issues 或 PR。
