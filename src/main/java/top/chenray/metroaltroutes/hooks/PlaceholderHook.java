package top.chenray.metroaltroutes.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.chenray.metroaltroutes.MetroAltroutes;
import top.chenray.metroaltroutes.cache.RouteCache;
import top.chenray.metroaltroutes.data.LineDataManager;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PlaceholderAPI 扩展 — 将线路运营数据暴露为占位符
 *
 * <p>占位符列表（所有 <line> 替换为线路 ID）：</p>
 * <ul>
 *   <li><code>%metroaltroutes_line_status_&lt;line&gt;%</code> — 线路状态 (normal/suspended/maintenance)</li>
 *   <li><code>%metroaltroutes_line_status_display_&lt;line&gt;%</code> — 带颜色的状态文本</li>
 *   <li><code>%metroaltroutes_line_boardable_&lt;line&gt;%</code> — 是否可乘坐 (true/false)</li>
 *   <li><code>%metroaltroutes_line_suspended_&lt;line&gt;%</code> — 是否暂停 (yes/no)</li>
 *   <li><code>%metroaltroutes_line_suspension_msg_&lt;line&gt;%</code> — 暂停公告文本</li>
 *   <li><code>%metroaltroutes_line_altroutes_&lt;line&gt;%</code> — 替代路线列表 (逗号分隔)</li>
 *   <li><code>%metroaltroutes_line_altroute_count_&lt;line&gt;%</code> — 替代路线数量</li>
 *   <li><code>%metroaltroutes_line_name_&lt;line&gt;%</code> — 线路名称</li>
 *   <li><code>%metroaltroutes_line_terminus_&lt;line&gt;%</code> — 线路终点站</li>
 *   <li><code>%metroaltroutes_line_stop_count_&lt;line&gt;%</code> — 站点数量</li>
 *   <li><code>%metroaltroutes_line_stats_suspend_&lt;line&gt;%</code> — 暂停次数统计</li>
 *   <li><code>%metroaltroutes_line_stats_intercept_&lt;line&gt;%</code> — 拦截人数统计</li>
 *   <li><code>%metroaltroutes_line_stats_alt_recommend_&lt;line&gt;%</code> — 推荐替代路线次数</li>
 *   <li><code>%metroaltroutes_line_autoresume_&lt;line&gt;%</code> — 自动恢复剩余分钟数 (0 表示未设置)</li>
 *   <li><code>%metroaltroutes_line_schedule_&lt;line&gt;%</code> — 计划维护时段 (HH:mm-HH:mm, 空表示未设置)</li>
 *   <li><code>%metroaltroutes_line_owner_&lt;line&gt;%</code> — 线路拥有者</li>
 *   <li><code>%metroaltroutes_line_color_&lt;line&gt;%</code> — 线路颜色代码</li>
 * </ul>
 */
public final class PlaceholderHook extends PlaceholderExpansion {

    private final MetroAltroutes plugin;
    private final RouteCache cache;
    private final LineDataManager dataManager;

    public PlaceholderHook(MetroAltroutes plugin) {
        this.plugin = plugin;
        this.cache = plugin.getRouteCache();
        this.dataManager = plugin.getLineDataManager();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "metroaltroutes";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // 插件重载后仍保持注册
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (params.isEmpty()) return null;

        // 解析参数: <placeholder>_<lineId>
        int lastUnderscore = params.lastIndexOf('_');
        if (lastUnderscore < 0) return null;

        String placeholder = params.substring(0, lastUnderscore);
        String lineId = params.substring(lastUnderscore + 1);
        if (lineId.isEmpty()) return null;

        // 缓存和 API 的基础查询
        var api = plugin.getMetroAPI();
        var snapshot = cache.getSnapshot(lineId);
        if (snapshot == null && !placeholder.equals("stats_suspend")
                && !placeholder.equals("stats_intercept")
                && !placeholder.equals("stats_alt_recommend")
                && !placeholder.equals("autoresume")
                && !placeholder.equals("schedule")) {
            return "";
        }

        return switch (placeholder) {
            case "line_status" -> {
                var status = cache.getStatus(lineId);
                yield status != null ? status.name().toLowerCase() : "";
            }
            case "line_status_display" -> {
                var status = cache.getStatus(lineId);
                if (status == null) yield "";
                yield switch (status) {
                    case NORMAL -> "§a● 正常运营";
                    case SUSPENDED -> "§c● 暂停运营";
                    case MAINTENANCE -> "§e● 维护中";
                };
            }
            case "line_boardable" ->
                    String.valueOf(cache.isBoardable(lineId));
            case "line_suspended" ->
                    cache.isSuspended(lineId) ? "yes" : "no";
            case "line_suspension_msg" -> {
                String msg = cache.getSuspensionMessage(lineId);
                yield msg != null ? msg : "";
            }
            case "line_altroutes" -> {
                List<String> alts = dataManager.getAlternativeRoutes(lineId);
                yield alts.isEmpty() ? "" : String.join(", ", alts);
            }
            case "line_altroute_count" -> {
                List<String> alts = dataManager.getAlternativeRoutes(lineId);
                yield String.valueOf(alts.size());
            }
            case "line_name" ->
                    snapshot != null ? snapshot.name() : "";
            case "line_terminus" ->
                    snapshot != null ? snapshot.terminusName() : "";
            case "line_stop_count" ->
                    snapshot != null ? String.valueOf(snapshot.orderedStopIds().size()) : "0";
            case "line_owner" ->
                    snapshot != null && snapshot.owner() != null ? snapshot.owner().toString() : "";
            case "line_color" ->
                    snapshot != null ? snapshot.color() : "";
            case "stats_suspend" ->
                    String.valueOf(dataManager.getStats(lineId).suspendCount());
            case "stats_intercept" ->
                    String.valueOf(dataManager.getStats(lineId).interceptCount());
            case "stats_alt_recommend" ->
                    String.valueOf(dataManager.getStats(lineId).altRecommendCount());
            case "autoresume" -> {
                Integer left = dataManager.getAutoRecoverMinutesLeft(lineId);
                yield left != null ? String.valueOf(left) : "0";
            }
            case "schedule" -> {
                var entry = dataManager.getSchedule(lineId);
                if (entry == null) yield "";
                yield entry.getFormattedStart() + "-" + entry.getFormattedEnd();
            }
            default -> null;
        };
    }
}
