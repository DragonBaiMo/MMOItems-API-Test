package net.Indyuce.mmoitems.api.upgrade.economy;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.manager.Reloadable;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * 强化经济消耗处理器
 * <p>
 * 管理强化时的货币消耗功能。主要特性：
 * <ul>
 *     <li>支持按等级段配置不同费用</li>
 *     <li>支持 Vault 和 PlayerPoints 经济系统（通过 DrcomoCoreLib）</li>
 *     <li>支持原子性操作（失败时可回滚）</li>
 * </ul>
 * </p>
 * <p>
 * 依赖 DrcomoCoreLib 的 EconomyProvider 接口：
 * <code>cn.drcomo.corelib.hook.economy.EconomyProvider</code>
 * </p>
 *
 * @author MMOItems Team
 * @since 强化系统扩展
 */
public class UpgradeEconomyHandler implements Reloadable {

    /**
     * 是否启用经济消耗
     */
    private boolean enabled;

    /**
     * 经济类型：vault | playerpoints
     */
    private String economyType;

    /**
     * 按等级段配置的费用
     * <p>Key: 等级范围字符串（如 "0-5"），Value: 费用</p>
     */
    private final Map<String, Double> tierCosts = new LinkedHashMap<>();

    /**
     * 默认费用（未匹配到等级段时使用）
     */
    private double defaultCost;

    /**
     * 经济提供者对象（DrcomoCoreLib）
     */
    private Object economyProvider;

    /**
     * DrcomoCoreLib 是否可用
     */
    private boolean drcomoAvailable;

    /**
     * 创建经济处理器并加载配置
     */
    public UpgradeEconomyHandler() {
        reload();
    }

    @Override
    public void reload() {
        tierCosts.clear();
        economyProvider = null;

        ConfigurationSection config = MMOItems.plugin.getConfig()
                .getConfigurationSection("item-upgrading.economy-cost");

        if (config == null) {
            this.enabled = false;
            return;
        }

        this.enabled = config.getBoolean("enabled", false);
        if (!enabled) {
            return;
        }

        this.economyType = config.getString("type", "vault").toLowerCase();

        // 加载等级段费用
        ConfigurationSection tiers = config.getConfigurationSection("tiers");
        if (tiers != null) {
            for (String key : tiers.getKeys(false)) {
                double cost = tiers.getDouble(key, 0);
                if ("default".equalsIgnoreCase(key)) {
                    this.defaultCost = cost;
                } else {
                    tierCosts.put(key, cost);
                }
            }
        }

        // 初始化经济提供者
        initEconomyProvider();
    }

    /**
     * 初始化经济提供者（通过反射加载 DrcomoCoreLib）
     */
    private void initEconomyProvider() {
        drcomoAvailable = Bukkit.getPluginManager().isPluginEnabled("DrcomoCoreLib");

        if (!drcomoAvailable) {
            MMOItems.plugin.getLogger().warning("[经济消耗] DrcomoCoreLib 未安装，经济消耗功能已禁用");
            enabled = false;
            return;
        }

        try {
            // 根据类型选择对应的 Provider
            Class<?> providerClass;
            if ("playerpoints".equals(economyType)) {
                providerClass = Class.forName("cn.drcomo.corelib.hook.economy.provider.PlayerPointsEconomyProvider");
            } else {
                // 默认使用 Vault
                providerClass = Class.forName("cn.drcomo.corelib.hook.economy.provider.VaultEconomyProvider");
            }

            // 获取 DebugUtil.LogLevel 枚举
            Class<?> logLevelClass = Class.forName("cn.drcomo.corelib.util.DebugUtil$LogLevel");
            Object infoLevel = Enum.valueOf((Class<Enum>) logLevelClass, "INFO");

            // 实例化 Provider (plugin, logLevel)
            economyProvider = providerClass.getConstructor(
                    org.bukkit.plugin.Plugin.class,
                    logLevelClass
            ).newInstance(MMOItems.plugin, infoLevel);

            // 检查是否可用
            Method isEnabledMethod = providerClass.getMethod("isEnabled");
            boolean providerEnabled = (boolean) isEnabledMethod.invoke(economyProvider);

            if (!providerEnabled) {
                MMOItems.plugin.getLogger().warning("[经济消耗] " + economyType + " 经济服务不可用");
                enabled = false;
                economyProvider = null;
            } else {
                MMOItems.plugin.getLogger().info("[经济消耗] 已挂钩到 " + economyType + " 经济服务");
            }
        } catch (ClassNotFoundException e) {
            MMOItems.plugin.getLogger().warning("[经济消耗] DrcomoCoreLib 经济模块未找到: " + e.getMessage());
            enabled = false;
        } catch (Exception e) {
            MMOItems.plugin.getLogger().log(Level.WARNING, "[经济消耗] 初始化经济提供者失败", e);
            enabled = false;
        }
    }

