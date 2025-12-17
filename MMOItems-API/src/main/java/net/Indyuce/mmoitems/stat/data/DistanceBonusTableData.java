package net.Indyuce.mmoitems.stat.data;

import net.Indyuce.mmoitems.api.item.build.MMOItemBuilder;
import net.Indyuce.mmoitems.stat.data.random.RandomStatData;
import net.Indyuce.mmoitems.stat.data.type.Mergeable;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 距离加成表的 StatData 表示。
 */
public final class DistanceBonusTableData implements StatData, Mergeable<DistanceBonusTableData>, RandomStatData<DistanceBonusTableData> {

    private final List<Map.Entry<Double, Double>> entries;

    public DistanceBonusTableData() {
        this.entries = new ArrayList<>();
    }

    public DistanceBonusTableData(List<Map.Entry<Double, Double>> entries) {
        this.entries = new ArrayList<>(entries == null ? List.of() : entries);
    }

    public List<Map.Entry<Double, Double>> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    @Override
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Override
    public void mergeWith(@NotNull DistanceBonusTableData mergeable) {
        entries.addAll(mergeable.entries);
    }

    /**
     * 按倍率调整百分比值。
     */
    public DistanceBonusTableData scaled(double factor) {
        if (factor == 1D || entries.isEmpty()) {
            return this;
        }
        List<Map.Entry<Double, Double>> copy = new ArrayList<>(entries.size());
        for (Map.Entry<Double, Double> e : entries) {
            copy.add(Map.entry(e.getKey(), e.getValue() * factor));
        }
        return new DistanceBonusTableData(copy);
    }

    @Override
    public DistanceBonusTableData randomize(MMOItemBuilder builder) {
        return clone();
    }

    @NotNull
    @Override
    public DistanceBonusTableData clone() {
        return new DistanceBonusTableData(entries);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DistanceBonusTableData)) {
            return false;
        }
        DistanceBonusTableData data = (DistanceBonusTableData) obj;
        return entries.equals(data.entries);
    }

    @Override
    public int hashCode() {
        return entries.hashCode();
    }
}
