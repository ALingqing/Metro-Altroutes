package top.chenray.metroaltroutes.cache;

import org.cubexmc.metro.api.MetroAPI;
import org.cubexmc.metro.model.LineStatus;
import top.chenray.metroaltroutes.MetroAltroutes;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 线路数据性能缓存
 *
 * <p>缓存线路状态、替代路线映射、停运公告等信息，
 * 避免频繁调用 MetroAPI 的快照查询，降低主线程开销。</p>
 *
 * <p>启动时加载一次，查询时自动按需刷新（TTL 30秒），
 * 也可通过 /m reload 手动触发刷新。</p>
 */
public final class RouteCache {

    private static final long CACHE_TTL_MS = TimeUnit.SECONDS.toMillis(30);

    private final MetroAltroutes plugin;
    private final MetroAPI api;

    // ---------- 缓存存储 ----------

    private final ConcurrentHashMap<String, CachedLine> lineCache = new ConcurrentHashMap<>();
    private volatile long lastRefresh = 0L;
    private volatile boolean running = true;

    // ---------- 替代路线索引（双向） ----------

    //  lineId -> 替代线路ID列表（已预计算的备用路线）
    private final ConcurrentHashMap<String, List<String>> altRouteForward = new ConcurrentHashMap<>();

    // 替代线路ID -> 哪些线路把它设为替代（反向索引，供 BoardingListener 使用）
    private final ConcurrentHashMap<String, List<String>> altRouteReverse = new ConcurrentHashMap<>();

    // ---------- 构造函数 ----------

    public RouteCache(MetroAltroutes plugin) {
        this.plugin = plugin;
        this.api = plugin.getMetroAPI();
    }

    // ==================== 缓存管理 ====================

    /**
     * 启动初始缓存加载（仅加载一次，不设定时后台刷新）
     */
    public void startCacheRefreshTask() {
        plugin.runTaskAsync(() -> {
            refreshAll();
            lastRefresh = System.currentTimeMillis();
        });
    }

    /**
     * 刷新全部缓存
     */
    public void refreshAll() {
        try {
            List<MetroAPI.LineSnapshot> snapshots = api.getLineSnapshots();
            for (MetroAPI.LineSnapshot snap : snapshots) {
                String lineId = snap.id();
                lineCache.put(lineId, new CachedLine(snap));
            }

            // 重建替代路线索引
            rebuildAltRouteIndex(snapshots);
        } catch (Exception e) {
            plugin.getLogger().warning("缓存刷新异常: " + e.getMessage());
        }
    }

    /**
     * 使指定线路的缓存失效
     */
    public void invalidate(String lineId) {
        lineCache.remove(lineId);
        altRouteForward.remove(lineId);
        // 反向索引中也清除该线路
        altRouteReverse.values().forEach(list -> list.remove(lineId));
        // 重建反向索引
        rebuildReverseIndex();
    }

    /**
     * 使全部缓存失效
     */
    public void invalidateAll() {
        lineCache.clear();
        altRouteForward.clear();
        altRouteReverse.clear();
        lastRefresh = 0L;
    }

    /**
     * 关闭缓存（插件卸载时）
     */
    public void shutdown() {
        running = false;
        invalidateAll();
    }

    // ==================== 查询方法 ====================

    /**
     * 获取缓存的线路状态
     */
    public LineStatus getStatus(String lineId) {
        ensureFresh();
        CachedLine cached = lineCache.get(lineId);
        return cached != null ? cached.status : api.getLineStatus(lineId);
    }

    /**
     * 获取缓存的停运公告
     */
    public String getSuspensionMessage(String lineId) {
        ensureFresh();
        CachedLine cached = lineCache.get(lineId);
        return cached != null ? cached.suspensionMessage : null;
    }

    /**
     * 获取线路是否暂停运营（缓存版）
     */
    public boolean isSuspended(String lineId) {
        return getStatus(lineId) == LineStatus.SUSPENDED;
    }

    /**
     * 获取线路是否维护中（缓存版）
     */
    public boolean isMaintenance(String lineId) {
        return getStatus(lineId) == LineStatus.MAINTENANCE;
    }

    /**
     * 获取线路是否可乘坐（缓存版）
     */
    public boolean isBoardable(String lineId) {
        return getStatus(lineId).isBoardable();
    }

    /**
     * 获取替代路线列表（缓存版）
     *
     * @param lineId 被暂停的线路ID
     * @return 推荐的替代线路ID列表，按名称排序
     */
    public List<String> getAlternativeRoutes(String lineId) {
        ensureFresh();
        return altRouteForward.getOrDefault(lineId, Collections.emptyList());
    }

    /**
     * 获取所有线路的摘要信息（供 /m line list 使用）
     */
    public List<LineSummary> getAllLineSummaries() {
        ensureFresh();
        return lineCache.values().stream()
                .map(c -> new LineSummary(c.snapshot.id(), c.snapshot.name(),
                        c.status, c.snapshot.terminusName()))
                .sorted(Comparator.comparing(LineSummary::id))
                .collect(Collectors.toList());
    }

    /**
     * 获取缓存的快照
     */
    public MetroAPI.LineSnapshot getSnapshot(String lineId) {
        ensureFresh();
        CachedLine cached = lineCache.get(lineId);
        return cached != null ? cached.snapshot : api.getLineSnapshot(lineId);
    }

    /**
     * 检查缓存是否需要刷新
     */
    private void ensureFresh() {
        if (System.currentTimeMillis() - lastRefresh > CACHE_TTL_MS) {
            // 异步刷新，不阻塞调用方
            plugin.runTaskAsync(this::refreshAll);
        }
    }

    // ==================== 内部工具 ====================

    /**
     * 从快照重建替代路线索引
     */
    private void rebuildAltRouteIndex(List<MetroAPI.LineSnapshot> snapshots) {
        altRouteForward.clear();

        for (MetroAPI.LineSnapshot snap : snapshots) {
            try {
                List<String> altIds = snap.alternativeRouteIds();
                if (altIds == null) altIds = Collections.emptyList();

                if (!altIds.isEmpty()) {
                    altRouteForward.put(snap.id(), altIds);
                }
            } catch (Exception ignored) {
                // 某些快照的 alternativeRouteIds 可能为 null，静默跳过
            }
        }

        rebuildReverseIndex();
    }

    /**
     * 重建反向索引（替代线路 -> 哪些线路依赖它）
     */
    private void rebuildReverseIndex() {
        altRouteReverse.clear();
        for (Map.Entry<String, List<String>> entry : altRouteForward.entrySet()) {
            String lineId = entry.getKey();
            for (String altId : entry.getValue()) {
                altRouteReverse.computeIfAbsent(altId, k -> Collections.synchronizedList(new ArrayList<>()))
                        .add(lineId);
            }
        }
    }

    // ==================== 内部类 ====================

    /**
     * 缓存的单条线路数据
     */
    private static final class CachedLine {
        final MetroAPI.LineSnapshot snapshot;
        final LineStatus status;
        final String suspensionMessage;

        CachedLine(MetroAPI.LineSnapshot snapshot) {
            this.snapshot = snapshot;
            this.status = snapshot.lineStatus();
            this.suspensionMessage = snapshot.suspensionMessage();
        }
    }

    /**
     * 线路摘要（供列表展示使用）
     */
    public record LineSummary(String id, String name, LineStatus status, String terminusName) {}
}
