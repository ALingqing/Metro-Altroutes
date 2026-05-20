# 命令参考

metro-altroutes 所有命令均通过 `/m` 别名执行。

---

## 线路状态管理

| 命令 | 描述 |
| :--- | :--- |
| `/m line setstatus <线路ID> <状态>` | 设置线路运营状态 |

**状态值：**

| 值 | 效果 | 是否可上车 |
| :--- | :--- | :--- |
| `normal` | 正常运营 | ✅ 是 |
| `suspended` | 暂停运营 | ❌ 否 |
| `maintenance` | 维护中 | ✅ 是 |

---

## 暂停公告

| 命令 | 描述 |
| :--- | :--- |
| `/m line setsuspensionmsg <线路ID> <消息>` | 设置暂停公告文本 |

消息支持 `&` 颜色代码。

---

## 替代路线管理

| 命令 | 描述 |
| :--- | :--- |
| `/m line setaltroute <线路ID> <替代ID> [优先级]` | 添加替代路线（数字越小优先级越高） |
| `/m line clearaltroute <线路ID> [替代ID]` | 清除替代路线（不指定替代ID则清空全部） |

优先级为可选参数，默认为 `100`。替代路线按优先级升序推荐。

---

## 自动恢复

| 命令 | 描述 |
| :--- | :--- |
| `/m line setautoresume <线路ID> <分钟>` | 设置自动恢复倒计时，到期后自动恢复正常 |
| `/m line cancelautoresume <线路ID>` | 取消自动恢复设置 |

---

## 计划维护时段

| 命令 | 描述 |
| :--- | :--- |
| `/m line setschedule <线路ID> <开始>-<结束>` | 设置每天的计划维护时段（HH:mm-HH:mm） |
| `/m line clearschedule <线路ID>` | 取消计划维护时段 |

到达时段开始时间时自动暂停运营，结束时自动恢复。

---

## 查询与信息

| 命令 | 描述 |
| :--- | :--- |
| `/m line stats <线路ID>` | 查看线路运营统计数据 |
| `/m line status <线路ID>` | 查看线路当前运营状态 |
| `/m line info <线路ID>` | 查看线路详细快照信息 |
| `/m line list` | 列出服务器上所有线路 |

---

## 系统管理

| 命令 | 描述 |
| :--- | :--- |
| `/m reload` | 重新加载缓存和插件配置（需要 `metroaltroutes.admin`） |

---

## 权限

| 权限 | 作用 | 默认 |
| :--- | :--- | :--- |
| `metroaltroutes.admin` | 管理线路运营与公告 | OP |
| `metroaltroutes.use` | 查看线路状态与信息 | 所有人 |

---

## PlaceholderAPI 占位符

| 占位符 | 返回值 |
| :--- | :--- |
| `%metroaltroutes_line_status_<line>%` | 线路状态 |
| `%metroaltroutes_line_status_display_<line>%` | 带颜色的状态文本 |
| `%metroaltroutes_line_boardable_<line>%` | 是否可乘坐 |
| `%metroaltroutes_line_suspended_<line>%` | 是否暂停 |
| `%metroaltroutes_line_suspension_msg_<line>%` | 暂停公告 |
| `%metroaltroutes_line_altroutes_<line>%` | 替代路线列表 |
| `%metroaltroutes_line_altroute_count_<line>%` | 替代路线数量 |
| `%metroaltroutes_line_name_<line>%` | 线路名称 |
| `%metroaltroutes_line_terminus_<line>%` | 终点站 |
| `%metroaltroutes_line_stop_count_<line>%` | 站点数量 |
| `%metroaltroutes_line_owner_<line>%` | 拥有者 |
| `%metroaltroutes_line_color_<line>%` | 颜色 |
| `%metroaltroutes_line_stats_suspend_<line>%` | 暂停次数 |
| `%metroaltroutes_line_stats_intercept_<line>%` | 拦截人数 |
| `%metroaltroutes_line_stats_alt_recommend_<line>%` | 推荐替代次数 |
| `%metroaltroutes_line_autoresume_<line>%` | 自动恢复剩余分钟 |
| `%metroaltroutes_line_schedule_<line>%` | 计划维护时段 |
