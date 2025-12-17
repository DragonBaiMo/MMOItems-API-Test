package net.Indyuce.mmoitems.stat;

import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.SupportedNBTTagValues;
import io.lumine.mythic.lib.api.util.AltChar;
import io.lumine.mythic.lib.api.util.ui.FriendlyFeedbackCategory;
import io.lumine.mythic.lib.api.util.ui.FriendlyFeedbackProvider;
import io.lumine.mythic.lib.api.util.ui.PlusMinusPercent;
import io.lumine.mythic.lib.gson.Gson;
import io.lumine.mythic.lib.gson.JsonArray;
import io.lumine.mythic.lib.gson.JsonSyntaxException;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.edition.StatEdition;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.api.item.mmoitem.ReadMMOItem;
import net.Indyuce.mmoitems.api.util.message.FFPMMOItems;
import net.Indyuce.mmoitems.gui.edition.EditionInventory;
import net.Indyuce.mmoitems.stat.category.StatCategory;
import net.Indyuce.mmoitems.stat.data.DistanceBonusTableData;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import net.Indyuce.mmoitems.stat.data.type.UpgradeInfo;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import net.Indyuce.mmoitems.stat.type.Upgradable;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Optional;

/**
 * 距离伤害加成表，使用“距离:百分比”多行格式，避免为每个距离注册独立属性 ID。
 */
public final class DistanceBonusTableStat extends ItemStat<DistanceBonusTableData, DistanceBonusTableData> implements Upgradable {

    public static final String ID = "DISTANCE_BONUS_TABLE";
    private static final double DEFAULT_TOLERANCE = 1.0D;

    public DistanceBonusTableStat() {
        super(ID, Material.TARGET, "距离伤害加成表",
                new String[]{"格式: 距离:百分比，可多行", "示例: 5:10 代表 5 格内 +10% 伤害"}, new String[]{"!block", "all"});
        setCategory(StatCategory.TEMPLATE_OPTION);
        setAliases(new String[]{ID.toLowerCase()});
    }

    /**
     * 判断距离是否匹配。
     *
     * @param configured 配置距离
     * @param actual     实际距离
     * @param tolerance  误差容忍
     */
    public static boolean isDistanceMatch(double configured, double actual, double tolerance) {
        double delta = Math.abs(configured - actual);
        return delta <= (tolerance <= 0 ? DEFAULT_TOLERANCE : tolerance);
    }

