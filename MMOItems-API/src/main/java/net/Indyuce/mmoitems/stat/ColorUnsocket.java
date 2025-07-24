package net.Indyuce.mmoitems.stat; // 请替换成你自己的包名

import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.version.Sounds;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.event.item.UnsocketGemStoneEvent;
import net.Indyuce.mmoitems.api.interaction.Consumable;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.VolatileMMOItem;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.api.util.message.Message;
import net.Indyuce.mmoitems.stat.data.GemSocketsData;
import net.Indyuce.mmoitems.stat.data.GemstoneData;
import net.Indyuce.mmoitems.stat.data.StringData;
import net.Indyuce.mmoitems.stat.type.ConsumableItemInteraction;
import net.Indyuce.mmoitems.stat.type.StringStat;
import net.Indyuce.mmoitems.util.MMOUtils;
import net.Indyuce.mmoitems.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ColorUnsocket extends StringStat implements ConsumableItemInteraction {

    public ColorUnsocket() {
        super("COLOR_UNSOCKET",
              Material.MUSHROOM_STEW,
              "指定颜色卸载",
              new String[] { "卸载指定颜色和数量的宝石。", "格式: '颜色:数量;颜色:数量...'", "若不写数量, 则卸载所有。" },
              new String[] { "consumable" });
    }

    @Override
    public boolean handleConsumableEffect(@NotNull InventoryClickEvent event, @NotNull PlayerData playerData, @NotNull Consumable consumable, @NotNull NBTItem target, Type targetType) {
        
        // --- V4.0 的所有前置检查依然有效 ---
        VolatileMMOItem consumableMMO = consumable.getMMOItem();
        if (!consumableMMO.hasData(this)) return false;

        if (targetType == null) return false;

        MMOItem targetVolatile = new VolatileMMOItem(target);
        if (!targetVolatile.hasData(ItemStats.GEM_SOCKETS)) return false;
        
        GemSocketsData gemData = (GemSocketsData) targetVolatile.getData(ItemStats.GEM_SOCKETS);
        if (gemData == null || gemData.getGemstones().isEmpty()) return false;
        
        // --- V5.0 全新逻辑开始 ---
        Player player = playerData.getPlayer();
        
        // 1. 解析我们的高级指令
        String rulesString = ((StringData) consumableMMO.getData(this)).getString();
        Map<String, Integer> unsocketRules = parseUnsocketRules(rulesString);

        if (unsocketRules.isEmpty()) return false; // 指令无效

        // 2. 准备操作，获取所有宝石
        LiveMMOItem liveTarget = new LiveMMOItem(target);
        List<Pair<GemstoneData, MMOItem>> allGemPairs = liveTarget.extractGemstones();

        // 触发一次总的事件
        UnsocketGemStoneEvent unsocketEvent = new UnsocketGemStoneEvent(playerData, consumableMMO, liveTarget);
        Bukkit.getServer().getPluginManager().callEvent(unsocketEvent);
        if (unsocketEvent.isCancelled()) return false;

        List<Pair<GemstoneData, MMOItem>> gemsToRemove = new ArrayList<>();
        
        // 3. 按规则筛选要移除的宝石
        for (Map.Entry<String, Integer> rule : unsocketRules.entrySet()) {
            String targetColor = rule.getKey();
            int amountToUnsocket = rule.getValue();

            // 筛选出当前颜色所有可用的宝石
            List<Pair<GemstoneData, MMOItem>> availableGemsOfColor = allGemPairs.stream()
                .filter(pair -> targetColor.equalsIgnoreCase(pair.getKey().getSocketColor()))
                .collect(Collectors.toList());
            
            // 确定实际要移除的数量
            int actualAmount = Math.min(availableGemsOfColor.size(), amountToUnsocket);

            // 添加到总的移除列表
            for (int i = 0; i < actualAmount; i++) {
                gemsToRemove.add(availableGemsOfColor.get(i));
            }
        }

        // 4. 如果没有找到任何可移除的宝石，则中止
        if (gemsToRemove.isEmpty()) {
            player.sendMessage("§c未能在物品上找到任何符合规则的宝石。");
            return false;
        }

        // 5. 执行批量卸载
        List<ItemStack> returnedItems = new ArrayList<>();
        for (Pair<GemstoneData, MMOItem> pair : gemsToRemove) {
            GemstoneData gemDataToRemove = pair.getKey();
            MMOItem gemMMOToReturn = pair.getValue();

            // 移除NBT
            liveTarget.removeGemStone(gemDataToRemove.getHistoricUUID(), gemDataToRemove.getSocketColor());
            // 收集返还的物品
            returnedItems.add(gemMMOToReturn.newBuilder().build());
        }

        // 6. 所有NBT操作完成后，最后更新一次物品
        event.setCurrentItem(liveTarget.newBuilder().build());

        // 7. 返还所有宝石，并发送总结信息
        player.getInventory().addItem(returnedItems.toArray(new ItemStack[0]));
        player.sendMessage("§a成功卸载了 §e" + gemsToRemove.size() + " §a颗宝石！");
        player.playSound(player.getLocation(), Sounds.BLOCK_ANVIL_LAND, 1, 2);

        return true;
    }

    /**
     * 解析我们自定义的 "颜色:数量;颜色:数量" 规则字符串.
     * @param rulesString 来自 yml 文件中的字符串
     * @return 一个 Map, Key 是颜色 (大写), Value 是数量.
     */
    private Map<String, Integer> parseUnsocketRules(String rulesString) {
        Map<String, Integer> rules = new HashMap<>();
        if (rulesString == null || rulesString.isEmpty()) {
            return rules;
        }

        // 按分号分割多个规则
        for (String rule : rulesString.split(";")) {
            if (rule.trim().isEmpty()) continue;
            
            String[] parts = rule.split(":");
            String color = parts[0].trim().toUpperCase();

            // 如果没有数量部分，默认为“全部” (用一个超大数表示)
            if (parts.length == 1) {
                rules.put(color, Integer.MAX_VALUE);
                continue;
            }

            // 如果有数量部分，尝试解析
            try {
                int amount = Integer.parseInt(parts[1].trim());
                if (amount > 0) {
                    rules.put(color, amount);
                }
            } catch (NumberFormatException e) {
                // 如果解析失败 (比如写了 "白色:abc"), 也视为“全部”
                rules.put(color, Integer.MAX_VALUE);
            }
        }
        return rules;
    }
}