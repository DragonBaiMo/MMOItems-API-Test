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
import net.Indyuce.mmoitems.api.upgrade.guarantee.GuaranteeManager;
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
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private FileConfiguration getConfig() {
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
        updateAllDisplays();
    }

    private void setSlotEmpty(int slot) {
        if (slot >= 0 && slot < inventorySize) {
            inventory.setItem(slot, null);
        }
    }

    private void setSlotItem(int slot, ItemStack item) {
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
    private ItemStack createConfigItem(String path, Material defaultMaterial, String defaultName) {
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

    // ===== 更新显示方法 =====

    /**
     * 更新所有显示内容
     */
    public void updateAllDisplays() {
        updatePreview();
        updateProgressBar();
        updateInfoPanel();
        updateUpgradeButton();
    }

    /**
     * 更新预览
     */
    private void updatePreview() {
        ItemStack targetItem = inventory.getItem(slotTargetItem);

        ItemStack previewItem;

        if (targetItem == null || targetItem.getType() == Material.AIR) {
            previewItem = createConfigItem("items.preview-waiting", Material.ARROW, "&f&l→ 强化 →");
        } else {
            NBTItem targetNBT = NBTItem.get(targetItem);
            if (!targetNBT.hasType()) {
                previewItem = createPreviewItem(Material.BARRIER, "&c非MMOItems物品", Arrays.asList("&7该物品不能强化"));
            } else {
                VolatileMMOItem mmoItem = new VolatileMMOItem(targetNBT);
                if (!mmoItem.hasData(ItemStats.UPGRADE)) {
                    previewItem = createPreviewItem(Material.BARRIER, "&c物品不可强化", Arrays.asList("&7该物品没有配置强化属性"));
                } else {
                    UpgradeData data = (UpgradeData) mmoItem.getData(ItemStats.UPGRADE);
                    previewItem = createDetailedPreview(targetItem, data);
                }
            }
        }

        setSlotItem(slotPreview, previewItem);
    }

    private ItemStack createPreviewItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(color(line));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createDetailedPreview(ItemStack targetItem, UpgradeData data) {
        int currentLevel = data.getLevel();
        int maxLevel = data.getMax();
        boolean atMax = maxLevel > 0 && currentLevel >= maxLevel;

        // 已达最高等级
        if (atMax) {
            List<String> lore = new ArrayList<>();
            lore.add("&7");
            lore.add(getMessage("item-name", "&f物品: {name}").replace("{name}", MMOUtils.getDisplayName(targetItem)));
            lore.add("&7");
            lore.add(getMessage("current-level", "&e当前等级: &f+{level}").replace("{level}", String.valueOf(currentLevel)));
            lore.add("&7");
            lore.add("&c已达到强化上限，无法继续强化");
            return createPreviewItem(Material.STRUCTURE_VOID, getMessage("preview-max-level", "&6&l已达最大等级"), lore);
        }

        // 尝试构建真实的强化后预览物品
        try {
            return buildUpgradedPreviewItem(targetItem, data, currentLevel, maxLevel);
        } catch (Exception e) {
            // 构建失败时回退到简单预览
            return createSimplePreview(targetItem, data, currentLevel, maxLevel);
        }
    }

    /**
     * 构建真实的强化后预览物品
     * <p>
     * 克隆当前物品，模拟强化到 +1，展示完整的强化后属性，
     * 并在属性行中直接嵌入变化标记（如 +0.8）
     * </p>
     */
    private ItemStack buildUpgradedPreviewItem(ItemStack targetItem, UpgradeData data, int currentLevel, int maxLevel) {
        NBTItem nbt = NBTItem.get(targetItem);

        // 1. 获取原始物品的 Lore（强化前）
        ItemMeta originalMeta = targetItem.getItemMeta();
        List<String> originalLore = (originalMeta != null && originalMeta.hasLore())
                ? new ArrayList<>(originalMeta.getLore()) : new ArrayList<>();

        // 2. 创建 LiveMMOItem 以读取所有属性
        LiveMMOItem originalItem = new LiveMMOItem(nbt);

        // 3. 克隆物品用于预览
        MMOItem previewMMO = originalItem.clone();

        // 4. 获取强化模板
        UpgradeTemplate template = data.getTemplate();
        if (template == null) {
            throw new RuntimeException("强化模板不存在: " + data.getTemplateName());
        }

        // 5. 模拟强化到 +1
        int targetLevel = currentLevel + 1;
        template.upgradeTo(previewMMO, targetLevel);

        // 6. 构建预览物品
        ItemStack preview = previewMMO.newBuilder().buildNBT().toItem();

        // 7. 获取强化后物品的 Lore
        ItemMeta previewMeta = preview.getItemMeta();
        if (previewMeta == null) {
            return preview;
        }

        List<String> upgradedLore = previewMeta.hasLore()
                ? new ArrayList<>(previewMeta.getLore()) : new ArrayList<>();

        // 8. 对比 Lore，在变化的属性行后面添加变化标记
        List<String> markedLore = injectChangeMarkers(originalLore, upgradedLore);

        // 9. 构建最终预览 Lore
        String originalName = previewMeta.hasDisplayName() ? previewMeta.getDisplayName() : MMOUtils.getDisplayName(targetItem);
        previewMeta.setDisplayName(color(getMessage("preview-title", "&e⚡ 强化预览 ⚡")));

        List<String> finalLore = new ArrayList<>();
        finalLore.add(color("&7"));
        finalLore.add(color(getMessage("preview-original-item", "&f原物品: {name}").replace("{name}", originalName)));
        finalLore.add(color("&7"));

        // 等级变化
        finalLore.add(color(getMessage("current-level", "&e当前等级: &f+{level}").replace("{level}", String.valueOf(currentLevel))));
        finalLore.add(color(getMessage("after-level", "&a强化后: &f+{level}").replace("{level}", String.valueOf(targetLevel))));
        if (maxLevel > 0) {
            finalLore.add(color(getMessage("max-level", "&7最大等级: &f+{level}").replace("{level}", String.valueOf(maxLevel))));
        }

        // 直达石效果
        double directChance = getAuxiliaryDirectUpChance();
        int directLevels = getAuxiliaryDirectUpLevels();
        if (directChance > 0 && directLevels > 0) {
            finalLore.add(color("&7"));
            finalLore.add(color(getMessage("direct-effect", "&d✦ 直达石效果:")));
            finalLore.add(color(getMessage("direct-chance", "&d  {chance}% 概率直接到 +{level}")
                    .replace("{chance}", String.format("%.0f", directChance))
                    .replace("{level}", String.valueOf(targetLevel + directLevels))));
        }

        // 分隔线 + 强化后完整属性（带变化标记）
        finalLore.add(color("&7"));
        finalLore.add(color(getMessage("preview-separator", "&8─────────────────")));
        finalLore.add(color(getMessage("preview-full-stats", "&7&o强化后完整属性:")));
        finalLore.addAll(markedLore);

        previewMeta.setLore(finalLore);
        preview.setItemMeta(previewMeta);

        return preview;
    }

    /**
     * 用于匹配 Lore 行中数值的正则表达式
     * 匹配格式如：▻ 7.8、: 100、+5%、-10% 等
     */
    private static final Pattern NUMBER_PATTERN = Pattern.compile("([▻:：]\\s*[+-]?)([\\d.]+)(%?)");

    /**
     * 对比原始 Lore 和强化后 Lore，在数值变化的行后面添加变化标记
     *
     * @param originalLore 原始物品 Lore
     * @param upgradedLore 强化后物品 Lore
     * @return 带有变化标记的 Lore
     */
    private List<String> injectChangeMarkers(List<String> originalLore, List<String> upgradedLore) {
        List<String> result = new ArrayList<>();

        // 检查是否启用变化标记
        boolean enabled = getConfig().getBoolean("change-marker.enabled", true);
        if (!enabled) {
            return new ArrayList<>(upgradedLore);
        }

        // 读取配置
        double minChange = getConfig().getDouble("change-marker.min-change", 0.01);

        // 构建原始 Lore 的数值映射（按行首文本作为键）
        Map<String, Double> originalValues = new LinkedHashMap<>();
        for (String line : originalLore) {
            String key = extractLineKey(line);
            Double value = extractFirstNumber(line);
            if (key != null && value != null) {
                originalValues.put(key, value);
            }
        }

        // 遍历强化后 Lore，对比并添加变化标记
        for (String line : upgradedLore) {
            String key = extractLineKey(line);
            Double newValue = extractFirstNumber(line);

            if (key != null && newValue != null && originalValues.containsKey(key)) {
                Double oldValue = originalValues.get(key);
                double diff = newValue - oldValue;

                // 如果有变化，在行尾添加变化标记
                if (Math.abs(diff) >= minChange) {
                    String changeMarker = formatChangeMarker(diff);
                    result.add(line + changeMarker);
                    continue;
                }
            }

            // 无变化或无法对比的行，直接添加
            result.add(line);
        }

        return result;
    }

    /**
     * 提取 Lore 行的键（用于匹配同一属性）
     * 取行首到第一个数值之前的文本（去除颜色代码后）
     */
    private String extractLineKey(String line) {
        if (line == null || line.isEmpty()) return null;

        // 去除颜色代码
        String stripped = ChatColor.stripColor(line);
        if (stripped == null || stripped.isEmpty()) return null;

        // 找到第一个数字的位置
        Matcher matcher = NUMBER_PATTERN.matcher(stripped);
        if (matcher.find()) {
            // 返回数字之前的部分作为键
            String key = stripped.substring(0, matcher.start()).trim();
            // 去除末尾的符号
            key = key.replaceAll("[▻:：\\s]+$", "").trim();
            return key.isEmpty() ? null : key;
        }

        return null;
    }

    /**
     * 从 Lore 行中提取第一个数值
     */
    private Double extractFirstNumber(String line) {
        if (line == null) return null;

        String stripped = ChatColor.stripColor(line);
        if (stripped == null) return null;

        Matcher matcher = NUMBER_PATTERN.matcher(stripped);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(2));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 格式化变化标记（从配置读取格式）
     */
    private String formatChangeMarker(double diff) {
        // 读取配置
        String format = getConfig().getString("change-marker.format", " &8({color}{sign}{value}&8)");
        String positiveColor = getConfig().getString("change-marker.positive-color", "&a");
        String negativeColor = getConfig().getString("change-marker.negative-color", "&c");
        int decimalPlaces = getConfig().getInt("change-marker.decimal-places", 1);

        // 确定颜色和符号
        String colorCode = diff > 0 ? positiveColor : negativeColor;
        String sign = diff > 0 ? "+" : "";

        // 格式化数值
        String valueFormat = "%." + decimalPlaces + "f";
        String value = String.format(valueFormat, diff);

        // 应用格式模板
        String result = format
                .replace("{color}", colorCode)
                .replace("{sign}", sign)
                .replace("{value}", value);

        return color(result);
    }

    /**
     * 简单预览（构建失败时的回退方案）
     */
    private ItemStack createSimplePreview(ItemStack targetItem, UpgradeData data, int currentLevel, int maxLevel) {
        List<String> lore = new ArrayList<>();
        lore.add("&7");

        String itemName = MMOUtils.getDisplayName(targetItem);
        lore.add(getMessage("item-name", "&f物品: {name}").replace("{name}", itemName));
        lore.add("&7");

        lore.add(getMessage("current-level", "&e当前等级: &f+{level}").replace("{level}", String.valueOf(currentLevel)));
        lore.add(getMessage("after-level", "&a强化后: &f+{level}").replace("{level}", String.valueOf(currentLevel + 1)));
        if (maxLevel > 0) {
            lore.add(getMessage("max-level", "&7最大等级: &f+{level}").replace("{level}", String.valueOf(maxLevel)));
        }

        // 直达石效果
        double directChance = getAuxiliaryDirectUpChance();
        int directLevels = getAuxiliaryDirectUpLevels();
        if (directChance > 0 && directLevels > 0) {
            lore.add("&7");
            lore.add(getMessage("direct-effect", "&d✦ 直达石效果:"));
            lore.add(getMessage("direct-chance", "&d  {chance}% 概率直接到 +{level}")
                    .replace("{chance}", String.format("%.0f", directChance))
                    .replace("{level}", String.valueOf(currentLevel + 1 + directLevels)));
        }

        lore.add("&7");
        lore.add("&8(无法生成完整预览)");

        return createPreviewItem(Material.NETHER_STAR, "&e&l强化预览", lore);
    }

    /**
     * 更新进度条
     */
    private void updateProgressBar() {
        double successRate = calculateActualSuccessRate();
        int filledSlots = (int) Math.round(successRate * progressLength);

        // 进度条标签
        setSlotItem(slotProgressStart - 1, createProgressLabel(successRate));

        // 进度条
        for (int i = 0; i < progressLength; i++) {
            int slot = slotProgressStart + i;
            if (slot >= inventorySize) break;

            Material material;
            String colorCode;

            if (i < filledSlots) {
                if (successRate >= 0.8) {
                    material = getMaterial("progress-bar.high.material", Material.LIME_STAINED_GLASS_PANE);
                    colorCode = getConfig().getString("progress-bar.high.color", "&a");
                } else if (successRate >= 0.5) {
                    material = getMaterial("progress-bar.medium.material", Material.YELLOW_STAINED_GLASS_PANE);
                    colorCode = getConfig().getString("progress-bar.medium.color", "&e");
                } else if (successRate >= 0.3) {
                    material = getMaterial("progress-bar.low.material", Material.ORANGE_STAINED_GLASS_PANE);
                    colorCode = getConfig().getString("progress-bar.low.color", "&6");
                } else {
                    material = getMaterial("progress-bar.danger.material", Material.RED_STAINED_GLASS_PANE);
                    colorCode = getConfig().getString("progress-bar.danger.color", "&c");
                }
            } else {
                material = getMaterial("progress-bar.empty.material", Material.WHITE_STAINED_GLASS_PANE);
                colorCode = getConfig().getString("progress-bar.empty.color", "&8");
            }

            ItemStack pane = new ItemStack(material);
            ItemMeta meta = pane.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color(colorCode + "█"));
                List<String> lore = new ArrayList<>();
                String rateMsg = getMessage("success-rate", "&7成功率: {color}{rate}%")
                        .replace("{color}", getSuccessColor(successRate))
                        .replace("{rate}", String.format("%.1f", successRate * 100));
                lore.add(color(rateMsg));
                meta.setLore(lore);
                pane.setItemMeta(meta);
            }
            inventory.setItem(slot, pane);
        }
    }

    private ItemStack createProgressLabel(double successRate) {
        ItemStack item = createConfigItem("items.progress-label", Material.EXPERIENCE_BOTTLE, "&f成功率");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String colorCode = getSuccessColor(successRate);
            meta.setDisplayName(color("&f成功率: " + colorCode + String.format("%.1f%%", successRate * 100)));

            List<String> lore = new ArrayList<>();
            lore.add(color("&7"));

            // 基础成功率
            double baseSuccess = getBaseSuccessFromStone();
            lore.add(color(getMessage("base-rate", "&7基础成功率: &f{rate}%")
                    .replace("{rate}", String.format("%.1f", baseSuccess * 100))));

            // 衰减后
            double decayedSuccess = getDecayedSuccessRate();
            if (decayedSuccess < baseSuccess - 0.001) {
                lore.add(color(getMessage("decayed-rate", "&7等级衰减后: &f{rate}%")
                        .replace("{rate}", String.format("%.1f", decayedSuccess * 100))));
            }

            // 幸运石加成
            double chanceBonus = getAuxiliaryChanceBonus();
            if (chanceBonus > 0) {
                lore.add(color(getMessage("lucky-bonus", "&a× 幸运石: &f×(1+{rate}%)")
                        .replace("{rate}", String.format("%.1f", chanceBonus))));
            }

            // 保底
            ItemStack targetItem = inventory.getItem(slotTargetItem);
            if (targetItem != null && targetItem.getType() != Material.AIR) {
                GuaranteeManager gm = MMOItems.plugin.getUpgrades().getGuaranteeManager();
                if (gm != null && gm.isEnabled()) {
                    int fails = gm.getConsecutiveFails(targetItem);
                    int threshold = gm.getThreshold();
                    if (fails > 0) {
                        lore.add(color("&7"));
                        lore.add(color(getMessage("guarantee-progress", "&6保底进度: &f{current}/{max}")
                                .replace("{current}", String.valueOf(fails))
                                .replace("{max}", String.valueOf(threshold))));
                        if (gm.isGuaranteed(targetItem)) {
                            lore.add(color(getMessage("guarantee-triggered", "&6★ 已触发保底！必定成功 ★")));
                        }
                    }
                }
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 更新信息面板
     */
    private void updateInfoPanel() {
        ItemStack item = createConfigItem("items.info-panel", Material.BOOK, "&e&l强化信息");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();

            // 每日限制
            DailyLimitManager dlm = MMOItems.plugin.getUpgrades().getDailyLimitManager();
            if (dlm != null && dlm.isEnabled()) {
                int used = dlm.getUsedAttempts(player);
                int max = dlm.getMaxAttempts(player);
                int remaining = max - used;

                lore.add(color("&7"));
                lore.add(color(getMessage("daily-limit", "&e今日强化次数:")));

                String remainColor = remaining > 10 ? "&a" : (remaining > 0 ? "&e" : "&c");
                lore.add(color(getMessage("daily-used", "  &7已用: &f{used}/{max}")
                        .replace("{used}", String.valueOf(used))
                        .replace("{max}", String.valueOf(max))));
                lore.add(color(getMessage("daily-remaining", "  &7剩余: {color}{remaining} 次")
                        .replace("{color}", remainColor)
                        .replace("{remaining}", String.valueOf(remaining))));
            }

            // 惩罚信息
            ItemStack targetItem = inventory.getItem(slotTargetItem);
            if (targetItem != null && targetItem.getType() != Material.AIR) {
                NBTItem targetNBT = NBTItem.get(targetItem);
                if (targetNBT.hasType()) {
                    VolatileMMOItem mmoItem = new VolatileMMOItem(targetNBT);
                    if (mmoItem.hasData(ItemStats.UPGRADE)) {
                        UpgradeData data = (UpgradeData) mmoItem.getData(ItemStats.UPGRADE);
                        int currentLevel = data.getLevel();

                        lore.add(color("&7"));
                        lore.add(color(getMessage("risk-title", "&c⚠ 失败风险:")));

                        boolean hasPenalty = false;
                        double protection = getAuxiliaryProtection();

                        // 碎裂风险
                        if (data.isInBreakRange(currentLevel) && data.getBreakChance() > 0) {
                            double breakChance = data.getBreakChance() * 100;
                            if (protection > 0) {
                                breakChance *= (1 - protection / 100.0);
                            }
                            lore.add(color(getMessage("risk-break", "  &c☠ 碎裂: {rate}%")
                                    .replace("{rate}", String.format("%.1f", breakChance))));
                            hasPenalty = true;
                        }

                        // 掉级风险
                        if (data.isInDowngradeRange(currentLevel) && data.getDowngradeChance() > 0) {
                            double downgradeChance = data.getDowngradeChance() * 100;
                            if (protection > 0) {
                                downgradeChance *= (1 - protection / 100.0);
                            }
                            lore.add(color(getMessage("risk-downgrade", "  &e↓ 掉级: {rate}% (-{amount}级)")
                                    .replace("{rate}", String.format("%.1f", downgradeChance))
                                    .replace("{amount}", String.valueOf(data.getDowngradeAmount()))));
                            hasPenalty = true;
                        }

                        // 销毁
                        if (data.destroysOnFail()) {
                            lore.add(color(getMessage("risk-destroy", "  &c✖ 失败销毁物品")));
                            hasPenalty = true;
                        }

                        if (!hasPenalty) {
                            lore.add(color(getMessage("risk-none", "  &a✓ 当前等级无惩罚")));
                        }

                        // 保护石效果
                        if (protection > 0) {
                            lore.add(color("&7"));
                            lore.add(color(getMessage("protection-effect", "&b✦ 保护石效果:")));
                            lore.add(color(getMessage("protection-value", "  &b惩罚概率 -{rate}%")
                                    .replace("{rate}", String.format("%.0f", protection))));
                        }
                    }
                }
            } else {
                lore.add(color("&7"));
                lore.add(color("&7放入物品查看详细信息"));
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        setSlotItem(slotInfoPanel, item);
    }

    /**
     * 更新强化按钮
     */
    private void updateUpgradeButton() {
        boolean canUpgrade = canPerformUpgrade();
        ItemStack button;

        if (processing) {
            button = createConfigItem("items.upgrade-button-cooldown", Material.GRAY_CONCRETE, "&7&l请稍候...");
        } else if (canUpgrade) {
            button = createConfigItem("items.upgrade-button-enabled", Material.LIME_CONCRETE, "&a&l✦ 点击强化 ✦");
        } else {
            button = createUpgradeButtonDisabled();
        }

        setSlotItem(slotUpgradeButton, button);
    }

    private ItemStack createUpgradeButtonDisabled() {
        ItemStack item = createConfigItem("items.upgrade-button-disabled", Material.RED_CONCRETE, "&c&l✦ 无法强化 ✦");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(color("&7"));
            lore.addAll(getUpgradeBlockReasons());
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ===== 计算方法 =====

    private double calculateActualSuccessRate() {
        ItemStack targetItem = inventory.getItem(slotTargetItem);
        if (targetItem == null || targetItem.getType() == Material.AIR) return 0;

        NBTItem targetNBT = NBTItem.get(targetItem);
        if (!targetNBT.hasType()) return 0;

        VolatileMMOItem mmoItem = new VolatileMMOItem(targetNBT);
        if (!mmoItem.hasData(ItemStats.UPGRADE)) return 0;

        UpgradeData data = (UpgradeData) mmoItem.getData(ItemStats.UPGRADE);

        // 保底检查
        GuaranteeManager gm = MMOItems.plugin.getUpgrades().getGuaranteeManager();
        if (gm != null && gm.isEnabled() && gm.isGuaranteed(targetItem)) {
            return 1.0;
        }

        double baseSuccess = getBaseSuccessFromStone();

        // 应用衰减
        double actualSuccess = baseSuccess;
        if (data.isDecayEnabled() && data.getDecayFactor() < 1.0) {
            actualSuccess *= Math.pow(data.getDecayFactor(), data.getLevel());
        }

        // 应用幸运石（累乘口径：actualSuccess = actualSuccess * (1 + bonus/100)）
        double chanceBonus = getAuxiliaryChanceBonus();
        if (chanceBonus > 0) {
            actualSuccess *= 1.0 + (chanceBonus / 100.0);
        }

        return Math.min(1.0, Math.max(0, actualSuccess));
    }

    private double getBaseSuccessFromStone() {
        ItemStack stoneItem = inventory.getItem(slotUpgradeStone);
        if (stoneItem == null || stoneItem.getType() == Material.AIR) return 1.0;

        NBTItem stoneNBT = NBTItem.get(stoneItem);
        VolatileMMOItem stoneMmo = new VolatileMMOItem(stoneNBT);
        if (stoneMmo.hasData(ItemStats.UPGRADE)) {
            UpgradeData stoneData = (UpgradeData) stoneMmo.getData(ItemStats.UPGRADE);
            return stoneData.getSuccess();
        }
        return 1.0;
    }

    private double getDecayedSuccessRate() {
        ItemStack targetItem = inventory.getItem(slotTargetItem);
        if (targetItem == null || targetItem.getType() == Material.AIR) return 0;

        NBTItem targetNBT = NBTItem.get(targetItem);
        if (!targetNBT.hasType()) return 0;

        VolatileMMOItem mmoItem = new VolatileMMOItem(targetNBT);
        if (!mmoItem.hasData(ItemStats.UPGRADE)) return 0;

        UpgradeData data = (UpgradeData) mmoItem.getData(ItemStats.UPGRADE);
        double baseSuccess = getBaseSuccessFromStone();

        if (data.isDecayEnabled() && data.getDecayFactor() < 1.0) {
            return baseSuccess * Math.pow(data.getDecayFactor(), data.getLevel());
        }
        return baseSuccess;
    }

    private String getSuccessColor(double success) {
        if (success >= 0.8) return "&a";
        if (success >= 0.5) return "&e";
        if (success >= 0.3) return "&6";
        return "&c";
    }

    private double getAuxiliaryChanceBonus() {
        ItemStack luckyStone = inventory.getItem(slotLuckyStone);
        if (luckyStone == null || luckyStone.getType() == Material.AIR) return 0;

        NBTItem nbt = NBTItem.get(luckyStone);
        if (nbt.hasTag(ItemStats.AUXILIARY_CHANCE_BONUS.getNBTPath())) {
            return nbt.getDouble(ItemStats.AUXILIARY_CHANCE_BONUS.getNBTPath());
        }
        return 0;
    }

    private double getAuxiliaryProtection() {
        ItemStack protectStone = inventory.getItem(slotProtectStone);
        if (protectStone == null || protectStone.getType() == Material.AIR) return 0;

        NBTItem nbt = NBTItem.get(protectStone);
        if (nbt.hasTag(ItemStats.AUXILIARY_PROTECTION.getNBTPath())) {
            return nbt.getDouble(ItemStats.AUXILIARY_PROTECTION.getNBTPath());
        }
        return 0;
    }

    private double getAuxiliaryDirectUpChance() {
        ItemStack directStone = inventory.getItem(slotDirectStone);
        if (directStone == null || directStone.getType() == Material.AIR) return 0;

        NBTItem nbt = NBTItem.get(directStone);
        if (nbt.hasTag(ItemStats.AUXILIARY_DIRECT_UP_CHANCE.getNBTPath())) {
            return nbt.getDouble(ItemStats.AUXILIARY_DIRECT_UP_CHANCE.getNBTPath());
        }
        return 0;
    }

    private int getAuxiliaryDirectUpLevels() {
        ItemStack directStone = inventory.getItem(slotDirectStone);
        if (directStone == null || directStone.getType() == Material.AIR) return 0;

        NBTItem nbt = NBTItem.get(directStone);
        if (nbt.hasTag(ItemStats.AUXILIARY_DIRECT_UP_LEVELS.getNBTPath())) {
            return (int) nbt.getDouble(ItemStats.AUXILIARY_DIRECT_UP_LEVELS.getNBTPath());
        }
        return 0;
    }

    private boolean canPerformUpgrade() {
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

    private boolean isValidUpgradeStone(@Nullable ItemStack stone, @Nullable ItemStack target) {
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

    private List<String> getUpgradeBlockReasons() {
        List<String> reasons = new ArrayList<>();
        ItemStack targetItem = inventory.getItem(slotTargetItem);
        ItemStack stoneItem = inventory.getItem(slotUpgradeStone);

        if (targetItem == null || targetItem.getType() == Material.AIR) {
            reasons.add(color(getMessage("block-no-item", "&c• 请放入待强化物品")));
        } else {
            NBTItem nbt = NBTItem.get(targetItem);
            if (!nbt.hasType()) {
                reasons.add(color(getMessage("block-not-mmoitem", "&c• 物品不是 MMOItems 物品")));
            } else {
                VolatileMMOItem mmo = new VolatileMMOItem(nbt);
                if (!mmo.hasData(ItemStats.UPGRADE)) {
                    reasons.add(color(getMessage("block-not-upgradable", "&c• 物品不可强化")));
                } else {
                    UpgradeData data = (UpgradeData) mmo.getData(ItemStats.UPGRADE);
                    if (!data.canLevelUp()) {
                        reasons.add(color(getMessage("block-max-level", "&c• 已达最大等级")));
                    }
                }
            }
        }

        if (stoneItem == null || stoneItem.getType() == Material.AIR) {
            reasons.add(color(getMessage("block-no-stone", "&c• 请放入强化石")));
        } else if (targetItem != null && !isValidUpgradeStone(stoneItem, targetItem)) {
            reasons.add(color(getMessage("block-stone-mismatch", "&c• 强化石不匹配")));
        }

        return reasons;
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
        updateUpgradeButton();

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
            updateUpgradeButton();
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
            updateUpgradeButton();
            playSound("sounds.deny");
            player.sendMessage(color(getMessage("daily-limit-reached", "&c今日强化次数已用尽 ({used}/{max})")
                    .replace("{used}", String.valueOf(used))
                    .replace("{max}", String.valueOf(max))));
            return;
        }

        UpgradeTemplate template = targetData.getTemplate();
        if (template == null) {
            processing = false;
            updateUpgradeButton();
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
            updateUpgradeButton();
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
            updateAllDisplays();
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

    private String color(String text) {
        return MythicLib.plugin.parseColors(text);
    }

    private String getMessage(String key, String defaultValue) {
        return getConfig().getString("messages." + key, defaultValue);
    }

    private Material getMaterial(String path, Material defaultMaterial) {
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
            Bukkit.getScheduler().runTaskLater(MMOItems.plugin, this::updateAllDisplays, 1L);
            return;
        }

        // 功能槽位：允许放置/取出
        if (isFunctionalSlot(slot)) {
            // 播放放入音效
            Bukkit.getScheduler().runTaskLater(MMOItems.plugin, () -> {
                playSound("sounds.item-place");
                updateAllDisplays();
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

        Bukkit.getScheduler().runTaskLater(MMOItems.plugin, this::updateAllDisplays, 1L);
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
