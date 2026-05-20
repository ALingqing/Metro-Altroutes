# metro-altroutes 开发者 API

metro-altroutes 目前不提供对外公开的 Java API。第三方插件可以通过以下方式集成：

---

## PlaceholderAPI 占位符

如果服务器安装了 PlaceholderAPI，请使用 `metroaltroutes` 扩展标识符。

详情见 [README PlaceholderAPI 节](../README.md#placeholderapi-占位符)。

---

## MetroAPI 集成

metro-altroutes 完全基于 Metro 的公开 API (`org.cubexmc.metro.api.MetroAPI`) 构建。所有 metro-altroutes 管理的线路操作最终通过 MetroAPI 执行。

---

## 内部 API（不保证稳定）

以下类对外暴露，但不保证跨版本兼容。如果其他插件需要集成，建议通过 MetroAPI + PlaceholderAPI 间接交互。

### `MetroAltroutes` 入口

```java
// 获取插件实例
MetroAltroutes plugin = MetroAltroutes.getInstance();

// 获取 MetroAPI
MetroAPI api = plugin.getMetroAPI();

// 获取缓存
RouteCache cache = plugin.getRouteCache();

// 获取数据管理器
LineDataManager dataManager = plugin.getLineDataManager();
```

### `RouteCache` 公开方法

```java
LineStatus getStatus(String lineId);
String getSuspensionMessage(String lineId);
boolean isSuspended(String lineId);
boolean isMaintenance(String lineId);
boolean isBoardable(String lineId);
List<String> getAlternativeRoutes(String lineId);      // 旧缓存索引
MetroAPI.LineSnapshot getSnapshot(String lineId);
List<LineSummary> getAllLineSummaries();
void invalidate(String lineId);
void invalidateAll();
void refreshAll();
```

### `LineDataManager` 公开方法

```java
List<String> getAlternativeRoutes(String lineId);       // 优先级排序
void setAlternativeRoute(String lineId, String altId, int priority);
boolean removeAlternativeRoute(String lineId, String altId);
boolean clearAlternativeRoutes(String lineId);
void setAutoRecover(String lineId, int minutes);
boolean cancelAutoRecover(String lineId);
Integer getAutoRecoverMinutesLeft(String lineId);
void setSchedule(String lineId, String start, String end);
boolean clearSchedule(String lineId);
ScheduleEntry getSchedule(String lineId);
LineStats getStats(String lineId);
void recordSuspend(String lineId);
void recordIntercept(String lineId);
void recordAltRecommend(String lineId);
void reload();
```

### `LineStats` 记录

```java
int suspendCount();          // 暂停次数
int interceptCount();        // 拦截人数
int altRecommendCount();     // 推荐替代路线次数
```

### `ScheduleEntry` 记录

```java
boolean isWithinWindow(int currentMinute);
String getFormattedStart();  // HH:mm
String getFormattedEnd();    // HH:mm
```

---

## 集成示例

### 检查线路是否可乘坐

```java
MetroAltroutes plugin = MetroAltroutes.getInstance();
RouteCache cache = plugin.getRouteCache();

if (!cache.isBoardable("line1")) {
    // 线路暂停或维护
}
```

### 获取线路统计数据

```java
LineDataManager dm = MetroAltroutes.getInstance().getLineDataManager();
LineDataManager.LineStats stats = dm.getStats("line1");

int suspended = stats.suspendCount();
int intercepted = stats.interceptCount();
```

### 获取替代路线（按优先级排序）

```java
List<String> alts = dm.getAlternativeRoutes("line1");
// 返回 ["line2", "line3"] 按优先级升序
```

---

## 稳定性说明

| API 级别 | 稳定性 | 说明 |
|----------|--------|------|
| PlaceholderAPI 占位符 | ✅ 稳定 | 语义化版本内保证兼容 |
| `MetroAltroutes.getInstance()` | ⚠️ 实验性 | 可能随插件重构改变 |
| `LineDataManager` 方法 | ⚠️ 实验性 | 方法签名可能调整 |
| `RouteCache` 方法 | ⚠️ 实验性 | 缓存策略可能调整 |
