# Changelog

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
- 无

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
- None
