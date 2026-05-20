# Changelog

## [1.2.0] - 2026-05-20

### 🇨🇳 中文

#### 新增
- **`setautoresume` / `cancelautoresume` 命令**：支持为线路设置自动恢复倒计时，到达时间后自动恢复正常运营
- **`setschedule` / `clearschedule` 命令**：支持设置计划维护时段，到点自动暂停、到点自动恢复
- **`stats` 命令**：查看线路运行统计数据（暂停次数、拦截人数、推荐替代路线次数）
- **`setaltroute` 优先级支持**：可为替代路线指定优先级，按优先级排序推荐
- **`clearaltroute [替代ID]` 单条删除**：支持按 ID 单独删除某条替代路线，而不再只能清空全部
- **`LineDataManager`**：新增数据管理层，统一管理替代路线优先级、自动恢复任务、计划调度、运行统计
- **定时调度任务**：每 60 秒检查自动恢复到期和计划维护时段切换

#### 优化
- **替代路线查询**：从 `RouteCache` 迁移至 `LineDataManager`，支持按优先级排序
- **权限检查增强**：所有新命令均执行 `canManageLine` 权限验证
- **服务端公告推送**：自动恢复和计划维护切换时向管理员推送通知

### 🇬🇧 English

#### Added
- **`setautoresume` / `cancelautoresume` commands**: Set auto-recovery countdown for lines. Lines automatically resume normal operation when the timer expires
- **`setschedule` / `clearschedule` commands**: Schedule maintenance windows. Lines auto-suspend at window start and auto-resume at window end
- **`stats` command**: View line operation statistics (suspend count, intercept count, alt-route recommendation count)
- **Priority support for `setaltroute`**: Assign a priority to each alternate route; suggestions are sorted by priority
- **`clearaltroute [altId]` single removal**: Remove a specific alternate route by ID instead of clearing all
- **`LineDataManager`**: New data management layer for alt-route priorities, auto-recovery, schedules, and statistics
- **Periodic scheduler**: Checks auto-recovery expiry and maintenance window transitions every 60 seconds

#### Improved
- **Alt-route queries**: Migrated from `RouteCache` to `LineDataManager` with priority-based sorting
- **Permission checks**: All new commands enforce `canManageLine` authorization
- **Admin notifications**: Push server notices when auto-recovery triggers or maintenance windows switch

---

## [1.1.0] - 2026-05-17

### 🇬🇧 English

#### Improved
- **Upgraded Actions to v6/v5 with Node.js 24 support**
- **Fixed CI dependency graph 403 error**

### 🇨🇳 中文

#### 新增
- `/m reload` 命令：支持手动刷新缓存（需要 `metroaltroutes.admin` 权限）
- 双语 CHANGELOG 文件

#### 优化
- **移除后台定时缓存刷新**：不再每 30 秒自动刷新缓存，改为启动时加载一次 + 查询时按需刷新（缓存 TTL 30 秒），减少不必要的 API 调用与日志输出
- **降低日志级别**：缓存刷新完成提示从 INFO 降为 FINE，避免控制台刷屏
- **状态显示优化**：线路列表和状态查询现在同时显示状态名称标签（如 `[NORMAL]`），便于机器解析和国际化

#### 修复
- 修复 `setstatus` 调用 Metro 内部广播消息中 `{status}` 占位符未替换的问题

---

### 🇬🇧 English

#### Added
- `/m reload` command: manually refresh cache (requires `metroaltroutes.admin` permission)
- Bilingual CHANGELOG file

#### Improved
- **Removed periodic background cache refresh**: Cache no longer auto-refreshes every 30 seconds. Instead, it loads once on startup and refreshes on demand when queried (TTL 30s), reducing unnecessary API calls and log spam
- **Reduced log verbosity**: Cache refresh message changed from INFO to FINE level to reduce console noise
- **Status display enhancement**: Line list and status queries now show a status name tag (e.g. `[NORMAL]`) alongside the coloured display, aiding machine parsing and internationalisation

#### Fixed
- Fixed `{status}` placeholder not being substituted in Metro's internal broadcast when using `setstatus`
