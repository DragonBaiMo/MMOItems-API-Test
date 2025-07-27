# 珠宝石状态保持修复方案

本次修复旨在解决连续拆卸并重新镶嵌同一颗宝石时，其属性值发生变化或累加的问题。主要修改点如下：

1. **在 `GemstoneData` 中保存原始宝石物品**
   - 新增字段 `storedItem`，通过 `BukkitObjectOutputStream` 将宝石 `ItemStack` 序列化为 Base64 字符串保存。
   - 在创建 `GemstoneData` 时，直接保存插入时宝石的完整 NBT 数据，确保其后续可以被完全重建。

2. **修改 `Gemstone.applyOntoItem`**
   - 创建 `GemstoneData` 时调用 `gemStoneMMOItem.getNBT().getItem()` 获取原始物品，保证保存的就是玩家当前插入的宝石，而非根据模板重新生成的物品。

3. **调整宝石提取逻辑**
   - `MMOItem.extractGemstones` 与 `extractGemstone` 在还原宝石时优先使用 `GemstoneData` 中存储的物品数据。若数据缺失，则退回到根据类型与ID重新生成。

通过以上措施，宝石在反复拆装过程中始终保持其原始属性，不会因重新计算而发生改变。


---

```md
1. In `MythicMobsCompatibility.reloadFactionStats()`, invoke `MMOItems.plugin.getStats().reload(false)` immediately after calling `MythicMobsLoadHook.registerFactionStats(false)` to load language data for the new stats.
2. Verify that unsocketing gems with faction-damage stats after a MythicMobs reload no longer logs “Cannot invoke 'String.replace' because 'format' is null”.
3. Ensure normal startup still loads faction-damage stats correctly.```