package net.Indyuce.mmoitems.api.upgrade.guarantee;

import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 物品强化保底数据
 * <p>
 * 存储物品的连续强化失败次数，用于保底机制判定。
 * 数据通过物品 NBT 持久化存储，跟随物品转移。
 * </p>
 *
 * @author MMOItems Team
 * @since 强化系统扩展
 */
public class GuaranteeData {

    /**
     * NBT 存储键：连续失败次数
     */
    public static final String NBT_CONSECUTIVE_FAILS = "MMOITEMS_UPGRADE_CONSECUTIVE_FAILS";

    /**
     * NBT 存储键：最后强化时间戳
     */
    public static final String NBT_LAST_UPGRADE_TIME = "MMOITEMS_UPGRADE_LAST_TIME";

    private int consecutiveFails;
    private long lastUpgradeTime;

    /**
     * 创建新的保底数据（初始值）
     */
    public GuaranteeData() {
        this.consecutiveFails = 0;
        this.lastUpgradeTime = System.currentTimeMillis();
    }

    /**
     * 从现有值创建保底数据
     *
     * @param consecutiveFails 连续失败次数
     * @param lastUpgradeTime  最后强化时间
     */
    public GuaranteeData(int consecutiveFails, long lastUpgradeTime) {
        this.consecutiveFails = Math.max(0, consecutiveFails);
        this.lastUpgradeTime = lastUpgradeTime;
    }

    /**
     * 从物品 NBT 读取保底数据
     *
     * @param item 物品
     * @return 保底数据，如果物品无效返回 null
     */
    @Nullable
    public static GuaranteeData fromItem(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }

        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasTag(NBT_CONSECUTIVE_FAILS)) {
            return new GuaranteeData();
        }

        int fails = nbt.getInteger(NBT_CONSECUTIVE_FAILS);
        // 使用 Double 存储时间戳（NBTItem 不支持 Long）
        long time = nbt.hasTag(NBT_LAST_UPGRADE_TIME) ? (long) nbt.getDouble(NBT_LAST_UPGRADE_TIME) : System.currentTimeMillis();

        return new GuaranteeData(fails, time);
    }

    /**
     * 将保底数据写入物品 NBT
     *
     * @param nbtItem NBT 物品包装器
     */
    public void applyToItem(@NotNull NBTItem nbtItem) {
        nbtItem.addTag(new io.lumine.mythic.lib.api.item.ItemTag(NBT_CONSECUTIVE_FAILS, consecutiveFails));
        // 使用 Double 存储时间戳（NBTItem 不支持 Long）
        nbtItem.addTag(new io.lumine.mythic.lib.api.item.ItemTag(NBT_LAST_UPGRADE_TIME, (double) lastUpgradeTime));
    }

    /**
     * 获取连续失败次数
     *
     * @return 连续失败次数
     */
    public int getConsecutiveFails() {
        return consecutiveFails;
    }

    /**
     * 设置连续失败次数
     *
     * @param fails 失败次数
     */
    public void setConsecutiveFails(int fails) {
        this.consecutiveFails = Math.max(0, fails);
    }

    /**
     * 增加失败次数
     */
    public void incrementFails() {
        this.consecutiveFails++;
        this.lastUpgradeTime = System.currentTimeMillis();
    }

    /**
     * 重置失败次数（成功后调用）
     */
    public void resetFails() {
        this.consecutiveFails = 0;
        this.lastUpgradeTime = System.currentTimeMillis();
    }

    /**
     * 获取最后强化时间
     *
     * @return 最后强化时间戳（毫秒）
     */
    public long getLastUpgradeTime() {
        return lastUpgradeTime;
    }

    /**
     * 检查是否达到保底阈值
     *
     * @param threshold 保底阈值
     * @return 如果达到阈值返回 true
     */
    public boolean isGuaranteed(int threshold) {
        return consecutiveFails >= threshold;
    }

    @Override
    public String toString() {
        return "GuaranteeData{" +
                "consecutiveFails=" + consecutiveFails +
                ", lastUpgradeTime=" + lastUpgradeTime +
                '}';
    }
}
