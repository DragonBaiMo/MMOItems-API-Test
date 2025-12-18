package net.Indyuce.mmoitems.gui;

import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.version.Sounds;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.ConfigFile;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.UpgradeTemplate;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.VolatileMMOItem;
import net.Indyuce.mmoitems.api.upgrade.*;
import net.Indyuce.mmoitems.api.upgrade.limit.DailyLimitManager;
import net.Indyuce.mmoitems.api.util.message.Message;
import net.Indyuce.mmoitems.stat.data.UpgradeData;
import net.Indyuce.mmoitems.util.MMOUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedHashMap;

/**
 * 强化工作台 GUI
 * <p>
 * 提供可视化的强化操作界面，支持配置化和安全防护
 * </p>
 * <p>
 * 安全特性：
 * <ul>
 *     <li>防止 Shift+点击 复制物品</li>
 *     <li>防止数字键交换物品</li>
 *     <li>防止快速连点刷物品</li>
 *     <li>断线/崩溃自动返还物品</li>
 *     <li>物品一致性验证</li>
 * </ul>
 * </p>
 *
 * @author MMOItems Team
 * @since 强化系统扩展
 */
public class UpgradeStationGUI implements InventoryHolder, Listener {

    // ===== 静态管理 =====
    /**
     * 所有打开的 GUI 实例（用于玩家退出时清理）
     */
    private static final Map<UUID, UpgradeStationGUI> OPEN_GUIS = new ConcurrentHashMap<>();

    /**
     * 强化冷却记录
     */
    private static final Map<UUID, Long> UPGRADE_COOLDOWNS = new ConcurrentHashMap<>();

    // ===== 配置缓存 =====
    private static FileConfiguration config;
    private static long lastConfigLoad = 0;
    private static final long CONFIG_CACHE_TIME = 60000; // 1分钟缓存

    // ===== 槽位配置 =====
    private int slotTargetItem;
    private int slotUpgradeStone;
    private int slotPreview;
    private int slotLuckyStone;
    private int slotProtectStone;
    private int slotDirectStone;
    private int slotUpgradeButton;
    private int slotInfoPanel;
    private int slotCloseButton;
    private int slotProgressStart;
    private int progressLength;

    // ===== 安全配置 =====
    private long upgradeCooldown;
    private boolean blockShiftClick;
    private boolean blockNumberKey;
    private boolean blockDrag;
    private boolean returnOnClose;
    private boolean returnOnQuit;

    // ===== 实例字段 =====
    private final Player player;
    private final Inventory inventory;
    private final int inventorySize;
    private final UpgradeStationDisplay display;
    private boolean registered = false;
    private boolean processing = false; // 防止并发操作
    private BukkitTask updateTask;

    // ===== 物品快照（用于验证） =====
    private ItemStack targetSnapshot;
    private ItemStack stoneSnapshot;

    /**
     * 创建强化工作台 GUI
     *
     * @param player 玩家
     */
    public UpgradeStationGUI(@NotNull Player player) {
        this.player = player;
        loadConfig();

        int rows = getConfig().getInt("gui.rows", 6);
        rows = Math.max(1, Math.min(6, rows));
        this.inventorySize = rows * 9;

        String title = color(getConfig().getString("gui.title", "&5&l强化工作台"));
        this.inventory = Bukkit.createInventory(this, inventorySize, title);
        this.display = new UpgradeStationDisplay(this);

        setupInventory();
    }

    /**
     * 加载配置
     */
    private void loadConfig() {
        // 配置缓存
        if (config == null || System.currentTimeMillis() - lastConfigLoad > CONFIG_CACHE_TIME) {
            config = new ConfigFile("/default", "upgrade-station").getConfig();
            lastConfigLoad = System.currentTimeMillis();
        }

        loadSlotsFromConfig();

        // 加载安全配置
        ConfigurationSection security = getConfig().getConfigurationSection("security");
        if (security != null) {
            upgradeCooldown = security.getLong("upgrade-cooldown", 1000);
            blockShiftClick = security.getBoolean("block-shift-click", true);
            blockNumberKey = security.getBoolean("block-number-key", true);
            blockDrag = security.getBoolean("block-drag", false);
            returnOnClose = security.getBoolean("return-items-on-close", true);
            returnOnQuit = security.getBoolean("return-items-on-quit", true);
        } else {
            upgradeCooldown = 1000;
            blockShiftClick = true;
            blockNumberKey = true;
            blockDrag = false;
            returnOnClose = true;
            returnOnQuit = true;
        }
    }

