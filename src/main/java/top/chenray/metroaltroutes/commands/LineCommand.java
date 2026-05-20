package top.chenray.metroaltroutes.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.cubexmc.metro.api.MetroAPI;
import org.cubexmc.metro.model.Line;
import org.cubexmc.metro.model.LineStatus;
import org.jetbrains.annotations.NotNull;
import top.chenray.metroaltroutes.MetroAltroutes;
import top.chenray.metroaltroutes.cache.RouteCache;
import top.chenray.metroaltroutes.data.LineDataManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * /m 命令执行器
 *
 * <p>管理线路运营状态（正常/暂停/维护）、暂停公告、替代路线等。</p>
 */
public final class LineCommand implements CommandExecutor, TabCompleter {

    private final MetroAltroutes plugin;
    private final MetroAPI api;
    private final RouteCache cache;
    private final LineDataManager lineDataManager;

    private static final List<String> SUBCOMMANDS = List.of(
            "setstatus", "setsuspensionmsg",
            "setaltroute", "clearaltroute",
            "setautoresume", "cancelautoresume",
            "setschedule", "clearschedule",
            "stats", "status", "info", "list"
    );

    private static final List<String> STATUS_VALUES = List.of(
            "normal", "suspended", "maintenance"
    );

    public LineCommand(MetroAltroutes plugin) {
        this.plugin = plugin;
        this.api = plugin.getMetroAPI();
        this.cache = plugin.getRouteCache();
        this.lineDataManager = plugin.getLineDataManager();
    }

    // ==================== CommandExecutor ====================

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        // /m reload 顶层命令
        if (args[0].equalsIgnoreCase("reload")) {
            return handleReload(sender);
        }

        // 必须是 /m line ...
        if (!args[0].equalsIgnoreCase("line")) {
            sendUsage(sender, label);
            return true;
        }

        if (args.length < 2) {
            sendLineUsage(sender);
            return true;
        }

        String sub = args[1].toLowerCase();