    /**
     * 检查经济消耗是否启用
     *
     * @return 如果启用返回 true
     */
    public boolean isEnabled() {
        return enabled && economyProvider != null;
    }

    /**
     * 获取指定等级的强化费用
     *
     * @param currentLevel 当前强化等级
     * @return 该等级所需费用
     */
    public double getCost(int currentLevel) {
        // 遍历等级段配置
        for (Map.Entry<String, Double> entry : tierCosts.entrySet()) {
            int[] range = parseRange(entry.getKey());
            if (range != null && currentLevel >= range[0] && currentLevel <= range[1]) {
                return entry.getValue();
            }
        }
        return defaultCost;
    }

    /**
     * 检查玩家是否有足够资金
     *
     * @param player 玩家
     * @param cost   所需费用
     * @return 如果余额足够返回 true
     */
    public boolean canAfford(@NotNull Player player, double cost) {
        if (!isEnabled() || cost <= 0) {
            return true;
        }

        try {
            Method hasBalanceMethod = economyProvider.getClass()
                    .getMethod("hasBalance", Player.class, double.class);
            return (boolean) hasBalanceMethod.invoke(economyProvider, player, cost);
        } catch (Exception e) {
            MMOItems.plugin.getLogger().log(Level.WARNING, "[经济消耗] 检查余额失败", e);
            return true; // 出错时允许继续
        }
    }

    /**
     * 扣除费用
     *
     * @param player 玩家
     * @param cost   扣除金额
     * @return 扣款结果
     */
    @NotNull
    public EconomyOperationResult withdraw(@NotNull Player player, double cost) {
        if (!isEnabled() || cost <= 0) {
            return EconomyOperationResult.success();
        }

        try {
            Method withdrawMethod = economyProvider.getClass()
                    .getMethod("withdraw", Player.class, double.class);
            Object response = withdrawMethod.invoke(economyProvider, player, cost);

            // 获取 EconomyResponse.success 字段
            java.lang.reflect.Field successField = response.getClass().getField("success");
            boolean success = successField.getBoolean(response);

            if (success) {
                return EconomyOperationResult.success();
            } else {
                java.lang.reflect.Field errorField = response.getClass().getField("errorMessage");
                String errorMessage = (String) errorField.get(response);
                return EconomyOperationResult.fail(errorMessage != null ? errorMessage : "扣款失败");
            }
        } catch (Exception e) {
            MMOItems.plugin.getLogger().log(Level.WARNING, "[经济消耗] 扣款失败", e);
            return EconomyOperationResult.fail("扣款异常: " + e.getMessage());
        }
    }

    /**
     * 回滚费用（退款）
     *
     * @param player 玩家
     * @param cost   退还金额
     */
    public void refund(@NotNull Player player, double cost) {
        if (!isEnabled() || cost <= 0) {
            return;
        }

        try {
            Method depositMethod = economyProvider.getClass()
                    .getMethod("deposit", Player.class, double.class);
            depositMethod.invoke(economyProvider, player, cost);
        } catch (Exception e) {
            MMOItems.plugin.getLogger().log(Level.WARNING, "[经济消耗] 退款失败", e);
        }
    }

    /**
     * 获取玩家余额
     *
     * @param player 玩家
     * @return 余额
     */
    public double getBalance(@NotNull Player player) {
        if (!isEnabled()) {
            return 0;
        }

        try {
            Method getBalanceMethod = economyProvider.getClass()
                    .getMethod("getBalance", Player.class);
            return (double) getBalanceMethod.invoke(economyProvider, player);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 格式化金额显示
     *
     * @param amount 金额
     * @return 格式化后的字符串
     */
    @NotNull
    public String format(double amount) {
        if (!isEnabled()) {
            return String.valueOf(amount);
        }

        try {
            Method formatMethod = economyProvider.getClass()
                    .getMethod("format", double.class);
            return (String) formatMethod.invoke(economyProvider, amount);
        } catch (Exception e) {
            return String.format("%.2f", amount);
        }
    }

    /**
     * 解析等级范围字符串
     *
     * @param rangeStr 范围字符串（如 "0-5"）
     * @return [min, max] 数组，解析失败返回 null
     */
    @Nullable
    private int[] parseRange(@NotNull String rangeStr) {
        try {
            String[] parts = rangeStr.split("-");
            if (parts.length == 2) {
                int min = Integer.parseInt(parts[0].trim());
                int max = Integer.parseInt(parts[1].trim());
                return new int[]{min, max};
            } else if (parts.length == 1) {
                // 单个等级
                int level = Integer.parseInt(parts[0].trim());
                return new int[]{level, level};
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    /**
     * 经济操作结果
     */
    public static class EconomyOperationResult {
        private final boolean success;
        private final String errorMessage;

        private EconomyOperationResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static EconomyOperationResult success() {
            return new EconomyOperationResult(true, null);
        }

        public static EconomyOperationResult fail(String errorMessage) {
            return new EconomyOperationResult(false, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