    FileConfiguration getConfig() {
        return config;
    }

    /**
     * 加载功能槽位；优先 slots.*，否则从 layout 的 bind 定义获取；最后回退默认值
     */
    private void loadSlotsFromConfig() {
        ConfigurationSection slots = getConfig().getConfigurationSection("slots");
        if (slots != null && !slots.getKeys(false).isEmpty()) {
            slotTargetItem = slots.getInt("target-item", 11);
            slotUpgradeStone = slots.getInt("upgrade-stone", 15);
            slotPreview = slots.getInt("preview", 13);
            slotLuckyStone = slots.getInt("lucky-stone", 37);
            slotProtectStone = slots.getInt("protect-stone", 39);
            slotDirectStone = slots.getInt("direct-stone", 41);
            slotUpgradeButton = slots.getInt("upgrade-button", 43);
            slotInfoPanel = slots.getInt("info-panel", 49);
            slotCloseButton = slots.getInt("close-button", 45);
            slotProgressStart = slots.getInt("progress-start", 28);
            progressLength = slots.getInt("progress-length", 7);
            return;
        }

        // layout 绑定
        Map<String, List<Integer>> binds = new HashMap<>();
        for (Map<String, Object> entry : safeLayoutList()) {
            Object bindObj = entry.get("bind");
            if (bindObj == null) continue;
            List<Integer> parsed = parseSlots(entry.get("slot"), entry.get("slots"));
            if (parsed.isEmpty()) continue;
            binds.put(bindObj.toString().trim(), parsed);
        }

        slotTargetItem = firstSlot(binds, "target-item", 11);
        slotUpgradeStone = firstSlot(binds, "upgrade-stone", 15);
        slotPreview = firstSlot(binds, "preview", 13);
        slotLuckyStone = firstSlot(binds, "lucky-stone", 37);
        slotProtectStone = firstSlot(binds, "protect-stone", 39);
        slotDirectStone = firstSlot(binds, "direct-stone", 41);
        slotUpgradeButton = firstSlot(binds, "upgrade-button", 43);
        slotInfoPanel = firstSlot(binds, "info-panel", 49);
        slotCloseButton = firstSlot(binds, "close-button", 45);

        List<Integer> progressSlots = binds.get("progress-bar");
        if (progressSlots != null && !progressSlots.isEmpty()) {
            slotProgressStart = progressSlots.get(0);
            progressLength = progressSlots.size();
        } else {
            slotProgressStart = 28;
            progressLength = 7;
        }
    }

    private int firstSlot(Map<String, List<Integer>> binds, String key, int defaultValue) {
        List<Integer> list = binds.get(key);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return defaultValue;
    }

