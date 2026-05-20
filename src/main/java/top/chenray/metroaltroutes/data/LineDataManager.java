package top.chenray.metroaltroutes.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.cubexmc.metro.api.MetroAPI;
import org.cubexmc.metro.model.LineStatus;
import top.chenray.metroaltroutes.MetroAltroutes;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class LineDataManager {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final MetroAltroutes plugin;
    private final MetroAPI api;

    private final Map<String, Map<String, Integer>> altRoutePriority = new ConcurrentHashMap<>();
    private final Map<String, AutoRecoveryEntry> autoRecoverMap = new ConcurrentHashMap<>();
    private final Map<String, ScheduleEntry> scheduleMap = new ConcurrentHashMap<>();
    private final Map<String, LineStats> statsMap = new ConcurrentHashMap<>();

    public LineDataManager(MetroAltroutes plugin) {
        this.plugin = plugin;
        this.api = plugin.getMetroAPI();
        plugin.saveDefaultConfig();
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        altRoutePriority.clear();
        autoRecoverMap.clear();
        scheduleMap.clear();
        statsMap.clear();

        ConfigurationSection altSection = config.getConfigurationSection("line-data.alt-routes");
        if (altSection != null) {
            for (String lineId : altSection.getKeys(false)) {
                ConfigurationSection lineSection = altSection.getConfigurationSection(lineId);
                if (lineSection == null) continue;
                Map<String, Integer> priorityMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                for (String altId : lineSection.getKeys(false)) {
                    priorityMap.put(altId, lineSection.getInt(altId, 100));
                }
                altRoutePriority.put(lineId.toLowerCase(Locale.ROOT), priorityMap);
            }
        }

        ConfigurationSection autoSection = config.getConfigurationSection("line-data.auto-recover");
        if (autoSection != null) {
            for (String lineId : autoSection.getKeys(false)) {
                long expireAt = autoSection.getLong(lineId, 0L);
                if (expireAt > System.currentTimeMillis()) {
                    autoRecoverMap.put(lineId.toLowerCase(Locale.ROOT), new AutoRecoveryEntry(expireAt));
                }
            }
        }

        ConfigurationSection scheduleSection = config.getConfigurationSection("line-data.schedules");
        if (scheduleSection != null) {
            for (String lineId : scheduleSection.getKeys(false)) {
                ConfigurationSection lineSection = scheduleSection.getConfigurationSection(lineId);
                if (lineSection == null) continue;
                String start = lineSection.getString("start", "00:00");
                String end = lineSection.getString("end", "00:00");
                scheduleMap.put(lineId.toLowerCase(Locale.ROOT), new ScheduleEntry(start, end));
            }
        }

        ConfigurationSection statsSection = config.getConfigurationSection("line-data.stats");
        if (statsSection != null) {
            for (String lineId : statsSection.getKeys(false)) {
                ConfigurationSection lineSection = statsSection.getConfigurationSection(lineId);
                if (lineSection == null) continue;
                LineStats stats = new LineStats(
                        lineSection.getInt("suspend-count", 0),
                        lineSection.getInt("intercept-count", 0),
                        lineSection.getInt("alt-recommend-count", 0)
                );
                statsMap.put(lineId.toLowerCase(Locale.ROOT), stats);
            }
        }
    }

    public void startScheduleTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::onTick, 20L, 1200L);
    }

    private void onTick() {
        checkAutoRecover();
        checkSchedules();
    }

    private void checkAutoRecover() {
        long now = System.currentTimeMillis();
        List<String> recovered = new ArrayList<>();
        for (var entry : autoRecoverMap.entrySet()) {
            String lineId = entry.getKey();
            AutoRecoveryEntry task = entry.getValue();
            if (task.expireAt <= now) {
                if (api.getLineStatus(lineId) != LineStatus.NORMAL) {
                    setLineStatus(lineId, LineStatus.NORMAL, "自动恢复为正常运营");
                    plugin.notifyAdmins("[metro-altroutes] 线路 " + lineId + " 已到达自动恢复时间，已恢复正常运营。");
                }
                recovered.add(lineId);
            }
        }
        for (String lineId : recovered) {
            autoRecoverMap.remove(lineId);
            saveAutoRecoverConfig();
        }
    }

    private void checkSchedules() {
        int currentMinute = LocalTime.now().getHour() * 60 + LocalTime.now().getMinute();
        for (var entry : scheduleMap.entrySet()) {
            String lineId = entry.getKey();
            ScheduleEntry schedule = entry.getValue();
            boolean withinWindow = schedule.isWithinWindow(currentMinute);
            LineStatus current = api.getLineStatus(lineId);
            if (withinWindow && current != LineStatus.SUSPENDED) {
                setLineStatus(lineId, LineStatus.SUSPENDED, "线路进入计划维护时段，自动切换为暂停运营");
                plugin.notifyAdmins("[metro-altroutes] 线路 " + lineId + " 已进入计划维护时段，已自动暂停运营。");
            } else if (!withinWindow && current == LineStatus.SUSPENDED) {
                setLineStatus(lineId, LineStatus.NORMAL, "计划维护结束，线路已自动恢复正常运营");
                plugin.notifyAdmins("[metro-altroutes] 线路 " + lineId + " 已退出计划维护时段，已自动恢复。");
            }
        }
    }

    private void setLineStatus(String lineId, LineStatus status, String note) {
        var line = api.getLineManager().getLine(lineId);
        if (line == null) return;
        line.setLineStatus(status);
        api.getLineManager().saveConfig();
        plugin.getRouteCache().invalidate(lineId);
        if (status == LineStatus.SUSPENDED) {
            recordSuspend(lineId);
        }
        plugin.log("自动调度: 线路 " + lineId + " -> " + status + ", 说明: " + note);
    }

    public List<String> getAlternativeRoutes(String lineId) {
        String normalized = lineId.toLowerCase(Locale.ROOT);
        Map<String, Integer> priorityMap = altRoutePriority.get(normalized);
        if (priorityMap != null && !priorityMap.isEmpty()) {
            return priorityMap.entrySet().stream()
                    .sorted(Comparator.comparingInt((Map.Entry<String, Integer> e) -> e.getValue())
                            .thenComparing(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }

        var snapshot = api.getLineSnapshot(lineId);
        if (snapshot == null) {
            return Collections.emptyList();
        }
        List<String> ids = snapshot.alternativeRouteIds();
        return ids == null ? Collections.emptyList() : new ArrayList<>(ids);
    }

    public void setAlternativeRoute(String lineId, String altId, int priority) {
        String normalized = lineId.toLowerCase(Locale.ROOT);
        altRoutePriority.computeIfAbsent(normalized, k -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER))
                .put(altId, priority);
        saveAltRoutesConfig();
    }

    public boolean removeAlternativeRoute(String lineId, String altId) {
        if (altId == null) {
            return clearAlternativeRoutes(lineId);
        }

        String normalized = lineId.toLowerCase(Locale.ROOT);
        Map<String, Integer> map = altRoutePriority.get(normalized);
        if (map == null || map.remove(altId) == null) {
            return false;
        }
        if (map.isEmpty()) {
            altRoutePriority.remove(normalized);
        }
        saveAltRoutesConfig();
        return true;
    }

    public boolean clearAlternativeRoutes(String lineId) {
        String normalized = lineId.toLowerCase(Locale.ROOT);
        if (altRoutePriority.remove(normalized) != null) {
            saveAltRoutesConfig();
            return true;
        }
        return false;
    }

    public LineStats getStats(String lineId) {
        return statsMap.computeIfAbsent(lineId.toLowerCase(Locale.ROOT), k -> new LineStats(0, 0, 0));
    }

    public void recordSuspend(String lineId) {
        String normalized = lineId.toLowerCase(Locale.ROOT);
        statsMap.compute(normalized, (key, stats) -> {
            if (stats == null) stats = new LineStats(0, 0, 0);
            return new LineStats(stats.suspendCount() + 1, stats.interceptCount(), stats.altRecommendCount());
        });
        saveStatsConfig();
    }

    public void recordIntercept(String lineId) {
        String normalized = lineId.toLowerCase(Locale.ROOT);
        statsMap.compute(normalized, (key, stats) -> {
            if (stats == null) stats = new LineStats(0, 0, 0);
            return new LineStats(stats.suspendCount(), stats.interceptCount() + 1, stats.altRecommendCount());
        });
        saveStatsConfig();
    }

    public void recordAltRecommend(String lineId) {
        String normalized = lineId.toLowerCase(Locale.ROOT);
        statsMap.compute(normalized, (key, stats) -> {
            if (stats == null) stats = new LineStats(0, 0, 0);
            return new LineStats(stats.suspendCount(), stats.interceptCount(), stats.altRecommendCount() + 1);
        });
        saveStatsConfig();
    }

    public void setAutoRecover(String lineId, int minutes) {
        long expireAt = System.currentTimeMillis() + minutes * 60_000L;
        autoRecoverMap.put(lineId.toLowerCase(Locale.ROOT), new AutoRecoveryEntry(expireAt));
        saveAutoRecoverConfig();
    }

    public boolean cancelAutoRecover(String lineId) {
        String normalized = lineId.toLowerCase(Locale.ROOT);
        if (autoRecoverMap.remove(normalized) != null) {
            saveAutoRecoverConfig();
            return true;
        }
        return false;
    }

    public Integer getAutoRecoverMinutesLeft(String lineId) {
        AutoRecoveryEntry entry = autoRecoverMap.get(lineId.toLowerCase(Locale.ROOT));
        if (entry == null) return null;
        long leftMs = entry.expireAt - System.currentTimeMillis();
        return leftMs > 0 ? (int) (leftMs / 60000L) : 0;
    }

    public void setSchedule(String lineId, String start, String end) {
        scheduleMap.put(lineId.toLowerCase(Locale.ROOT), new ScheduleEntry(start, end));
        saveScheduleConfig();
    }

    public boolean clearSchedule(String lineId) {
        String normalized = lineId.toLowerCase(Locale.ROOT);
        if (scheduleMap.remove(normalized) != null) {
            saveScheduleConfig();
            return true;
        }
        return false;
    }

    public ScheduleEntry getSchedule(String lineId) {
        return scheduleMap.get(lineId.toLowerCase(Locale.ROOT));
    }

    public Set<String> getScheduledLines() {
        return scheduleMap.keySet();
    }

    private void saveAltRoutesConfig() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.createSection("line-data.alt-routes");
        for (var entry : altRoutePriority.entrySet()) {
            ConfigurationSection lineSection = section.createSection(entry.getKey());
            for (var altEntry : entry.getValue().entrySet()) {
                lineSection.set(altEntry.getKey(), altEntry.getValue());
            }
        }
        plugin.saveConfig();
    }

    private void saveAutoRecoverConfig() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.createSection("line-data.auto-recover");
        for (var entry : autoRecoverMap.entrySet()) {
            section.set(entry.getKey(), entry.getValue().expireAt);
        }
        plugin.saveConfig();
    }

    private void saveScheduleConfig() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.createSection("line-data.schedules");
        for (var entry : scheduleMap.entrySet()) {
            ConfigurationSection lineSection = section.createSection(entry.getKey());
            lineSection.set("start", entry.getValue().formatTime(entry.getValue().startMinute));
            lineSection.set("end", entry.getValue().formatTime(entry.getValue().endMinute));
        }
        plugin.saveConfig();
    }

    private void saveStatsConfig() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.createSection("line-data.stats");
        for (var entry : statsMap.entrySet()) {
            ConfigurationSection lineSection = section.createSection(entry.getKey());
            lineSection.set("suspend-count", entry.getValue().suspendCount());
            lineSection.set("intercept-count", entry.getValue().interceptCount());
            lineSection.set("alt-recommend-count", entry.getValue().altRecommendCount());
        }
        plugin.saveConfig();
    }

    private void saveAll() {
        saveAltRoutesConfig();
        saveAutoRecoverConfig();
        saveScheduleConfig();
        saveStatsConfig();
    }

    public static final class LineStats {
        private final int suspendCount;
        private final int interceptCount;
        private final int altRecommendCount;

        public LineStats(int suspendCount, int interceptCount, int altRecommendCount) {
            this.suspendCount = suspendCount;
            this.interceptCount = interceptCount;
            this.altRecommendCount = altRecommendCount;
        }

        public int suspendCount() {
            return suspendCount;
        }

        public int interceptCount() {
            return interceptCount;
        }

        public int altRecommendCount() {
            return altRecommendCount;
        }
    }

    public static final class ScheduleEntry {
        private final int startMinute;
        private final int endMinute;

        public ScheduleEntry(String start, String end) {
            this.startMinute = parseTime(start);
            this.endMinute = parseTime(end);
        }

        private static int parseTime(String input) {
            try {
                LocalTime time = LocalTime.parse(input, TIME_FORMAT);
                return time.getHour() * 60 + time.getMinute();
            } catch (Exception e) {
                return 0;
            }
        }

        public String getFormattedStart() { return formatTime(startMinute); }
        public String getFormattedEnd() { return formatTime(endMinute); }

        public boolean isWithinWindow(int currentMinute) {
            if (startMinute <= endMinute) {
                return currentMinute >= startMinute && currentMinute < endMinute;
            }
            return currentMinute >= startMinute || currentMinute < endMinute;
        }

        public String formatTime(int minute) {
            int hour = minute / 60;
            int minuteOfHour = minute % 60;
            return String.format("%02d:%02d", hour, minuteOfHour);
        }
    }

    private static final class AutoRecoveryEntry {
        private final long expireAt;

        private AutoRecoveryEntry(long expireAt) {
            this.expireAt = expireAt;
        }
    }
}
