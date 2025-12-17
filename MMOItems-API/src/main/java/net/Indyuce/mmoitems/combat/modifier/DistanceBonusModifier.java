package net.Indyuce.mmoitems.combat.modifier;

import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.combat.CombatContext;
import net.Indyuce.mmoitems.combat.CombatModifier;
import net.Indyuce.mmoitems.stat.DistanceBonusTableStat;
import net.Indyuce.mmoitems.stat.data.DistanceBonusTableData;
import io.lumine.mythic.lib.gson.Gson;
import io.lumine.mythic.lib.gson.JsonSyntaxException;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Arrays;
import java.util.List;

/**
 * 距离加成表修正器：匹配距离后返回乘数。
 */
public final class DistanceBonusModifier implements CombatModifier {

    private static final double DEFAULT_TOLERANCE = 1.0D;

    @Override
    public String getName() {
        return "distance-bonus-table";
    }

    @Override
    public @Nullable Double apply(CombatContext ctx) {
        DistanceBonusTableData data = readDistanceBonus(ctx);
        if (data == null || data.isEmpty()) {
            return null;
        }
        double distance = ctx.getDistance();
        double totalBonus = 0D;
        for (Map.Entry<Double, Double> entry : data.getEntries()) {
            if (DistanceBonusTableStat.isDistanceMatch(entry.getKey(), distance, DEFAULT_TOLERANCE)) {
                totalBonus += entry.getValue();
            }
        }
        if (totalBonus == 0D) {
            return null;
        }
        return 1 + totalBonus / 100D;
    }

    @Nullable
    private DistanceBonusTableData readDistanceBonus(CombatContext ctx) {
        DistanceBonusTableStat stat = (DistanceBonusTableStat) ItemStats.DISTANCE_BONUS_TABLE;
        String raw = ctx.getWeapon().getString(stat.getNBTPath());
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            String[] arr = new Gson().fromJson(raw, String[].class);
            List<String> lines = Arrays.asList(arr);
            return stat.parseLines(lines);
        } catch (JsonSyntaxException | IllegalStateException ignored) {
            return null;
        }
    }
}