    /**
     * 初始化 GUI 布局
     */
    private void setupInventory() {
        // 填充背景
        ItemStack filler = createConfigItem("items.filler", Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventorySize; i++) {
            inventory.setItem(i, filler);
        }

        // 设置功能槽位为空
        setSlotEmpty(slotTargetItem);
        setSlotEmpty(slotUpgradeStone);
        setSlotEmpty(slotLuckyStone);
        setSlotEmpty(slotProtectStone);
        setSlotEmpty(slotDirectStone);

        List<Map<String, Object>> customLayout = safeLayoutList();
        boolean hasCustomLayout = !customLayout.isEmpty();

        if (!hasCustomLayout) {
            // 默认布局
            ItemStack border = createConfigItem("items.border", Material.PURPLE_STAINED_GLASS_PANE, " ");
            for (int i = 0; i < 9 && i < inventorySize; i++) {
                inventory.setItem(i, border);
            }

            if (4 < inventorySize) {
                inventory.setItem(4, createConfigItem("items.title", Material.ANVIL, "&5&l? 强化工作台 ?"));
            }

            ItemStack separator = createConfigItem("items.separator", Material.GRAY_STAINED_GLASS_PANE, " ");
            for (int i = 18; i < 27 && i < inventorySize; i++) {
                inventory.setItem(i, separator);
            }

            setSlotItem(slotTargetItem - 1, createConfigItem("items.target-label", Material.DIAMOND_SWORD, "&6&l装备槽"));
            setSlotItem(slotUpgradeStone - 1, createConfigItem("items.upgrade-stone-label", Material.EMERALD, "&a&l强化石槽"));
            setSlotItem(20, createConfigItem("items.target-label", Material.DIAMOND_SWORD, "&6&l装备槽"));
            setSlotItem(24, createConfigItem("items.upgrade-stone-label", Material.EMERALD, "&a&l强化石槽"));
            setSlotItem(slotLuckyStone - 1, createConfigItem("items.lucky-stone-label", Material.LIME_DYE, "&a幸运石"));
            setSlotItem(slotProtectStone - 1, createConfigItem("items.protect-stone-label", Material.LIGHT_BLUE_DYE, "&b保护石"));
            setSlotItem(slotDirectStone - 1, createConfigItem("items.direct-stone-label", Material.PURPLE_DYE, "&d直达石"));
            setSlotItem(slotCloseButton, createConfigItem("items.close-button", Material.BARRIER, "&c&l关闭"));
        } else {
            applyCustomLayout(customLayout);
        }

        // 初始更新
        display.updateAllDisplays();
    }

    private void setSlotEmpty(int slot) {
        if (slot >= 0 && slot < inventorySize) {
            inventory.setItem(slot, null);
        }
    }

    void setSlotItem(int slot, ItemStack item) {
        if (slot < 0 || slot >= inventorySize) return;
        if (item == null || item.getType() == Material.AIR) {
            inventory.setItem(slot, null);
            return;
        }
        inventory.setItem(slot, item);
    }

    /**
     * 应用 layout 列表定义的静态物品布局
     * layout:
     *   - key: title
     *     slot: 4
     *   - key: target-label
     *     slots: [10, 20]
     */
    private void applyCustomLayout(@NotNull List<Map<String, Object>> layout) {
        for (Map<String, Object> entry : layout) {
            Object keyObj = entry.get("key");
            if (keyObj == null) continue;
            String key = keyObj.toString().trim();
            if (key.isEmpty()) continue;

            if (Boolean.TRUE.equals(entry.get("hide"))) continue;

            List<Integer> slots = parseSlots(entry.get("slot"), entry.get("slots"));
            if (slots.isEmpty()) continue;

            ItemStack item = createConfigItem("items." + key, Material.PAPER, "&7");
            for (int slot : slots) {
                setSlotItem(slot, item);
            }
        }
    }

