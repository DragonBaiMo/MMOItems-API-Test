package net.Indyuce.mmoitems.api.upgrade.bonus;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.manager.Reloadable;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * 强化概率加成计算器
 * <p>
 * 支持以下加成来源：
 * <ul>
 *     <li>PAPI 公式加成（需要 PlaceholderAPI + DrcomoCoreLib）</li>
 *     <li>权限节点加成</li>
 * </ul>
 * </p>
 *
 * @author MMOItems Team
 * @since 强化系统扩展
 */
public class UpgradeChanceBonusCalculator implements Reloadable {

    /**
     * 是否启用概率加成
     */
    private boolean enabled;

    /**
     * 概率加成公式（支持 PAPI 变量）
     */
    private String formula;

    /**
     * 权限加成配置列表（按优先级排序）
     */
    private final List<PermissionBonus> permissionBonuses = new ArrayList<>();

    /**
     * 最大额外加成上限
     */
    private double maxBonus;

    /**
     * PlaceholderAPI 是否可用
     */
    private boolean papiAvailable;

    /**
     * DrcomoCoreLib FormulaCalculator 是否可用
     */
    private boolean formulaCalculatorAvailable;

    /**
     * PlaceholderAPI setPlaceholders 方法（反射缓存）
     */
    private Method papiSetPlaceholdersMethod;

    /**
     * DrcomoCoreLib FormulaCalculator evaluate 方法（反射缓存）
     */
    private Method formulaEvaluateMethod;
    private Object formulaCalculatorInstance;

    /**
     * 创建并加载配置
     */
    public UpgradeChanceBonusCalculator() {
        reload();
    }

    @Override
    public void reload() {
        permissionBonuses.clear();

        // 检测 PlaceholderAPI 可用性
        papiAvailable = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        if (papiAvailable) {
            try {
                Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                papiSetPlaceholdersMethod = papiClass.getMethod("setPlaceholders", org.bukkit.OfflinePlayer.class, String.class);
            } catch (Exception e) {
                papiAvailable = false;
                MMOItems.plugin.getLogger().log(Level.WARNING, "PlaceholderAPI 反射失败，公式变量替换不可用");
            }
        }

        // 检测 DrcomoCoreLib FormulaCalculator 可用性
        formulaCalculatorAvailable = false;
        try {
            Class<?> formulaClass = Class.forName("cn.drcomo.corelib.math.FormulaCalculator");
            formulaCalculatorInstance = formulaClass.getDeclaredConstructor().newInstance();
            formulaEvaluateMethod = formulaClass.getMethod("evaluate", String.class);
            formulaCalculatorAvailable = true;
        } catch (Exception e) {
            // DrcomoCoreLib 不可用，使用简单解析
        }

        ConfigurationSection config = MMOItems.plugin.getConfig().getConfigurationSection("item-upgrading.chance-bonus");
        if (config == null) {
            this.enabled = false;
            this.formula = "0";
            this.maxBonus = 50;
            return;
        }

        this.enabled = config.getBoolean("enabled", false);
        this.formula = config.getString("formula", "0");
        this.maxBonus = config.getDouble("max-bonus", 50);

        // 解析权限加成配置
        ConfigurationSection permSection = config.getConfigurationSection("permission-bonus");
        if (permSection != null) {
            for (String key : permSection.getKeys(false)) {
                ConfigurationSection bonusConfig = permSection.getConfigurationSection(key);
                if (bonusConfig == null) continue;

                String permission = bonusConfig.getString("permission", "");
                double bonus = bonusConfig.getDouble("bonus", 0);
                int priority = bonusConfig.getInt("priority", 0);

                permissionBonuses.add(new PermissionBonus(key, permission, bonus, priority));
            }
            // 按优先级降序排列
            Collections.sort(permissionBonuses);
        }
    }

    /**
     * 检查是否启用
     *
     * @return 如果启用返回 true
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 计算玩家的额外概率加成
     *
     * @param player       玩家
     * @param upgradeLevel 当前强化等级
     * @return 额外加成百分比（0-maxBonus）
     */
    public double calculateBonus(@NotNull Player player, int upgradeLevel) {
        if (!enabled) {
            return 0;
        }

        double totalBonus = 0;

        // 1. 公式加成
        totalBonus += calculateFormulaBonus(player, upgradeLevel);

        // 2. 权限加成（只匹配最高优先级的一个）
        totalBonus += calculatePermissionBonus(player);

        // 应用上限
        return Math.min(totalBonus, maxBonus);
    }

    /**
     * 计算公式加成
     *
     * @param player       玩家
     * @param upgradeLevel 强化等级
     * @return 公式计算结果
     */
    private double calculateFormulaBonus(@NotNull Player player, int upgradeLevel) {
        if (formula == null || formula.isEmpty() || "0".equals(formula)) {
            return 0;
        }

        String processedFormula = formula;

        // 替换内置变量
        processedFormula = processedFormula.replace("%mmoitems_upgrade_level%", String.valueOf(upgradeLevel));

        // 使用 PAPI 替换变量
        if (papiAvailable && papiSetPlaceholdersMethod != null) {
            try {
                processedFormula = (String) papiSetPlaceholdersMethod.invoke(null, player, processedFormula);
            } catch (Exception e) {
                // PAPI 替换失败，继续使用原公式
            }
        }

        // 计算公式
        return evaluateFormula(processedFormula);
    }

    /**
     * 计算权限加成
     *
     * @param player 玩家
     * @return 权限加成值
     */
    private double calculatePermissionBonus(@NotNull Player player) {
        for (PermissionBonus bonus : permissionBonuses) {
            // 默认权限（空字符串）或玩家有对应权限
            if (bonus.isDefault() || player.hasPermission(bonus.getPermission())) {
                return bonus.getBonus();
            }
        }
        return 0;
    }

    /**
     * 计算数学表达式
     *
     * @param expression 表达式
     * @return 计算结果
     */
    private double evaluateFormula(@NotNull String expression) {
        // 尝试使用 DrcomoCoreLib FormulaCalculator
        if (formulaCalculatorAvailable && formulaEvaluateMethod != null && formulaCalculatorInstance != null) {
            try {
                Object result = formulaEvaluateMethod.invoke(formulaCalculatorInstance, expression);
                if (result instanceof Number) {
                    return ((Number) result).doubleValue();
                }
            } catch (Exception e) {
                // 公式计算失败，尝试简单解析
            }
        }

        // 简单数字解析
        try {
            return Double.parseDouble(expression.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 权限加成配置
     */
    public static class PermissionBonus implements Comparable<PermissionBonus> {
        private final String id;
        private final String permission;
        private final double bonus;
        private final int priority;

        public PermissionBonus(@NotNull String id, @NotNull String permission, double bonus, int priority) {
            this.id = id;
            this.permission = permission;
            this.bonus = bonus;
            this.priority = priority;
        }

        @NotNull
        public String getId() {
            return id;
        }

        @NotNull
        public String getPermission() {
            return permission;
        }

        public double getBonus() {
            return bonus;
        }

        public int getPriority() {
            return priority;
        }

        public boolean isDefault() {
            return permission.isEmpty();
        }

        @Override
        public int compareTo(@NotNull PermissionBonus other) {
            return Integer.compare(other.priority, this.priority);
        }
    }
}
