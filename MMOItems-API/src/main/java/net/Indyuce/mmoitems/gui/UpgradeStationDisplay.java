package net.Indyuce.mmoitems.gui;

import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.UpgradeTemplate;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.VolatileMMOItem;
import net.Indyuce.mmoitems.api.upgrade.bonus.UpgradeChanceBonusCalculator;
import net.Indyuce.mmoitems.api.upgrade.economy.UpgradeEconomyHandler;
import net.Indyuce.mmoitems.api.upgrade.guarantee.GuaranteeManager;
import net.Indyuce.mmoitems.api.upgrade.limit.DailyLimitManager;
import net.Indyuce.mmoitems.stat.data.UpgradeData;
import net.Indyuce.mmoitems.util.MMOUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 负责升级工作台 GUI 的展示与进度计算，保持纯渲染职责。
 */
class UpgradeStationDisplay {

    /**
     * 用于匹配 Lore 行中数值的正则表达式
     * 匹配格式如：? 7.8、: 100、+5%、-10% 等
     */
    private static final Pattern NUMBER_PATTERN = Pattern.compile("([?:：]\\s*[+-]?)([\\d.]+)(%?)");

    private final UpgradeStationGUI gui;

    UpgradeStationDisplay(UpgradeStationGUI gui) {
        this.gui = gui;
    }

    void updateAllDisplays() {
        updatePreview();
        updateProgressBar();
        updateInfoPanel();
        updateUpgradeButton();
    }

    private void updatePreview() {
        Inventory inventory = gui.getInventory();
        ItemStack targetItem = inventory.getItem(gui.getSlotTargetItem());

        ItemStack previewItem;

        if (targetItem == null || targetItem.getType() == Material.AIR) {
            previewItem = gui.createConfigItem("items.preview-waiting", Material.ARROW, "&f&l→ 强化 →");
        } else {
            VolatileMMOItem mmoItem = new VolatileMMOItem(NBTItem.get(targetItem));
            if (!mmoItem.getNBT().hasType()) {
                previewItem = createPreviewItem(Material.BARRIER, "&c非MMOItems物品", Arrays.asList("&7该物品不能强化"));
            } else if (!mmoItem.hasData(ItemStats.UPGRADE)) {
                previewItem = createPreviewItem(Material.BARRIER, "&c物品不可强化", Arrays.asList("&7该物品没有配置强化属性"));
            } else {
                UpgradeData data = (UpgradeData) mmoItem.getData(ItemStats.UPGRADE);
                previewItem = createDetailedPreview(targetItem, data);
            }
        }

        gui.setSlotItem(gui.getSlotPreview(), previewItem);
    }

    private ItemStack createPreviewItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(gui.color(name));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(gui.color(line));
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

        if (atMax) {
            List<String> lore = new ArrayList<>();
            lore.add("&7");
            lore.add(gui.getMessage("item-name", "&f物品: {name}").replace("{name}", MMOUtils.getDisplayName(targetItem)));
            lore.add("&7");
            lore.add(gui.getMessage("current-level", "&e当前等级: &f+{level}").replace("{level}", String.valueOf(currentLevel)));
            lore.add("&7");
            lore.add("&c已达到强化上限，无法继续强化");
            return createPreviewItem(Material.STRUCTURE_VOID, gui.getMessage("preview-max-level", "&6&l已达最大等级"), lore);
        }