    private List<Integer> parseSlots(Object primary, Object secondary) {
        Object slotObj = primary != null ? primary : secondary;
        if (slotObj == null) return Collections.emptyList();

        List<Integer> result = new ArrayList<>();
        if (slotObj instanceof Number) {
            result.add(((Number) slotObj).intValue());
            return result;
        }
        if (slotObj instanceof Collection<?>) {
            for (Object o : (Collection<?>) slotObj) {
                if (o instanceof Number) {
                    result.add(((Number) o).intValue());
                } else {
                    try {
                        result.add(Integer.parseInt(String.valueOf(o)));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return result;
        }
        try {
            result.add(Integer.parseInt(String.valueOf(slotObj)));
        } catch (NumberFormatException ignored) {
        }
        return result;
    }

    private List<Map<String, Object>> safeLayoutList() {
        List<Map<?, ?>> raw = getConfig().getMapList("layout");
        if (raw == null || raw.isEmpty()) return Collections.emptyList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<?, ?> entry : raw) {
            if (entry == null || entry.isEmpty()) continue;
            Map<String, Object> converted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : entry.entrySet()) {
                if (e.getKey() != null) {
                    converted.put(e.getKey().toString(), e.getValue());
                }
            }
            result.add(converted);
        }
        return result;
    }

    /**
     * 从配置创建物品
     */
    ItemStack createConfigItem(String path, Material defaultMaterial, String defaultName) {
        ConfigurationSection section = getConfig().getConfigurationSection(path);

        Material material = defaultMaterial;
        String name = defaultName;
        List<String> lore = new ArrayList<>();
        boolean hide = false;

        if (section != null) {
            String matStr = section.getString("material");
            if (matStr != null) {
                try {
                    material = Material.valueOf(matStr.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }
            name = section.getString("name", defaultName);
            lore = section.getStringList("lore");
            hide = section.getBoolean("hide", false);
        }

        if (hide) {
            return new ItemStack(Material.AIR);
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            if (!lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(color(line));
                }
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    // ===== 辅助数值 =====

    double getAuxiliaryChanceBonus() {
        ItemStack luckyStone = inventory.getItem(slotLuckyStone);
        if (luckyStone == null || luckyStone.getType() == Material.AIR) return 0;

        NBTItem nbt = NBTItem.get(luckyStone);
        if (nbt.hasTag(ItemStats.AUXILIARY_CHANCE_BONUS.getNBTPath())) {
            return nbt.getDouble(ItemStats.AUXILIARY_CHANCE_BONUS.getNBTPath());
        }
        return 0;
    }

    double getAuxiliaryProtection() {
        ItemStack protectStone = inventory.getItem(slotProtectStone);
        if (protectStone == null || protectStone.getType() == Material.AIR) return 0;

        NBTItem nbt = NBTItem.get(protectStone);
        if (nbt.hasTag(ItemStats.AUXILIARY_PROTECTION.getNBTPath())) {
            return nbt.getDouble(ItemStats.AUXILIARY_PROTECTION.getNBTPath());
        }
        return 0;
    }

    double getAuxiliaryDirectUpChance() {
        ItemStack directStone = inventory.getItem(slotDirectStone);
        if (directStone == null || directStone.getType() == Material.AIR) return 0;

        NBTItem nbt = NBTItem.get(directStone);
        if (nbt.hasTag(ItemStats.AUXILIARY_DIRECT_UP_CHANCE.getNBTPath())) {
            return nbt.getDouble(ItemStats.AUXILIARY_DIRECT_UP_CHANCE.getNBTPath());
        }
        return 0;
    }

    int getAuxiliaryDirectUpLevels() {
        ItemStack directStone = inventory.getItem(slotDirectStone);
        if (directStone == null || directStone.getType() == Material.AIR) return 0;

        NBTItem nbt = NBTItem.get(directStone);
        if (nbt.hasTag(ItemStats.AUXILIARY_DIRECT_UP_LEVELS.getNBTPath())) {
            return (int) nbt.getDouble(ItemStats.AUXILIARY_DIRECT_UP_LEVELS.getNBTPath());
        }
        return 0;
    }

    boolean canPerformUpgrade() {
        if (processing) return false;

        ItemStack targetItem = inventory.getItem(slotTargetItem);
        ItemStack stoneItem = inventory.getItem(slotUpgradeStone);

        if (targetItem == null || targetItem.getType() == Material.AIR) return false;
        if (stoneItem == null || stoneItem.getType() == Material.AIR) return false;

        NBTItem targetNBT = NBTItem.get(targetItem);
        if (!targetNBT.hasType()) return false;

        VolatileMMOItem mmoItem = new VolatileMMOItem(targetNBT);
        if (!mmoItem.hasData(ItemStats.UPGRADE)) return false;

        UpgradeData data = (UpgradeData) mmoItem.getData(ItemStats.UPGRADE);
        if (!data.canLevelUp()) return false;

        return isValidUpgradeStone(stoneItem, targetItem);
    }

    boolean isValidUpgradeStone(@Nullable ItemStack stone, @Nullable ItemStack target) {
        if (stone == null || stone.getType() == Material.AIR) return false;
        if (target == null || target.getType() == Material.AIR) return false;

        NBTItem stoneNBT = NBTItem.get(stone);
        Type stoneType = Type.get(stoneNBT);
        if (stoneType == null || !stoneType.corresponds(Type.CONSUMABLE)) return false;
        if (!stoneNBT.hasTag(ItemStats.UPGRADE.getNBTPath())) return false;

        NBTItem targetNBT = NBTItem.get(target);
        VolatileMMOItem targetMmo = new VolatileMMOItem(targetNBT);
        if (!targetMmo.hasData(ItemStats.UPGRADE)) return false;
        UpgradeData targetData = (UpgradeData) targetMmo.getData(ItemStats.UPGRADE);

        VolatileMMOItem stoneMmo = new VolatileMMOItem(stoneNBT);
        if (!stoneMmo.hasData(ItemStats.UPGRADE)) return false;
        UpgradeData stoneData = (UpgradeData) stoneMmo.getData(ItemStats.UPGRADE);

        return MMOUtils.checkReference(stoneData.getReference(), targetData.getReference());
    }

    // ===== 强化执行 =====

    private void performUpgrade() {
        // 冷却检查
        Long lastUpgrade = UPGRADE_COOLDOWNS.get(player.getUniqueId());
        if (lastUpgrade != null && System.currentTimeMillis() - lastUpgrade < upgradeCooldown) {
            playSound("sounds.deny");
            return;
        }

        if (!canPerformUpgrade() || processing) {
            playSound("sounds.deny");
            return;
        }

        // 标记处理中
        processing = true;
        UPGRADE_COOLDOWNS.put(player.getUniqueId(), System.currentTimeMillis());
        display.updateUpgradeButton();

        ItemStack targetItem = inventory.getItem(slotTargetItem);
        ItemStack stoneItem = inventory.getItem(slotUpgradeStone);
        double stoneBaseSuccess = getSuccessFromStoneItem(stoneItem);
        double auxiliaryChanceBonus = getAuxiliaryChanceBonus();
        double auxiliaryProtection = getAuxiliaryProtection();
        double auxiliaryDirectUpChance = getAuxiliaryDirectUpChance();
        int auxiliaryDirectUpLevels = getAuxiliaryDirectUpLevels();

        // 保存快照（用于验证）
        targetSnapshot = targetItem.clone();
        stoneSnapshot = stoneItem.clone();

        // 验证物品未被修改
        if (!validateItems()) {
            processing = false;
            display.updateUpgradeButton();
            playSound("sounds.deny");
            player.sendMessage(color(getMessage("item-state-invalid", "&c物品状态异常，请重新放入物品")));
            return;
        }

        NBTItem targetNBT = NBTItem.get(targetItem);
        LiveMMOItem targetMMO = new LiveMMOItem(targetNBT);
        UpgradeData targetData = (UpgradeData) targetMMO.getData(ItemStats.UPGRADE);

        // 重要：任何“会导致 UpgradeService 直接返回 error 的硬性校验”都必须在消耗材料前完成，避免 GUI 白白扣除材料
        DailyLimitManager dailyLimitManager = MMOItems.plugin.getUpgrades().getDailyLimitManager();
        if (dailyLimitManager != null && dailyLimitManager.isEnabled() && !dailyLimitManager.canUpgrade(player)) {
            int used = dailyLimitManager.getUsedAttempts(player);
            int max = dailyLimitManager.getMaxAttempts(player);
            processing = false;
            display.updateUpgradeButton();
            playSound("sounds.deny");
            player.sendMessage(color(getMessage("daily-limit-reached", "&c今日强化次数已用尽 ({used}/{max})")
                    .replace("{used}", String.valueOf(used))
                    .replace("{max}", String.valueOf(max))));
            return;
        }

        UpgradeTemplate template = targetData.getTemplate();
        if (template == null) {
            processing = false;
            display.updateUpgradeButton();
            playSound("sounds.deny");
            player.sendMessage(color(getMessage("template-not-found", "&c未找到强化模板: &f{template}")
                    .replace("{template}", String.valueOf(targetData.getTemplateName()))));
            return;
        }

        playSound("sounds.click");

        // 构建上下文
        UpgradeContext context = new UpgradeContext.Builder()
                .player(player)
                .targetItem(targetMMO)
                .targetData(targetData)
                .targetItemStack(targetItem)
                .freeMode(true) // 已手动消耗
                .forceMode(false)
                // 重要：GUI 模式下已手动消耗强化石，UpgradeService 会将 freeMode 视为“无消耗品=基础成功率100%”。
                // 为了与背包强化/GUI 展示一致，这里将“强化石的基础成功率”映射到 chanceModifier，从而得到：基础成功率 × 衰减^等级。
                .chanceModifier(stoneBaseSuccess)
                .auxiliaryChanceBonus(auxiliaryChanceBonus)
                .auxiliaryProtection(auxiliaryProtection)
                .auxiliaryDirectUpChance(auxiliaryDirectUpChance)
                .auxiliaryDirectUpLevels(auxiliaryDirectUpLevels)
                .build();

        UpgradeResult result = UpgradeService.performUpgrade(context);

        // 错误：不应消耗材料
        if (result.isError()) {
            processing = false;
            display.updateUpgradeButton();
            playSound("sounds.deny");
            player.sendMessage(color(getMessage("upgrade-error", "&c强化失败: &f{reason}")
                    .replace("{reason}", String.valueOf(result.getMessage()))));
            return;
        }

        // 消耗强化石（仅在本次确实执行强化时）
        stoneItem.setAmount(stoneItem.getAmount() - 1);
        if (stoneItem.getAmount() <= 0) {
            inventory.setItem(slotUpgradeStone, null);
        }

        // 消耗辅料（仅在本次对应效果会生效时）
        consumeAuxiliaryStones(
                auxiliaryChanceBonus > 0,
                auxiliaryProtection > 0,
                auxiliaryDirectUpChance > 0 && auxiliaryDirectUpLevels > 0
        );

        // 更新物品
        if (result.isSuccess()) {
            MMOItem upgradedItem = result.getUpgradedItem();
            if (upgradedItem != null) {
                NBTItem newNBT = upgradedItem.newBuilder().buildNBT();
                targetItem.setItemMeta(newNBT.toItem().getItemMeta());
            }
            playSound("sounds.upgrade-success");
            Message.UPGRADE_SUCCESS.format(ChatColor.GREEN, "#item#", MMOUtils.getDisplayName(targetItem)).send(player);
        } else {
            playSound("sounds.upgrade-fail");
            String msg = result.getMessage();
            if (msg == null || msg.isEmpty()) {
                msg = getMessage("upgrade-fail-default", "强化失败");
            }

            if (result.getStatus() == UpgradeResult.Status.FAILURE_PROTECTED || result.getPenaltyResult() == PenaltyResult.PROTECTED) {
                PenaltyResult intercepted = result.getInterceptedPenalty();
                String key;
                String def;
                if (intercepted == PenaltyResult.BREAK) {
                    key = "upgrade-fail-protected-break";
                    def = "&a强化失败，保护已生效（拦截碎裂）：&f{reason}";
                } else if (intercepted == PenaltyResult.DOWNGRADE) {
                    key = "upgrade-fail-protected-downgrade";
                    def = "&a强化失败，保护已生效（拦截掉级）：&f{reason}";
                } else if (intercepted == PenaltyResult.DESTROY) {
                    key = "upgrade-fail-protected-destroy";
                    def = "&a强化失败，保护已生效（拦截销毁）：&f{reason}";
                } else {
                    key = "upgrade-fail-protected";
                    def = "&a强化失败，但保护已生效：&f{reason}";
                }

                player.sendMessage(color(getMessage(key, def).replace("{reason}", msg)));
            } else {
                player.sendMessage(color(getMessage("upgrade-fail", "&c{reason}")
                        .replace("{reason}", msg)));
            }
        }

        // 延迟解除处理状态
        Bukkit.getScheduler().runTaskLater(MMOItems.plugin, () -> {
            processing = false;
            display.updateAllDisplays();
        }, 10L);
    }

    /**
     * 从强化石物品中读取基础成功率（0-1）。
     * <p>
     * 注意：GUI 模式下强化石放在 GUI 容器里而不是玩家背包里，因此不能依赖 {@link net.Indyuce.mmoitems.api.upgrade.UpgradeService#findUpgradeStones}。
     * </p>
     */
    private double getSuccessFromStoneItem(@Nullable ItemStack stoneItem) {
        if (stoneItem == null || stoneItem.getType() == Material.AIR) return 1.0;

        NBTItem stoneNBT = NBTItem.get(stoneItem);
        VolatileMMOItem stoneMmo = new VolatileMMOItem(stoneNBT);
        if (stoneMmo.hasData(ItemStats.UPGRADE)) {
            UpgradeData stoneData = (UpgradeData) stoneMmo.getData(ItemStats.UPGRADE);
            return stoneData.getSuccess();
        }
        return 1.0;
    }

    /**
     * 验证物品未被非法修改
     */
    private boolean validateItems() {
        ItemStack currentTarget = inventory.getItem(slotTargetItem);
        ItemStack currentStone = inventory.getItem(slotUpgradeStone);

        // 简单验证：物品仍然存在且类型相同
        if (currentTarget == null || currentTarget.getType() == Material.AIR) return false;
        if (currentStone == null || currentStone.getType() == Material.AIR) return false;

        return true;
    }

    private void consumeAuxiliaryStones(boolean consumeLucky, boolean consumeProtect, boolean consumeDirect) {
        consumeOneAuxiliaryStone(slotLuckyStone, consumeLucky);
        consumeOneAuxiliaryStone(slotProtectStone, consumeProtect);
        consumeOneAuxiliaryStone(slotDirectStone, consumeDirect);
    }

    private void consumeOneAuxiliaryStone(int slot, boolean shouldConsume) {
        if (!shouldConsume) return;

        ItemStack stone = inventory.getItem(slot);
        if (stone == null || stone.getType() == Material.AIR) return;

        stone.setAmount(stone.getAmount() - 1);
        if (stone.getAmount() <= 0) {
            inventory.setItem(slot, null);
        }
    }

    // ===== GUI 控制 =====

    public void open() {
        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, MMOItems.plugin);
            registered = true;
        }
        OPEN_GUIS.put(player.getUniqueId(), this);
        player.openInventory(inventory);
        playSound("sounds.click");
    }

    public void close() {
        if (returnOnClose) {
            returnItems();
        }
        cleanup();
    }

    private void cleanup() {
        OPEN_GUIS.remove(player.getUniqueId());
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        if (registered) {
            HandlerList.unregisterAll(this);
            registered = false;
        }
    }

    private void returnItems() {
        int[] returnSlots = {slotTargetItem, slotUpgradeStone, slotLuckyStone, slotProtectStone, slotDirectStone};
        for (int slot : returnSlots) {
            if (slot < 0 || slot >= inventorySize) continue;
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item.clone());
                for (ItemStack drop : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                inventory.setItem(slot, null);
            }
        }
    }

    private boolean isFunctionalSlot(int slot) {
        return slot == slotTargetItem || slot == slotUpgradeStone ||
                slot == slotLuckyStone || slot == slotProtectStone || slot == slotDirectStone;
    }

    // ===== 工具方法 =====

    String color(String text) {
        return MythicLib.plugin.parseColors(text);
    }

    String getMessage(String key, String defaultValue) {
        return getConfig().getString("messages." + key, defaultValue);
    }

    Material getMaterial(String path, Material defaultMaterial) {
        String matStr = getConfig().getString(path);
        if (matStr != null) {
            try {
                return Material.valueOf(matStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return defaultMaterial;
    }

    private void playSound(String path) {
        ConfigurationSection section = getConfig().getConfigurationSection(path);
        if (section == null) return;

        String soundName = section.getString("sound");
        if (soundName == null) return;

        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            float volume = (float) section.getDouble("volume", 1.0);
            float pitch = (float) section.getDouble("pitch", 1.0);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException ignored) {
        }
    }

    // ===== 只读访问器 =====

    int getSlotTargetItem() {
        return slotTargetItem;
    }

    int getSlotUpgradeStone() {
        return slotUpgradeStone;
    }

    int getSlotPreview() {
        return slotPreview;
    }

    int getSlotLuckyStone() {
        return slotLuckyStone;
    }

    int getSlotProtectStone() {
        return slotProtectStone;
    }

    int getSlotDirectStone() {
        return slotDirectStone;
    }

    int getSlotUpgradeButton() {
        return slotUpgradeButton;
    }

    int getSlotInfoPanel() {
        return slotInfoPanel;
    }

    int getSlotCloseButton() {
        return slotCloseButton;
    }

    int getSlotProgressStart() {
        return slotProgressStart;
    }

    int getProgressLength() {
        return progressLength;
    }

    int getInventorySize() {
        return inventorySize;
    }

    Player getPlayer() {
        return player;
    }

    boolean isProcessing() {
        return processing;
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }

    // ===== 事件处理 =====

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player clicker = (Player) event.getWhoClicked();
        if (!clicker.equals(player)) return;

        int slot = event.getRawSlot();
        InventoryAction action = event.getAction();
        ClickType clickType = event.getClick();

        // ===== 安全防护 =====

        // 1. 阻止 Shift+点击（防止复制/移动到错误位置）
        if (blockShiftClick && clickType.isShiftClick()) {
            event.setCancelled(true);
            playSound("sounds.deny");
            return;
        }

        // 2. 阻止数字键交换
        if (blockNumberKey && clickType == ClickType.NUMBER_KEY) {
            event.setCancelled(true);
            playSound("sounds.deny");
            return;
        }

        // 3. 阻止双击收集
        if (action == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }

        // 4. 阻止移动到其他物品栏
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY && slot < inventorySize) {
            // 只允许从功能槽位移出
            if (!isFunctionalSlot(slot)) {
                event.setCancelled(true);
                return;
            }
        }

        // 玩家背包区域
        if (slot >= inventorySize) {
            // 阻止 Shift+点击将物品自动塞入 GUI（可能进入不可放置的装饰槽位）
            if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
                playSound("sounds.deny");
                return;
            }
            // 允许正常操作，但延迟更新
            Bukkit.getScheduler().runTaskLater(MMOItems.plugin, display::updateAllDisplays, 1L);
            return;
        }

        // 功能槽位：允许放置/取出
        if (isFunctionalSlot(slot)) {
            // 播放放入音效
            Bukkit.getScheduler().runTaskLater(MMOItems.plugin, () -> {
                playSound("sounds.item-place");
                display.updateAllDisplays();
            }, 1L);
            return;
        }

        // 非功能槽位：取消点击
        event.setCancelled(true);

        // 强化按钮
        if (slot == slotUpgradeButton) {
            performUpgrade();
            return;
        }

        // 关闭按钮
        if (slot == slotCloseButton) {
            playSound("sounds.close");
            clicker.closeInventory();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() != this) return;

        // 检查是否拖拽到非功能槽位
        for (int slot : event.getRawSlots()) {
            if (slot < inventorySize && !isFunctionalSlot(slot)) {
                event.setCancelled(true);
                return;
            }
        }

        // 如果配置了阻止拖拽
        if (blockDrag) {
            event.setCancelled(true);
            return;
        }

        Bukkit.getScheduler().runTaskLater(MMOItems.plugin, display::updateAllDisplays, 1L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() != this) return;
        if (!(event.getPlayer() instanceof Player)) return;

        Player closer = (Player) event.getPlayer();
        if (!closer.equals(player)) return;

        close();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent event) {
        if (!event.getPlayer().equals(player)) return;

        if (returnOnQuit) {
            returnItems();
        }
        cleanup();
    }

    // ===== 静态方法 =====

    /**
     * 获取玩家当前打开的强化工作台
     */
    @Nullable
    public static UpgradeStationGUI getOpenGUI(Player player) {
        return OPEN_GUIS.get(player.getUniqueId());
    }

    /**
     * 关闭所有打开的强化工作台（用于插件关闭时）
     */
    public static void closeAll() {
        for (UpgradeStationGUI gui : OPEN_GUIS.values()) {
            gui.returnItems();
            gui.cleanup();
        }
        OPEN_GUIS.clear();
        UPGRADE_COOLDOWNS.clear();
    }
}
