package top.chenray.metroaltroutes.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.cubexmc.metro.api.MetroAPI;
import org.cubexmc.metro.model.Portal;
import top.chenray.metroaltroutes.MetroAltroutes;
import top.chenray.metroaltroutes.cache.RouteCache;

import java.util.List;
import java.util.UUID;

/**
 * 乘车拦截监听器
 *
 * <p>当线路暂停运营（SUSPENDED）时，拦截玩家进入矿车/乘坐行为，
 * 并向玩家展示暂停公告和替代路线建议。</p>
 */
public final class BoardingListener implements Listener {

    private static final String PREFIX = "§8[§bMetro§8] §7";

    private final MetroAltroutes plugin;
    private final MetroAPI api;
    private final RouteCache cache;

    public BoardingListener(MetroAltroutes plugin) {
        this.plugin = plugin;
        this.api = plugin.getMetroAPI();
        this.cache = plugin.getRouteCache();
    }

    // ==================== 事件处理 ====================

    /**
     * 玩家右键点击实体（包括矿车）
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Minecart minecart)) return;

        Player player = event.getPlayer();
        if (tryBlockBoarding(player, minecart.getLocation())) {
            event.setCancelled(true);
        }
    }

    /**
     * 玩家进入交通工具（矿车）
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player player)) return;
        if (!(event.getVehicle() instanceof Minecart minecart)) return;

        if (tryBlockBoarding(player, minecart.getLocation())) {
            event.setCancelled(true);
        }
    }

    /**
     * 玩家与方块交互（右键轨道附近）
     * 捕获直接右键矿车轨道的行为
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        // 检测是否右键了轨道
        if (!isRail(block.getType())) return;

        Player player = event.getPlayer();
        if (tryBlockBoarding(player, block.getLocation())) {
            event.setCancelled(true);
        }
    }

    // ==================== 核心拦截逻辑 ====================

    /**
     * 检查位置是否属于暂停线路，如是则拦截并发送提示
     *
     * @return true 如果成功拦截
     */
    private boolean tryBlockBoarding(Player player, Location location) {
        try {
            // 通过位置查找线路
            String lineId = resolveLineFromLocation(location);
            if (lineId == null) return false;

            // 检查线路是否暂停运营
            if (!cache.isSuspended(lineId)) return false;

            // 执行拦截：发送暂停公告 + 替代路线推荐
            blockBoarding(player, lineId);
            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("BoardingListener 异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 根据位置解析所属线路ID
     */
    private String resolveLineFromLocation(Location location) {
        // 方法1：通过Portal查找
        Portal portal = api.getPortalAt(location);
        if (portal != null) {
            // 找到portal所属的line
            return findLineByPortal(portal.getId());
        }

        // 方法2：遍历所有线路的快照，查找包含此位置的停靠点
        // 这也算是一个 fallback 方案
        for (MetroAPI.LineSnapshot line : api.getLineSnapshots()) {
            for (String stopId : line.orderedStopIds()) {
                MetroAPI.StopSnapshot stop = api.getStopSnapshot(stopId);
                if (stop != null) {
                    // 检查位置是否在停靠点范围内
                    if (isLocationInStop(location, stop)) {
                        return line.id();
                    }
                }
            }
        }

        return null;
    }

    /**
     * 通过 PortalID 查找所属线路
     */
    private String findLineByPortal(String portalId) {
        for (MetroAPI.LineSnapshot line : api.getLineSnapshots()) {
            if (line.portalIds() != null && line.portalIds().contains(portalId)) {
                return line.id();
            }
        }
        return null;
    }

    /**
     * 粗略判断位置是否在停靠点范围内
     */
    private boolean isLocationInStop(Location location, MetroAPI.StopSnapshot stop) {
        // 若 stop 不包含 corner 信息则跳过
        // 这里简化为：检查 stop 所属的世界是否匹配
        try {
            if (stop.worldName() != null && !stop.worldName().equals(location.getWorld().getName())) {
                return false;
            }
            // 如果停靠点有精确坐标范围可进一步判断
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 拦截处理 ====================

    /**
     * 执行拦截并发送暂停公告和替代路线
     */
    private void blockBoarding(Player player, String lineId) {
        // 1. 发送暂停公告
        String suspensionMsg = cache.getSuspensionMessage(lineId);
        if (suspensionMsg != null && !suspensionMsg.isEmpty()) {
            player.sendMessage(PREFIX + "§c该线路已暂停运营:");
            player.sendMessage("§7  " + suspensionMsg.replace('&', '§'));
        } else {
            player.sendMessage(PREFIX + "§c线路 §e" + lineId + " §c当前暂停运营，暂不可乘坐。");
        }

        // 2. 发送替代路线推荐
        List<String> altRoutes = cache.getAlternativeRoutes(lineId);
        if (altRoutes != null && !altRoutes.isEmpty()) {
            player.sendMessage(PREFIX + "§a建议您选择以下替代路线乘坐:");
            for (String altId : altRoutes) {
                MetroAPI.LineSnapshot altSnap = cache.getSnapshot(altId);
                String altName = (altSnap != null) ? altSnap.name() : altId;
                player.sendMessage("  §e● §6" + altName + " §7(" + altId + ")");
            }
        }

        // 3. 音效反馈
        if (!api.isFoliaRuntime()) {
            player.playSound(player.getLocation(),
                    org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
        }

        // 4. 记录日志
        plugin.log(player.getName() + " 尝试乘坐暂停线路 " + lineId + " 已被拦截。");
    }

    // ==================== 工具方法 ====================

    /**
     * 判断方块是否为轨道
     */
    private boolean isRail(Material material) {
        return switch (material) {
            case RAIL, POWERED_RAIL, DETECTOR_RAIL, ACTIVATOR_RAIL -> true;
            default -> false;
        };
    }
}