        try {
            return buildUpgradedPreviewItem(targetItem, data, currentLevel, maxLevel);
        } catch (Exception e) {
            return createSimplePreview(targetItem, data, currentLevel, maxLevel);
        }
    }

    private ItemStack buildUpgradedPreviewItem(ItemStack targetItem, UpgradeData data, int currentLevel, int maxLevel) {
        VolatileMMOItem volatileItem = new VolatileMMOItem(NBTItem.get(targetItem));
        ItemMeta originalMeta = targetItem.getItemMeta();
        List<String> originalLore = (originalMeta != null && originalMeta.hasLore()) ? new ArrayList<>(originalMeta.getLore()) : new ArrayList<>();

        LiveMMOItem originalItem = new LiveMMOItem(volatileItem.getNBT());
        MMOItem previewMMO = originalItem.clone();

        UpgradeTemplate template = data.getTemplate();
        if (template == null) {
            throw new RuntimeException("强化模板不存在: " + data.getTemplateName());
        }

        int targetLevel = currentLevel + 1;
        template.upgradeTo(previewMMO, targetLevel);

        ItemStack preview = previewMMO.newBuilder().buildNBT().toItem();
        ItemMeta previewMeta = preview.getItemMeta();
        if (previewMeta == null) {
            return preview;
        }

        List<String> upgradedLore = previewMeta.hasLore() ? new ArrayList<>(previewMeta.getLore()) : new ArrayList<>();
        List<String> markedLore = injectChangeMarkers(originalLore, upgradedLore);

        String originalName = previewMeta.hasDisplayName() ? previewMeta.getDisplayName() : MMOUtils.getDisplayName(targetItem);
        previewMeta.setDisplayName(gui.color(gui.getMessage("preview-title", "&e? 强化预览 ?")));

        List<String> finalLore = new ArrayList<>();
        finalLore.add(gui.color("&7"));
        finalLore.add(gui.color(gui.getMessage("preview-original-item", "&f原物品: {name}").replace("{name}", originalName)));
        finalLore.add(gui.color("&7"));

        finalLore.add(gui.color(gui.getMessage("current-level", "&e当前等级: &f+{level}").replace("{level}", String.valueOf(currentLevel))));
        finalLore.add(gui.color(gui.getMessage("after-level", "&a强化后: &f+{level}").replace("{level}", String.valueOf(targetLevel))));
        if (maxLevel > 0) {
            finalLore.add(gui.color(gui.getMessage("max-level", "&7最大等级: &f+{level}").replace("{level}", String.valueOf(maxLevel))));
        }

        double directChance = gui.getAuxiliaryDirectUpChance();
        int directLevels = gui.getAuxiliaryDirectUpLevels();
        if (directChance > 0 && directLevels > 0) {
            finalLore.add(gui.color("&7"));
            finalLore.add(gui.color(gui.getMessage("direct-effect", "&d? 直达石效果:")));
            finalLore.add(gui.color(gui.getMessage("direct-chance", "&d  {chance}% 概率直接到 +{level}")
                    .replace("{chance}", String.format("%.0f", directChance))
                    .replace("{level}", String.valueOf(targetLevel + directLevels))));
        }

        finalLore.add(gui.color("&7"));
        finalLore.add(gui.color(gui.getMessage("preview-separator", "&8─────────────────")));
        finalLore.add(gui.color(gui.getMessage("preview-full-stats", "&7&o强化后完整属性:")));
        finalLore.addAll(markedLore);

        previewMeta.setLore(finalLore);
        preview.setItemMeta(previewMeta);

        return preview;
    }

    private List<String> injectChangeMarkers(List<String> originalLore, List<String> upgradedLore) {
        FileConfiguration config = gui.getConfig();
        List<String> result = new ArrayList<>();

        boolean enabled = config.getBoolean("change-marker.enabled", true);
        if (!enabled) {
            return new ArrayList<>(upgradedLore);
        }

        double minChange = config.getDouble("change-marker.min-change", 0.01);

        Map<String, Double> originalValues = new LinkedHashMap<>();
        for (String line : originalLore) {
            String key = extractLineKey(line);
            Double value = extractFirstNumber(line);
            if (key != null && value != null) {
                originalValues.put(key, value);
            }
        }

        for (String line : upgradedLore) {
            String key = extractLineKey(line);
            Double newValue = extractFirstNumber(line);

            if (key != null && newValue != null && originalValues.containsKey(key)) {
                Double oldValue = originalValues.get(key);
                double diff = newValue - oldValue;

                if (Math.abs(diff) >= minChange) {
                    String changeMarker = formatChangeMarker(diff);
                    result.add(line + changeMarker);
                    continue;
                }
            }

            result.add(line);
        }

        return result;
    }

    private String extractLineKey(String line) {
        if (line == null || line.isEmpty()) return null;

        String stripped = ChatColor.stripColor(line);
        if (stripped == null || stripped.isEmpty()) return null;

        Matcher matcher = NUMBER_PATTERN.matcher(stripped);
        if (matcher.find()) {
            String key = stripped.substring(0, matcher.start()).trim();
            key = key.replaceAll("[?:：\\s]+$", "").trim();
            return key.isEmpty() ? null : key;
        }

        return null;
    }

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

    private String formatChangeMarker(double diff) {
        FileConfiguration config = gui.getConfig();
        String format = config.getString("change-marker.format", " &8({color}{sign}{value}&8)");
        String positiveColor = config.getString("change-marker.positive-color", "&a");
        String negativeColor = config.getString("change-marker.negative-color", "&c");
        int decimalPlaces = config.getInt("change-marker.decimal-places", 1);

        String colorCode = diff > 0 ? positiveColor : negativeColor;
        String sign = diff > 0 ? "+" : "";

        String valueFormat = "%." + decimalPlaces + "f";
        String value = String.format(valueFormat, diff);

        String result = format
                .replace("{color}", colorCode)
                .replace("{sign}", sign)
                .replace("{value}", value);

        return gui.color(result);
    }

    private ItemStack createSimplePreview(ItemStack targetItem, UpgradeData data, int currentLevel, int maxLevel) {
        List<String> lore = new ArrayList<>();
        lore.add("&7");

        String itemName = MMOUtils.getDisplayName(targetItem);
        lore.add(gui.getMessage("item-name", "&f物品: {name}").replace("{name}", itemName));
        lore.add("&7");

        lore.add(gui.getMessage("current-level", "&e当前等级: &f+{level}").replace("{level}", String.valueOf(currentLevel)));
        lore.add(gui.getMessage("after-level", "&a强化后: &f+{level}").replace("{level}", String.valueOf(currentLevel + 1)));
        if (maxLevel > 0) {
            lore.add(gui.getMessage("max-level", "&7最大等级: &f+{level}").replace("{level}", String.valueOf(maxLevel)));
        }

        double directChance = gui.getAuxiliaryDirectUpChance();
        int directLevels = gui.getAuxiliaryDirectUpLevels();
        if (directChance > 0 && directLevels > 0) {
            lore.add("&7");
            lore.add(gui.getMessage("direct-effect", "&d? 直达石效果:"));
            lore.add(gui.getMessage("direct-chance", "&d  {chance}% 概率直接到 +{level}")
                    .replace("{chance}", String.format("%.0f", directChance))
                    .replace("{level}", String.valueOf(currentLevel + 1 + directLevels)));
        }

        lore.add("&7");
        lore.add("&8(无法生成完整预览)");

        return createPreviewItem(Material.NETHER_STAR, "&e&l强化预览", lore);
    }

    private void updateProgressBar() {
        Inventory inventory = gui.getInventory();
        double successRate = calculateActualSuccessRate();
        int filledSlots = (int) Math.round(successRate * gui.getProgressLength());

        gui.setSlotItem(gui.getSlotProgressStart() - 1, createProgressLabel(successRate));

        for (int i = 0; i < gui.getProgressLength(); i++) {
            int slot = gui.getSlotProgressStart() + i;
            if (slot >= gui.getInventorySize()) break;

            Material material;
            String colorCode;

            if (i < filledSlots) {
                if (successRate >= 0.8) {
                    material = gui.getMaterial("progress-bar.high.material", Material.LIME_STAINED_GLASS_PANE);
                    colorCode = gui.getConfig().getString("progress-bar.high.color", "&a");
                } else if (successRate >= 0.5) {
                    material = gui.getMaterial("progress-bar.medium.material", Material.YELLOW_STAINED_GLASS_PANE);
                    colorCode = gui.getConfig().getString("progress-bar.medium.color", "&e");
                } else if (successRate >= 0.3) {
                    material = gui.getMaterial("progress-bar.low.material", Material.ORANGE_STAINED_GLASS_PANE);
                    colorCode = gui.getConfig().getString("progress-bar.low.color", "&6");
                } else {
                    material = gui.getMaterial("progress-bar.danger.material", Material.RED_STAINED_GLASS_PANE);
                    colorCode = gui.getConfig().getString("progress-bar.danger.color", "&c");
                }
            } else {
                material = gui.getMaterial("progress-bar.empty.material", Material.WHITE_STAINED_GLASS_PANE);
                colorCode = gui.getConfig().getString("progress-bar.empty.color", "&8");
            }

            ItemStack pane = new ItemStack(material);
            ItemMeta meta = pane.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(gui.color(colorCode + "█"));
                List<String> lore = new ArrayList<>();
                String rateMsg = gui.getMessage("success-rate", "&7成功率: {color}{rate}%")
                        .replace("{color}", getSuccessColor(successRate))
                        .replace("{rate}", String.format("%.1f", successRate * 100));
                lore.add(gui.color(rateMsg));
                meta.setLore(lore);
                pane.setItemMeta(meta);
            }
            inventory.setItem(slot, pane);
        }
    }

    private ItemStack createProgressLabel(double successRate) {
        ItemStack item = gui.createConfigItem("items.progress-label", Material.EXPERIENCE_BOTTLE, "&f成功率");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String colorCode = getSuccessColor(successRate);
            meta.setDisplayName(gui.color("&f成功率: " + colorCode + String.format("%.1f%%", successRate * 100)));

            List<String> lore = new ArrayList<>();
            lore.add(gui.color("&7"));

            double baseSuccess = getBaseSuccessFromStone();
            lore.add(gui.color(gui.getMessage("base-rate", "&7基础成功率: &f{rate}%")
                    .replace("{rate}", String.format("%.1f", baseSuccess * 100))));

            double decayedSuccess = getDecayedSuccessRate();
            if (decayedSuccess < baseSuccess - 0.001) {
                lore.add(gui.color(gui.getMessage("decayed-rate", "&7等级衰减后: &f{rate}%")
                        .replace("{rate}", String.format("%.1f", decayedSuccess * 100))));
            }

            double chanceBonus = gui.getAuxiliaryChanceBonus();
            if (chanceBonus > 0) {
                lore.add(gui.color(gui.getMessage("lucky-bonus", "&a× 幸运石: &f×(1+{rate}%)")
                        .replace("{rate}", String.format("%.1f", chanceBonus))));
            }

            ItemStack targetItem = gui.getInventory().getItem(gui.getSlotTargetItem());
            if (targetItem != null && targetItem.getType() != Material.AIR) {
                GuaranteeManager gm = MMOItems.plugin.getUpgrades().getGuaranteeManager();
                if (gm != null && gm.isEnabled()) {
                    int fails = gm.getConsecutiveFails(targetItem);
                    int threshold = gm.getThreshold();
                    if (fails > 0) {
                        lore.add(gui.color("&7"));
                        lore.add(gui.color(gui.getMessage("guarantee-progress", "&6保底进度: &f{current}/{max}")
                                .replace("{current}", String.valueOf(fails))
                                .replace("{max}", String.valueOf(threshold))));
                        if (gm.isGuaranteed(targetItem)) {
                            lore.add(gui.color(gui.getMessage("guarantee-triggered", "&6★ 已触发保底！必定成功 ★")));
                        }
                    }
                }
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void updateInfoPanel() {
        ItemStack item = gui.createConfigItem("items.info-panel", Material.BOOK, "&e&l强化信息");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();

            DailyLimitManager dlm = MMOItems.plugin.getUpgrades().getDailyLimitManager();
            if (dlm != null && dlm.isEnabled()) {
                int used = dlm.getUsedAttempts(gui.getPlayer());
                int max = dlm.getMaxAttempts(gui.getPlayer());
                int remaining = max - used;

                lore.add(gui.color("&7"));
                lore.add(gui.color(gui.getMessage("daily-limit", "&e今日强化次数:")));

                String remainColor = remaining > 10 ? "&a" : (remaining > 0 ? "&e" : "&c");
                lore.add(gui.color(gui.getMessage("daily-used", "  &7已用: &f{used}/{max}")
                        .replace("{used}", String.valueOf(used))
                        .replace("{max}", String.valueOf(max))));
                lore.add(gui.color(gui.getMessage("daily-remaining", "  &7剩余: {color}{remaining} 次")
                        .replace("{color}", remainColor)
                        .replace("{remaining}", String.valueOf(remaining))));
            }

            ItemStack targetItem = gui.getInventory().getItem(gui.getSlotTargetItem());
            if (targetItem != null && targetItem.getType() != Material.AIR) {
                VolatileMMOItem volatileTarget = new VolatileMMOItem(NBTItem.get(targetItem));
                if (volatileTarget.getNBT().hasType() && volatileTarget.hasData(ItemStats.UPGRADE)) {
                    UpgradeData data = (UpgradeData) volatileTarget.getData(ItemStats.UPGRADE);
                    int currentLevel = data.getLevel();

                    lore.add(gui.color("&7"));
                    lore.add(gui.color(gui.getMessage("risk-title", "&c? 失败风险:")));

                    boolean hasPenalty = false;
                    double protection = gui.getAuxiliaryProtection();

                    if (data.isInBreakRange(currentLevel) && data.getBreakChance() > 0) {
                        double breakChance = data.getBreakChance() * 100;
                        if (protection > 0) {
                            breakChance *= (1 - protection / 100.0);
                        }
                        lore.add(gui.color(gui.getMessage("risk-break", "  &c? 碎裂: {rate}%")
                                .replace("{rate}", String.format("%.1f", breakChance))));
                        hasPenalty = true;
                    }

                    if (data.isInDowngradeRange(currentLevel) && data.getDowngradeChance() > 0) {
                        double downgradeChance = data.getDowngradeChance() * 100;
                        if (protection > 0) {
                            downgradeChance *= (1 - protection / 100.0);
                        }
                        lore.add(gui.color(gui.getMessage("risk-downgrade", "  &e↓ 掉级: {rate}% (-{amount}级)")
                                .replace("{rate}", String.format("%.1f", downgradeChance))
                                .replace("{amount}", String.valueOf(data.getDowngradeAmount()))));
                        hasPenalty = true;
                    }

                    if (data.destroysOnFail()) {
                        lore.add(gui.color(gui.getMessage("risk-destroy", "  &c? 失败销毁物品")));
                        hasPenalty = true;
                    }

                    if (!hasPenalty) {
                        lore.add(gui.color(gui.getMessage("risk-none", "  &a? 当前等级无惩罚")));
                    }

                    if (protection > 0) {
                        lore.add(gui.color("&7"));
                        lore.add(gui.color(gui.getMessage("protection-effect", "&b? 保护石效果:")));
                        lore.add(gui.color(gui.getMessage("protection-value", "  &b惩罚概率 -{rate}%")
                                .replace("{rate}", String.format("%.0f", protection))));
                    }

                    // ========== 经济消耗显示（新增） ==========
                    UpgradeEconomyHandler economyHandler = MMOItems.plugin.getUpgrades().getEconomyHandler();
                    if (economyHandler != null && economyHandler.isEnabled()) {
                        double cost = economyHandler.getCost(currentLevel);
                        if (cost > 0) {
                            lore.add(gui.color("&7"));
                            lore.add(gui.color(gui.getMessage("economy-title", "&6? 强化费用:")));
                            lore.add(gui.color(gui.getMessage("economy-cost", "  &6消耗: &f{cost}")
                                    .replace("{cost}", economyHandler.format(cost))));

                            // 显示玩家余额
                            double balance = economyHandler.getBalance(gui.getPlayer());
                            String balanceColor = balance >= cost ? "&a" : "&c";
                            lore.add(gui.color(gui.getMessage("economy-balance", "  &7余额: {color}{balance}")
                                    .replace("{color}", balanceColor)
                                    .replace("{balance}", economyHandler.format(balance))));
                        }
                    }

                    // ========== 全局概率加成显示（新增） ==========
                    UpgradeChanceBonusCalculator bonusCalculator = MMOItems.plugin.getUpgrades().getChanceBonusCalculator();
                    if (bonusCalculator != null && bonusCalculator.isEnabled()) {
                        double bonus = bonusCalculator.calculateBonus(gui.getPlayer(), currentLevel);
                        if (bonus > 0) {
                            lore.add(gui.color("&7"));
                            lore.add(gui.color(gui.getMessage("global-bonus-title", "&d? 全局加成:")));
                            lore.add(gui.color(gui.getMessage("global-bonus-value", "  &d成功率 +{rate}%")
                                    .replace("{rate}", String.format("%.1f", bonus))));
                        }
                    }
                }
            } else {
                lore.add(gui.color("&7"));
                lore.add(gui.color("&7放入物品查看详细信息"));
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        gui.setSlotItem(gui.getSlotInfoPanel(), item);
    }

    void updateUpgradeButton() {
        boolean canUpgrade = gui.canPerformUpgrade();
        ItemStack button;

        if (gui.isProcessing()) {
            button = gui.createConfigItem("items.upgrade-button-cooldown", Material.GRAY_CONCRETE, "&7&l请稍候...");
        } else if (canUpgrade) {
            button = gui.createConfigItem("items.upgrade-button-enabled", Material.LIME_CONCRETE, "&a&l? 点击强化 ?");
        } else {
            button = createUpgradeButtonDisabled();
        }

        gui.setSlotItem(gui.getSlotUpgradeButton(), button);
    }

    private ItemStack createUpgradeButtonDisabled() {
        ItemStack item = gui.createConfigItem("items.upgrade-button-disabled", Material.RED_CONCRETE, "&c&l? 无法强化 ?");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(gui.color("&7"));
            lore.addAll(getUpgradeBlockReasons());
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private double calculateActualSuccessRate() {
        Inventory inventory = gui.getInventory();
        ItemStack targetItem = inventory.getItem(gui.getSlotTargetItem());
        if (targetItem == null || targetItem.getType() == Material.AIR) return 0;

        VolatileMMOItem mmoItem = new VolatileMMOItem(NBTItem.get(targetItem));
        if (!mmoItem.getNBT().hasType() || !mmoItem.hasData(ItemStats.UPGRADE)) {
            return 0;
        }

        UpgradeData data = (UpgradeData) mmoItem.getData(ItemStats.UPGRADE);

        GuaranteeManager gm = MMOItems.plugin.getUpgrades().getGuaranteeManager();
        if (gm != null && gm.isEnabled() && gm.isGuaranteed(targetItem)) {
            return 1.0;
        }

        double baseSuccess = getBaseSuccessFromStone();

        double actualSuccess = baseSuccess;
        if (data.isDecayEnabled() && data.getDecayFactor() < 1.0) {
            actualSuccess *= Math.pow(data.getDecayFactor(), data.getLevel());
        }

        double chanceBonus = gui.getAuxiliaryChanceBonus();
        if (chanceBonus > 0) {
            actualSuccess *= 1.0 + (chanceBonus / 100.0);
        }

        return Math.min(1.0, Math.max(0, actualSuccess));
    }

    private double getBaseSuccessFromStone() {
        Inventory inventory = gui.getInventory();
        ItemStack stoneItem = inventory.getItem(gui.getSlotUpgradeStone());
        if (stoneItem == null || stoneItem.getType() == Material.AIR) return 1.0;

        VolatileMMOItem stoneMmo = new VolatileMMOItem(NBTItem.get(stoneItem));
        if (stoneMmo.hasData(ItemStats.UPGRADE)) {
            UpgradeData stoneData = (UpgradeData) stoneMmo.getData(ItemStats.UPGRADE);
            return stoneData.getSuccess();
        }
        return 1.0;
    }

    private double getDecayedSuccessRate() {
        Inventory inventory = gui.getInventory();
        ItemStack targetItem = inventory.getItem(gui.getSlotTargetItem());
        if (targetItem == null || targetItem.getType() == Material.AIR) return 0;

        VolatileMMOItem mmoItem = new VolatileMMOItem(NBTItem.get(targetItem));
        if (!mmoItem.getNBT().hasType() || !mmoItem.hasData(ItemStats.UPGRADE)) {
            return 0;
        }

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

    private List<String> getUpgradeBlockReasons() {
        List<String> reasons = new ArrayList<>();
        Inventory inventory = gui.getInventory();
        ItemStack targetItem = inventory.getItem(gui.getSlotTargetItem());
        ItemStack stoneItem = inventory.getItem(gui.getSlotUpgradeStone());

        if (targetItem == null || targetItem.getType() == Material.AIR) {
            reasons.add(gui.color(gui.getMessage("block-no-item", "&c? 请放入待强化物品")));
        } else {
            VolatileMMOItem mmo = new VolatileMMOItem(NBTItem.get(targetItem));
            if (!mmo.getNBT().hasType()) {
                reasons.add(gui.color(gui.getMessage("block-not-mmoitem", "&c? 物品不是 MMOItems 物品")));
            } else if (!mmo.hasData(ItemStats.UPGRADE)) {
                reasons.add(gui.color(gui.getMessage("block-not-upgradable", "&c? 物品不可强化")));
            } else {
                UpgradeData data = (UpgradeData) mmo.getData(ItemStats.UPGRADE);
                if (!data.canLevelUp()) {
                    reasons.add(gui.color(gui.getMessage("block-max-level", "&c? 已达最大等级")));
                }
            }
        }

        if (stoneItem == null || stoneItem.getType() == Material.AIR) {
            reasons.add(gui.color(gui.getMessage("block-no-stone", "&c? 请放入强化石")));
        } else if (targetItem != null && !gui.isValidUpgradeStone(stoneItem, targetItem)) {
            reasons.add(gui.color(gui.getMessage("block-stone-mismatch", "&c? 强化石不匹配")));
        }

        return reasons;
    }
}