        switch (sub) {
            case "setstatus":
                return handleSetStatus(sender, args);
            case "setsuspensionmsg":
                return handleSetSuspensionMsg(sender, args);
            case "setaltroute":
                return handleSetAltRoute(sender, args);
            case "clearaltroute":
                return handleClearAltRoute(sender, args);
            case "setautoresume":
                return handleSetAutoResume(sender, args);
            case "cancelautoresume":
                return handleCancelAutoResume(sender, args);
            case "setschedule":
                return handleSetSchedule(sender, args);
            case "clearschedule":
                return handleClearSchedule(sender, args);
            case "stats":
                return handleStats(sender, args);
            case "status":
                return handleStatus(sender, args);
            case "info":
                return handleInfo(sender, args);
            case "list":
                return handleList(sender);
            default:
                sendLineUsage(sender);
                return true;
        }
    }

    // ==================== 子命令 ====================

    // ---- setstatus ----

    /**
     * /m line setstatus <lineId> <normal|suspended|maintenance>
     */
    private boolean handleSetStatus(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sendMsg(sender, "&c用法: /m line setstatus <线路ID> <normal|suspended|maintenance>");
            return true;
        }

        String lineId = args[2];
        String statusStr = args[3].toUpperCase();

        if (!api.canManageLine(sender, lineId)) {
            sendMsg(sender, "&c你没有权限管理线路 &6" + lineId + "&c。");
            return true;
        }

        if (api.getLine(lineId) == null) {
            sendMsg(sender, "&c线路 &6" + lineId + " &c不存在。");
            return true;
        }

        LineStatus status;
        try {
            status = LineStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            sendMsg(sender, "&c无效状态值。可选: normal, suspended, maintenance");
            return true;
        }

        try {
            Line line = api.getLineManager().getLine(lineId);
            if (line == null) {
                sendMsg(sender, "&c无法获取线路内部对象。");
                return true;
            }

            // 直接设置 Line 对象的状态，绕过 api.setLineStatus()
            // 避免 Metro 内部广播消息中 {status} 占位符未替换的 bug
            line.setLineStatus(status);
            api.getLineManager().saveConfig();

            cache.invalidate(lineId); // 使缓存失效，下次查询重新加载

            String displayStatus = switch (status) {
                case NORMAL -> "&a● 正常运营";
                case SUSPENDED -> "&c● 暂停运营";
                case MAINTENANCE -> "&e● 维护中";
            };
            sendMsg(sender, "&a线路 &6" + lineId + " &a状态已更新为: " + displayStatus);

            // 如果是暂停运营，自动提示管理员设置公告和替代路线
            if (status == LineStatus.SUSPENDED) {
                sendMsg(sender, "");
                sendMsg(sender, "&e▸ 建议设置暂停公告: &7/m line setsuspensionmsg " + lineId + " <消息>");
                sendMsg(sender, "&e▸ 建议设置替代路线: &7/m line setaltroute " + lineId + " <替代线路ID>");
            }

            plugin.log(sender.getName() + " 将线路 " + lineId + " 状态设置为 " + status);
        } catch (Exception e) {
            sendMsg(sender, "&c设置线路状态时出错: " + e.getMessage());
            plugin.getLogger().warning("设置线路状态异常: " + e.getMessage());
        }

        return true;
    }

    // ---- setsuspensionmsg ----

    /**
     * /m line setsuspensionmsg <lineId> <消息...>
     */
    private boolean handleSetSuspensionMsg(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sendMsg(sender, "&c用法: /m line setsuspensionmsg <线路ID> <公告消息>");
            return true;
        }

        String lineId = args[2];

        if (!api.canManageLine(sender, lineId)) {
            sendMsg(sender, "&c你没有权限管理线路 &6" + lineId + "&c。");
            return true;
        }

        if (api.getLine(lineId) == null) {
            sendMsg(sender, "&c线路 &6" + lineId + " &c不存在。");
            return true;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        if (message.isEmpty()) {
            sendMsg(sender, "&c请输入暂停公告消息。");
            return true;
        }

        api.setSuspensionMessage(lineId, message);
        cache.invalidate(lineId);

        sendMsg(sender, "&a线路 &6" + lineId + " &a的暂停公告已设置:");
        sendMsg(sender, "§7  " + message.replace('&', '§'));
        plugin.log(sender.getName() + " 设置线路 " + lineId + " 暂停公告: " + message);

        return true;
    }

    // ---- setaltroute ----

    /**
     * /m line setaltroute <lineId> <替代线路ID>
     *
     * <p>通过 LineManager 高级 API 设置替代路线。</p>
     */
    private boolean handleSetAltRoute(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sendMsg(sender, "&c用法: /m line setaltroute <线路ID> <替代线路ID> [优先级]");
            return true;
        }

        String lineId = args[2];
        String altId = args[3];
        int priority = 100;
        if (args.length >= 5) {
            try {
                priority = Integer.parseInt(args[4]);
            } catch (NumberFormatException ignore) {
                priority = 100;
            }
        }

        if (!api.canManageLine(sender, lineId)) {
            sendMsg(sender, "&c你没有权限管理线路 &6" + lineId + "&c。");
            return true;
        }

        if (api.getLine(lineId) == null) {
            sendMsg(sender, "&c线路 &6" + lineId + " &c不存在。");
            return true;
        }
        if (api.getLine(altId) == null) {
            sendMsg(sender, "&c替代线路 &6" + altId + " &c不存在。");
            return true;
        }
        if (lineId.equalsIgnoreCase(altId)) {
            sendMsg(sender, "&c不能将线路设为自己的替代路线。");
            return true;
        }

        // 读取当前线路对象，通过 live 对象添加替代线路
        try {
            Line line = api.getLineManager().getLine(lineId);
            if (line == null) {
                sendMsg(sender, "&c无法获取线路内部对象。");
                return true;
            }

            Line altLine = api.getLineManager().getLine(altId);
            if (altLine == null) {
                sendMsg(sender, "&c无法获取替代线路内部对象。");
                return true;
            }

            // 使用 Line 的 API 添加替代路线
            line.addAlternativeRoute(altId);
            api.getLineManager().saveConfig();
            cache.invalidate(lineId);
            lineDataManager.setAlternativeRoute(lineId, altId, priority);

            sendMsg(sender, "&a已将 &6" + altId + " &a设为线路 &6" + lineId + " &a的替代路线，优先级 " + priority + "。");
            plugin.log(sender.getName() + " 设置线路 " + lineId + " 替代路线: " + altId + " (priority=" + priority + ")");

        } catch (Exception e) {
            sendMsg(sender, "&c设置替代路线时出错: " + e.getMessage());
            plugin.getLogger().warning("设置替代路线异常: " + e.getMessage());
        }

        return true;
    }

    // ---- clearaltroute ----

    /**
     * /m line clearaltroute <lineId> [altLineId]
     */
    private boolean handleClearAltRoute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMsg(sender, "&c用法: /m line clearaltroute <线路ID> [替代线路ID]");
            return true;
        }

        String lineId = args[2];
        String altId = args.length >= 4 ? args[3] : null;

        if (!api.canManageLine(sender, lineId)) {
            sendMsg(sender, "&c你没有权限管理线路 &6" + lineId + "&c。");
            return true;
        }

        if (api.getLine(lineId) == null) {
            sendMsg(sender, "&c线路 &6" + lineId + " &c不存在。");
            return true;
        }

        if (altId == null) {
            try {
                Line line = api.getLineManager().getLine(lineId);
                if (line == null) {
                    sendMsg(sender, "&c无法获取线路内部对象。");
                    return true;
                }

                line.getAlternativeRouteIds().clear();
                api.getLineManager().saveConfig();
                cache.invalidate(lineId);
                lineDataManager.clearAlternativeRoutes(lineId);
                sendMsg(sender, "&a已清除线路 &6" + lineId + " &a的所有替代路线。");
                plugin.log(sender.getName() + " 清除了线路 " + lineId + " 的全部替代路线。");
            } catch (Exception e) {
                sendMsg(sender, "&c清除替代路线时出错: " + e.getMessage());
            }
            return true;
        }

        if (!lineDataManager.removeAlternativeRoute(lineId, altId)) {
            sendMsg(sender, "&c线路 &6" + lineId + " &c未配置替代路线 &6" + altId + "&c。请确认替代线路是否存在。");
            return true;
        }

        cache.invalidate(lineId);
        sendMsg(sender, "&a已删除线路 &6" + lineId + " &a的替代路线 &6" + altId + "&a。");
        plugin.log(sender.getName() + " 删除了线路 " + lineId + " 的替代路线 " + altId + "。");
        return true;
    }

    // ---- setautoresume ----

    private boolean handleSetAutoResume(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sendMsg(sender, "&c用法: /m line setautoresume <线路ID> <分钟>");
            return true;
        }

        String lineId = args[2];
        int minutes;
        try {
            minutes = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sendMsg(sender, "&c请提供有效的时间（分钟）。");
            return true;
        }

        if (!api.canManageLine(sender, lineId)) {
            sendMsg(sender, "&c你没有权限管理线路 &6" + lineId + "&c。");
            return true;
        }
        if (api.getLine(lineId) == null) {
            sendMsg(sender, "&c线路 &6" + lineId + " &c不存在。");
            return true;
        }

        lineDataManager.setAutoRecover(lineId, minutes);
        sendMsg(sender, "&a已为线路 &6" + lineId + " &a设置自动恢复，" + minutes + " 分钟后恢复正常。");
        plugin.log(sender.getName() + " 为线路 " + lineId + " 设置自动恢复 " + minutes + " 分钟。");
        return true;
    }

    private boolean handleCancelAutoResume(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMsg(sender, "&c用法: /m line cancelautoresume <线路ID>");
            return true;
        }

        String lineId = args[2];
        if (!api.canManageLine(sender, lineId)) {
            sendMsg(sender, "&c你没有权限管理线路 &6" + lineId + "&c。");
            return true;
        }
        if (api.getLine(lineId) == null) {
            sendMsg(sender, "&c线路 &6" + lineId + " &c不存在。");
            return true;
        }
        if (!lineDataManager.cancelAutoRecover(lineId)) {
            sendMsg(sender, "&c线路 &6" + lineId + " &c没有设置自动恢复。");
            return true;
        }

        sendMsg(sender, "&a已取消线路 &6" + lineId + " &a的自动恢复设置。");
        plugin.log(sender.getName() + " 取消了线路 " + lineId + " 的自动恢复。");
        return true;
    }

    private boolean handleSetSchedule(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sendMsg(sender, "&c用法: /m line setschedule <线路ID> <开始时间>-<结束时间> (HH:mm)");
            return true;
        }

        String lineId = args[2];
        String period = args[3];
        if (!period.contains("-")) {
            sendMsg(sender, "&c时间格式错误，请使用 HH:mm-HH:mm。");
            return true;
        }

        String[] parts = period.split("-", 2);
        String start = parts[0];
        String end = parts[1];

        if (!api.canManageLine(sender, lineId)) {
            sendMsg(sender, "&c你没有权限管理线路 &6" + lineId + "&c。");
            return true;
        }
        if (api.getLine(lineId) == null) {
            sendMsg(sender, "&c线路 &6" + lineId + " &c不存在。");
            return true;
        }

        lineDataManager.setSchedule(lineId, start, end);
        sendMsg(sender, "&a已为线路 &6" + lineId + " &a设置计划维护时段: " + start + " - " + end + "。");
        plugin.log(sender.getName() + " 设置线路 " + lineId + " 的维护时段 " + start + " - " + end + "。");
        return true;
    }

    private boolean handleClearSchedule(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMsg(sender, "&c用法: /m line clearschedule <线路ID>");
            return true;
        }

        String lineId = args[2];
        if (!api.canManageLine(sender, lineId)) {
            sendMsg(sender, "&c你没有权限管理线路 &6" + lineId + "&c。");
            return true;
        }
        if (api.getLine(lineId) == null) {
            sendMsg(sender, "&c线路 &6" + lineId + " &c不存在。");
            return true;
        }
        if (!lineDataManager.clearSchedule(lineId)) {
            sendMsg(sender, "&c线路 &6" + lineId + " &c没有设置计划维护时段。");
            return true;
        }

        sendMsg(sender, "&a已取消线路 &6" + lineId + " &a的计划维护时段。");
        plugin.log(sender.getName() + " 取消了线路 " + lineId + " 的计划维护时段。");
        return true;
    }

    private boolean handleStats(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMsg(sender, "&c用法: /m line stats <线路ID>");
            return true;
        }

        String lineId = args[2];
        if (api.getLine(lineId) == null) {
            sendMsg(sender, "&c线路 &6" + lineId + " &c不存在。");
            return true;
        }

        LineDataManager.LineStats stats = lineDataManager.getStats(lineId);
        sendMsg(sender, "&6===== &e线路 " + lineId + " 运行统计 &6=====");
        sendMsg(sender, "&7暂停次数: &f" + stats.suspendCount());
        sendMsg(sender, "&7拦截人数: &f" + stats.interceptCount());
        sendMsg(sender, "&7推荐替代路线次数: &f" + stats.altRecommendCount());

        LineDataManager.ScheduleEntry schedule = lineDataManager.getSchedule(lineId);
        if (schedule != null) {
            sendMsg(sender, "&7计划维护时段: &f" + schedule.getFormattedStart() + " - "
                    + schedule.getFormattedEnd());
        }

        Integer minutesLeft = lineDataManager.getAutoRecoverMinutesLeft(lineId);
        if (minutesLeft != null) {
            sendMsg(sender, "&7自动恢复剩余: &f" + minutesLeft + " 分钟");
        }

        return true;
    }

    // ---- status ----

    /**
     * /m line status <lineId>
     */
    private boolean handleStatus(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMsg(sender, "&c用法: /m line status <线路ID>");
            return true;
        }

        String lineId = args[2];
        if (api.getLine(lineId) == null) {
            sendMsg(sender, "&c线路 &6" + lineId + " &c不存在。");
            return true;
        }

        // 优先使用缓存
        LineStatus status = cache.getStatus(lineId);
        boolean boardable = cache.isBoardable(lineId);
        boolean suspended = cache.isSuspended(lineId);
        boolean maintenance = cache.isMaintenance(lineId);
        String suspensionMsg = cache.getSuspensionMessage(lineId);
        List<String> altRoutes = lineDataManager.getAlternativeRoutes(lineId);

        sendMsg(sender, "&6===== &e线路 &f" + lineId + " &e运营状态 &6=====");
        sendMsg(sender, "&7状态: " + getStatusDisplay(status));
        sendMsg(sender, "&7可乘坐: " + (boardable ? "&a是" : "&c否"));

        if (suspensionMsg != null && !suspensionMsg.isEmpty()) {
            sendMsg(sender, "&7公告: §7" + suspensionMsg.replace('&', '§'));
        }

        if (altRoutes != null && !altRoutes.isEmpty()) {
            sendMsg(sender, "&7替代路线: &b" + String.join("&7, &b", altRoutes));
        }

        // 附加线路快照信息
        MetroAPI.LineSnapshot snapshot = cache.getSnapshot(lineId);
        if (snapshot != null) {
            sendMsg(sender, "&7名称: &f" + snapshot.name());
            sendMsg(sender, "&7终点站: &f" + snapshot.terminusName());
            sendMsg(sender, "&7站点数: &f" + snapshot.orderedStopIds().size());
        }

        return true;
    }

    // ---- info ----

    /**
     * /m line info <lineId>
     */
    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMsg(sender, "&c用法: /m line info <线路ID>");
            return true;
        }

        String lineId = args[2];
        MetroAPI.LineSnapshot snap = cache.getSnapshot(lineId);

        if (snap == null) {
            sendMsg(sender, "&c线路 &6" + lineId + " &c不存在。");
            return true;
        }

        sendMsg(sender, "&6========== &e线路详细快照 &6==========");
        sendMsg(sender, "&7ID: &f" + snap.id());
        sendMsg(sender, "&7名称: &f" + snap.name());
        sendMsg(sender, "&7状态: " + getStatusDisplay(snap.lineStatus()));
        sendMsg(sender, "&7终点站: &f" + snap.terminusName());
        sendMsg(sender, "&7颜色: &f" + snap.color());
        sendMsg(sender, "&7最高速度: &f" + snap.maxSpeed());
        sendMsg(sender, "&7票价: &f" + snap.ticketPrice());
        sendMsg(sender, "&7世界: &f" + snap.worldName());
        sendMsg(sender, "&7拥有者: &f" + (snap.owner() != null ? snap.owner().toString() : "&7(服务器)"));
        sendMsg(sender, "&7铁轨保护: " + (snap.railProtected() ? "&a开启" : "&c关闭"));

        List<String> stops = snap.orderedStopIds();
        sendMsg(sender, "&7站点 (" + stops.size() + "): &f" + String.join("&7, &f", stops));

        List<String> portals = snap.portalIds();
        if (portals != null && !portals.isEmpty()) {
            sendMsg(sender, "&7传送门数: &f" + portals.size());
        }

        if (snap.admins() != null && !snap.admins().isEmpty()) {
            sendMsg(sender, "&7管理员: &f" + snap.admins().size() + " 人");
        }

        // 暂停公告
        String msg = cache.getSuspensionMessage(lineId);
        if (msg != null && !msg.isEmpty()) {
            sendMsg(sender, "&7公告: §7" + msg.replace('&', '§'));
        }

        // 替代路线
        List<String> alts = lineDataManager.getAlternativeRoutes(lineId);
        if (alts != null && !alts.isEmpty()) {
            sendMsg(sender, "&7替代路线: &b" + String.join("&7, &b", alts));
        }

        return true;
    }

    // ---- list ----

    /**
     * /m line list
     */
    private boolean handleList(CommandSender sender) {
        List<RouteCache.LineSummary> summaries = cache.getAllLineSummaries();

        if (summaries.isEmpty()) {
            sendMsg(sender, "&7当前没有加载任何线路。");
            return true;
        }

        sendMsg(sender, "&6========== &e全部线路运营状态 &6==========");
        for (RouteCache.LineSummary s : summaries) {
            String statusStr = getStatusDisplay(s.status());
            sendMsg(sender, "  " + statusStr + " &f" + s.name()
                    + " &7(" + s.id() + ") &7→ " + s.terminusName());
        }
        sendMsg(sender, "&7总计: &f" + summaries.size() + " &7条线路");

        return true;
    }

    // ---- reload ----

    /**
     * /m reload
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("metroaltroutes.admin")) {
            sendMsg(sender, "&c你没有权限执行此操作。");
            return true;
        }

        sendMsg(sender, "&7正在刷新缓存...");
        cache.invalidateAll();
        plugin.runTaskAsync(() -> {
            cache.refreshAll();
            sendMsg(sender, "&a缓存刷新完成。");
        });

        return true;
    }

    // ==================== TabCompleter ====================

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String label,
                                      @NotNull String[] args) {

        if (args.length == 1) {
            // 第一层: "line" 或 "reload"
            if ("reload".startsWith(args[0].toLowerCase())) {
                return List.of("reload");
            }
            return List.of("line");
        }

        if (args.length == 2) {
            // 第二层: 子命令
            return filterStartsWith(SUBCOMMANDS, args[1]);
        }

        if (args.length == 3) {
            // 第三层: 线路ID
            String sub = args[1].toLowerCase();
            if (SUBCOMMANDS.contains(sub)) {
                return filterStartsWith(getLineIds(), args[2]);
            }
            return Collections.emptyList();
        }

        if (args.length == 4) {
            String sub = args[1].toLowerCase();
            switch (sub) {
                case "setstatus":
                    return filterStartsWith(STATUS_VALUES, args[3]);
                case "setaltroute":
                    String current = args[2];
                    return filterStartsWith(
                            getLineIds().stream()
                                    .filter(id -> !id.equalsIgnoreCase(current))
                                    .collect(Collectors.toList()),
                            args[3]);
                default:
                    return Collections.emptyList();
            }
        }

        return Collections.emptyList();
    }

    // ==================== 工具方法 ====================

    private void sendMsg(CommandSender sender, String message) {
        sender.sendMessage(message.replace('&', '§'));
    }

    private void sendUsage(CommandSender sender, String label) {
        sendMsg(sender, "&6===== &eMetro 线路运营管理 &6=====");
        sendMsg(sender, "&7/" + label + " line setstatus <线路ID> <状态>");
        sendMsg(sender, "&7/" + label + " line setsuspensionmsg <线路ID> <公告>");
        sendMsg(sender, "&7/" + label + " line setaltroute <线路ID> <替代ID> [优先级]");
        sendMsg(sender, "&7/" + label + " line clearaltroute <线路ID> [替代ID]");
        sendMsg(sender, "&7/" + label + " line setautoresume <线路ID> <分钟>");
        sendMsg(sender, "&7/" + label + " line cancelautoresume <线路ID>");
        sendMsg(sender, "&7/" + label + " line setschedule <线路ID> <开始时间>-<结束时间>");
        sendMsg(sender, "&7/" + label + " line clearschedule <线路ID>");
        sendMsg(sender, "&7/" + label + " line stats <线路ID>");
        sendMsg(sender, "&7/" + label + " line status <线路ID>");
        sendMsg(sender, "&7/" + label + " line info <线路ID>");
        sendMsg(sender, "&7/" + label + " line list");
        sendMsg(sender, "&7/" + label + " reload");
        sendMsg(sender, "&a状态值: &fnormal &7(正常) &c| &fsuspended &7(暂停) &e| &fmaintenance &7(维护)");
    }

    private void sendLineUsage(CommandSender sender) {
        sendMsg(sender, "&c未知子命令。可用: setstatus, setsuspensionmsg, setaltroute, clearaltroute, setautoresume, cancelautoresume, setschedule, clearschedule, stats, status, info, list");
    }

    /**
     * 获取状态显示文本（包含彩色图标和状态标签）
     */
    private String getStatusDisplay(LineStatus status) {
        return switch (status) {
            case NORMAL -> "&a● 正常运营 &8[&aNORMAL&8]";
            case SUSPENDED -> "&c● 暂停运营 &8[&cSUSPENDED&8]";
            case MAINTENANCE -> "&e● 维护中 &8[&eMAINTENANCE&8]";
        };
    }

    /**
     * 仅获取状态标签（用于紧凑显示）
     */
    private String getStatusTag(LineStatus status) {
        return switch (status) {
            case NORMAL -> "&a[NORMAL]";
            case SUSPENDED -> "&c[SUSPENDED]";
            case MAINTENANCE -> "&e[MAINTENANCE]";
        };
    }

    private List<String> getLineIds() {
        return api.getAllLines().stream()
                .map(line -> line.getId())
                .collect(Collectors.toList());
    }

    private List<String> filterStartsWith(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream()
                .filter(opt -> opt.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}
