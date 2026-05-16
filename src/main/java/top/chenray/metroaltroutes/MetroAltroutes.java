package top.chenray.metroaltroutes;

import org.bukkit.plugin.java.JavaPlugin;
import org.cubexmc.metro.api.MetroAPI;
import top.chenray.metroaltroutes.cache.RouteCache;
import top.chenray.metroaltroutes.commands.LineCommand;
import top.chenray.metroaltroutes.listeners.BoardingListener;

import java.util.Objects;
import java.util.logging.Level;

/**
 * metro-altroutes - Metro 附属插件
 * 提供线路暂停运营管理、替代路线推荐、乘车拦截功能
 *
 * @author ChenRay
 */
public final class MetroAltroutes extends JavaPlugin {

    private static MetroAltroutes instance;
    private MetroAPI metroAPI;
    private RouteCache routeCache;

    @Override
    public void onEnable() {
        instance = this;

        // 等待 Metro 启用后获取 API
        metroAPI = MetroAPI.getInstance();
        if (metroAPI == null) {
            getLogger().severe("无法获取 MetroAPI 实例！请确保 Metro 插件已正确加载。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("已成功连接到 Metro API v" +
                metroAPI.getPlugin().getDescription().getVersion());
        getLogger().info("运行环境: " + (metroAPI.isFoliaRuntime() ? "Folia" : "Paper/Spigot"));

        // 初始化性能缓存
        routeCache = new RouteCache(this);
        routeCache.startCacheRefreshTask();

        // 注册命令
        LineCommand commandExecutor = new LineCommand(this);
        Objects.requireNonNull(getCommand("metroaltroutes")).setExecutor(commandExecutor);
        Objects.requireNonNull(getCommand("metroaltroutes")).setTabCompleter(commandExecutor);

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new BoardingListener(this), this);

        getLogger().info("metro-altroutes 已启用！使用 /m line 管理线路运营状态。");
    }

    @Override
    public void onDisable() {
        if (routeCache != null) {
            routeCache.shutdown();
        }
        getLogger().info("metro-altroutes 已卸载。");
        instance = null;
    }

    // ==================== Getters ====================

    public static MetroAltroutes getInstance() {
        return instance;
    }

    public MetroAPI getMetroAPI() {
        return metroAPI;
    }

    public RouteCache getRouteCache() {
        return routeCache;
    }

    // ==================== Utility ====================

    /**
     * 安全地记录操作日志
     */
    public void log(String message) {
        getLogger().log(Level.INFO, message);
    }

    /**
     * 在 Folia 环境下安全地调度任务
     */
    public void runTask(Runnable task) {
        if (metroAPI.isFoliaRuntime()) {
            getServer().getGlobalRegionScheduler().run(instance, scheduledTask -> task.run());
        } else {
            getServer().getScheduler().runTask(instance, task);
        }
    }

    /**
     * 安全地调度异步任务
     */
    public void runTaskAsync(Runnable task) {
        if (metroAPI.isFoliaRuntime()) {
            getServer().getAsyncScheduler().runNow(instance, scheduledTask -> task.run());
        } else {
            getServer().getScheduler().runTaskAsynchronously(instance, task);
        }
    }
}
