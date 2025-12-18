package net.Indyuce.mmoitems.api.upgrade.guarantee;

import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.manager.Reloadable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 强化保底机制管理器
 * <p>
 * 管理强化保底功能的配置和操作。保底机制：
 * 同一物品连续强化失败 N 次后，下一次强化必定成功。
 * </p>
 * <p>
 * 保底数据存储在物品 NBT 中，跟随物品转移。
 * </p>
 *
 * @author MMOItems Team
 * @since 强化系统扩展
 */
public class GuaranteeManager implements Reloadable {

    /**
     * 是否启用保底机制
     */
    private boolean enabled;

    /**
     * 保底阈值（连续失败次数）
     */
    private int threshold;

    /**
     * 成功后是否重置计数
     */
    private boolean resetOnSuccess;

    /**
     * 失败计数过期小时数（<=0 表示不过期）
     */
    private int expireHours;

    /**
     * 创建保底管理器并加载配置
     */
    public GuaranteeManager() {
        reload();
    }

    @Override
    public void reload() {
        ConfigurationSection config = MMOItems.plugin.getConfig().getConfigurationSection("item-upgrading.guarantee");

        if (config == null) {
            // 默认配置
            this.enabled = true;
            this.threshold = 30;
            this.resetOnSuccess = true;
            this.expireHours = 0;
            return;
        }

        this.enabled = config.getBoolean("enabled", true);
        this.threshold = config.getInt("threshold", 30);
        this.resetOnSuccess = config.getBoolean("reset-on-success", true);
        this.expireHours = config.getInt("expire-hours", 0);
    }

    /**
     * 检查保底机制是否启用
     *
     * @return 如果启用返回 true
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 获取保底阈值
     *
     * @return 连续失败次数阈值
     */
    public int getThreshold() {
        return threshold;
    }

    /**
     * 检查成功后是否重置计数
     *
     * @return 如果重置返回 true
     */
    public boolean isResetOnSuccess() {
        return resetOnSuccess;
    }

    /**
     * 从物品获取保底数据
     *
     * @param item 物品
     * @return 保底数据，如果物品无效返回 null
     */
    @Nullable
    public GuaranteeData getData(@Nullable ItemStack item) {
        GuaranteeData data = GuaranteeData.fromItem(item);
        if (data != null) {
            data.applyExpiration(expireHours);
        }
        return data;
    }

    /**
     * 检查物品是否触发保底
     * <p>
     * 当保底机制启用且物品连续失败次数达到阈值时返回 true
     * </p>
     *
     * @param item 物品
     * @return 如果触发保底返回 true
     */
    public boolean isGuaranteed(@Nullable ItemStack item) {
        if (!enabled) {
            return false;
        }

        GuaranteeData data = getData(item);
        return data != null && data.isGuaranteed(threshold);
    }

    /**
     * 记录强化失败
     * <p>
     * 增加连续失败计数并保存到物品 NBT
     * </p>
     *
     * @param nbtItem NBT 物品包装器
     * @return 更新后的保底数据
     */
    @NotNull
    public GuaranteeData recordFail(@NotNull NBTItem nbtItem) {
        GuaranteeData data = GuaranteeData.fromItem(nbtItem.getItem());
        if (data == null) {
            data = new GuaranteeData();
        }
        data.applyExpiration(expireHours);
        data.incrementFails();
        data.applyToItem(nbtItem);
        return data;
    }

    /**
     * 记录强化成功
     * <p>
     * 如果配置为成功后重置，则重置计数
     * </p>
     *
     * @param nbtItem NBT 物品包装器
     * @return 更新后的保底数据
     */
    @NotNull
    public GuaranteeData recordSuccess(@NotNull NBTItem nbtItem) {
        GuaranteeData data = GuaranteeData.fromItem(nbtItem.getItem());
        if (data == null) {
            data = new GuaranteeData();
        }
        data.applyExpiration(expireHours);
        if (resetOnSuccess) {
            data.resetFails();
        }
        data.applyToItem(nbtItem);
        return data;
    }

    /**
     * 获取物品的连续失败次数
     *
     * @param item 物品
     * @return 连续失败次数，物品无效返回 0
     */
    public int getConsecutiveFails(@Nullable ItemStack item) {
        GuaranteeData data = getData(item);
        return data != null ? data.getConsecutiveFails() : 0;
    }

    /**
     * 获取距离保底还需要的失败次数
     *
     * @param item 物品
     * @return 剩余失败次数，已达保底返回 0
     */
    public int getRemainingFails(@Nullable ItemStack item) {
        int current = getConsecutiveFails(item);
        return Math.max(0, threshold - current);
    }
}