    /**
     * 解析字符串列表为数据对象。
     */
    public DistanceBonusTableData parseLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return new DistanceBonusTableData();
        }
        List<Map.Entry<Double, Double>> result = new ArrayList<>();
        for (String raw : lines) {
            if (raw == null) {
                continue;
            }
            String[] parts = raw.replace(" ", "").split(":");
            if (parts.length != 2) {
                continue;
            }
            Double distance = safeParse(parts[0]);
            Double percent = safeParse(parts[1]);
            if (distance == null || percent == null || distance <= 0) {
                continue;
            }
            result.add(Map.entry(distance, percent));
        }
        return new DistanceBonusTableData(result);
    }

    @Override
    public @NotNull DistanceBonusTableData whenInitialized(Object object) {
        if (object instanceof List<?>) {
            List<?> list = (List<?>) object;
            List<String> cast = new ArrayList<>();
            for (Object o : list) {
                cast.add(String.valueOf(o));
            }
            return parseLines(cast);
        }
        return new DistanceBonusTableData();
    }

    @Override
    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull DistanceBonusTableData data) {
        // 写入 Lore + NBT，供玩家可见与战斗读取
        if (data == null || data.isEmpty()) {
            return;
        }

        List<String> parts = new ArrayList<>();
        for (Map.Entry<Double, Double> e : data.getEntries()) {
            double percent = e.getValue();
            String sign = percent >= 0 ? "+" : "";
            parts.add(format(e.getKey()) + "格:" + sign + format(percent) + "%");
        }
        String joined = String.join(", ", parts);
        String line = getGeneralStatFormat().replace("{value}", joined);
        item.getLore().insert(getPath(), line);

        item.addItemTag(getAppliedNBT(data));
    }

    @Override
    public @NotNull ArrayList<ItemTag> getAppliedNBT(@NotNull DistanceBonusTableData data) {
        JsonArray array = new JsonArray();
        for (Map.Entry<Double, Double> e : data.getEntries()) {
            array.add(format(e.getKey()) + ":" + format(e.getValue()));
        }
        ArrayList<ItemTag> tags = new ArrayList<>();
        tags.add(new ItemTag(getNBTPath(), array.toString()));
        return tags;
    }

    @Override
    public void whenClicked(@NotNull EditionInventory inv, @NotNull InventoryClickEvent event) {
        if (event.getAction() == InventoryAction.PICKUP_ALL) {
            new StatEdition(inv, this).enable("在聊天输入一行数值，格式 距离:百分比，例如 5:10");
        }
        if (event.getAction() == InventoryAction.PICKUP_HALF && inv.getEditedSection().contains(getPath())) {
            List<String> list = inv.getEditedSection().getStringList(getPath());
            if (list.isEmpty()) {
                return;
            }
            String last = list.get(list.size() - 1);
            list.remove(last);
            inv.getEditedSection().set(getPath(), list.isEmpty() ? null : list);
            inv.registerTemplateEdition();
            inv.getPlayer().sendMessage(MMOItems.plugin.getPrefix() + "已移除末尾条目: " + MythicLib.plugin.parseColors(last));
        }
    }

    @Override
    public void whenInput(@NotNull EditionInventory inv, @NotNull String message, Object... info) {
        List<String> list = inv.getEditedSection().contains(getPath()) ? inv.getEditedSection().getStringList(getPath()) : new ArrayList<>();
        list.add(message);
        inv.getEditedSection().set(getPath(), list);
        inv.registerTemplateEdition();
        inv.getPlayer().sendMessage(MMOItems.plugin.getPrefix() + getName() + " 已新增配置行。");
    }

    @Override
    public void whenLoaded(@NotNull ReadMMOItem mmoitem) {
        ArrayList<ItemTag> relevantTags = new ArrayList<>();
        if (mmoitem.getNBT().hasTag(getNBTPath())) {
            relevantTags.add(ItemTag.getTagAtPath(getNBTPath(), mmoitem.getNBT(), SupportedNBTTagValues.STRING));
        }
        StatData data = getLoadedNBT(relevantTags);
        if (data != null) {
            mmoitem.setData(this, data);
        }
    }

    @Override
    public @Nullable DistanceBonusTableData getLoadedNBT(@NotNull ArrayList<ItemTag> storedTags) {
        ItemTag tag = ItemTag.getTagAtPath(getNBTPath(), storedTags);
        if (tag == null || tag.getValue() == null) {
            return null;
        }
        if (!(tag.getValue() instanceof String)) {
            return null;
        }
        try {
            String[] array = new Gson().fromJson((String) tag.getValue(), String[].class);
            return parseLines(Arrays.asList(array));
        } catch (JsonSyntaxException | IllegalStateException ignored) {
            return null;
        }
    }

    @NotNull
    @Override
    public DistanceBonusTableData getClearStatData() {
        return new DistanceBonusTableData();
    }

    @Override
    public @NotNull UpgradeInfo loadUpgradeInfo(@Nullable Object obj) throws IllegalArgumentException {
        // 复用 PlusMinusPercent 解析：支持 +2, +10%, n0 等写法
        FriendlyFeedbackProvider ffp = new FriendlyFeedbackProvider(FFPMMOItems.get());
        PlusMinusPercent pmp = PlusMinusPercent.getFromString(String.valueOf(obj), ffp);
        if (pmp == null) {
            throw new IllegalArgumentException(ffp.getFeedbackOf(FriendlyFeedbackCategory.ERROR).get(0).forConsole(ffp.getPalette()));
        }
        return new DistanceBonusUpgradeInfo(pmp);
    }

    @Override
    public @NotNull DistanceBonusTableData apply(@NotNull StatData original, @NotNull UpgradeInfo info, int level) {
        if (!(original instanceof DistanceBonusTableData) || !(info instanceof DistanceBonusUpgradeInfo)) {
            return (DistanceBonusTableData) original;
        }
        DistanceBonusTableData data = (DistanceBonusTableData) original;
        DistanceBonusUpgradeInfo upg = (DistanceBonusUpgradeInfo) info;
        DistanceBonusTableData result = data.clone();
        PlusMinusPercent pmp = upg.pmp;
        int steps = Math.abs(level);
        boolean reverse = level < 0;
        for (int i = 0; i < steps; i++) {
            List<Map.Entry<Double, Double>> updated = new ArrayList<>();
            for (Map.Entry<Double, Double> e : result.getEntries()) {
                double val = e.getValue();
                val = reverse ? pmp.reverse(val) : pmp.apply(val);
                updated.add(Map.entry(e.getKey(), val));
            }
            result = new DistanceBonusTableData(updated);
        }
        return result;
    }

    @Override
    public void whenDisplayed(List<String> lore, Optional<DistanceBonusTableData> statData) {
        DistanceBonusTableData data = statData.orElse(null);
        if (data == null || data.isEmpty()) {
            lore.add("§7未配置任何距离加成。");
            return;
        }
        lore.add("§7距离加成列表:");
        for (Map.Entry<Double, Double> e : data.getEntries()) {
            lore.add(" §8- §f" + format(e.getKey()) + "格: §a+" + format(e.getValue()) + "%");
        }
    }

    private Double safeParse(String text) {
        try {
            return Double.parseDouble(text.toLowerCase(Locale.ROOT));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public static class DistanceBonusUpgradeInfo implements UpgradeInfo {
        final PlusMinusPercent pmp;

        public DistanceBonusUpgradeInfo(@NotNull PlusMinusPercent pmp) {
            this.pmp = pmp;
        }
    }
}
