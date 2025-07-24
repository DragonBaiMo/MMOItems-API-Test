package net.Indyuce.mmoitems.stat.block;

import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.api.util.NumericStatFormula;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public class BlockID extends DoubleStat {

    public BlockID() {
        super("BLOCK_ID", Material.STONE, "Block ID", new String[]{"This value determines which", "custom block will get placed."}, new String[]{"block"});
    }

    @Override
    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull DoubleData data) {
        super.whenApplied(item, data);
        item.getMeta().setCustomModelData((int) data.getValue() + 1000);
    }

    @Override
    public void whenPreviewed(@NotNull ItemStackBuilder item, @NotNull DoubleData currentData, @NotNull NumericStatFormula templateData) throws IllegalArgumentException {
        whenApplied(item, currentData);
    }
}
