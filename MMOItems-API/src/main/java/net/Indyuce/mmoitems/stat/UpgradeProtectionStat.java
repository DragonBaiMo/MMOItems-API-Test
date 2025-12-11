package net.Indyuce.mmoitems.stat;

import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.SupportedNBTTagValues;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.api.item.mmoitem.ReadMMOItem;
import net.Indyuce.mmoitems.stat.data.StringData;
import net.Indyuce.mmoitems.stat.type.StringStat;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * 强化保护属性，用于消耗品类型物品
 * <p>
 * 当物品携带此属性时，可以作为强化保护物品使用。
 * 属性值为保护标签字符串，需要与目标物品的 downgrade-protect-key 或 break-protect-key 匹配。
 * </p>
 * <p>
 * 配置示例：
 * <pre>
 * PROTECTION_SCROLL:
 *   base:
 *     material: PAPER
 *     name: '&e保护卷轴'
 *     upgrade-protection: "sword-protection"
 * </pre>
 * </p>
 */
public class UpgradeProtectionStat extends StringStat {

    public UpgradeProtectionStat() {
        super("UPGRADE_PROTECTION",
                Material.PAPER,
                "强化保护",
                new String[]{
                        "该物品可作为强化保护消耗品",
                        "匹配目标物品的保护标签",
                        "",
                        "当强化失败触发惩罚时",
                        "如果背包中有匹配的保护物品",
                        "将消耗一个以免除惩罚"
                },
                new String[]{"consumable"});
    }

    @Override
    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull StringData data) {
        // 添加 NBT 标签
        item.addItemTag(getAppliedNBT(data));

        // 不在物品描述中显示（可选：如果需要显示，取消注释下面的行）
        // item.getLore().insert(getPath(), getGeneralStatFormat().replace("{value}", data.toString()));
    }

    @NotNull
    @Override
    public ArrayList<ItemTag> getAppliedNBT(@NotNull StringData data) {
        ArrayList<ItemTag> tags = new ArrayList<>();
        tags.add(new ItemTag(getNBTPath(), data.toString()));
        return tags;
    }

    @Override
    public void whenLoaded(@NotNull ReadMMOItem mmoitem) {
        // 从 NBT 读取保护标签
        if (mmoitem.getNBT().hasTag(getNBTPath())) {
            ArrayList<ItemTag> relevantTags = new ArrayList<>();
            relevantTags.add(ItemTag.getTagAtPath(getNBTPath(), mmoitem.getNBT(), SupportedNBTTagValues.STRING));

            StringData data = getLoadedNBT(relevantTags);
            if (data != null) {
                mmoitem.setData(this, data);
            }
        }
    }

    @Nullable
    @Override
    public StringData getLoadedNBT(@NotNull ArrayList<ItemTag> storedTags) {
        ItemTag tag = ItemTag.getTagAtPath(getNBTPath(), storedTags);
        if (tag != null) {
            String value = (String) tag.getValue();
            if (value != null && !value.isEmpty()) {
                return new StringData(value);
            }
        }
        return null;
    }
}
