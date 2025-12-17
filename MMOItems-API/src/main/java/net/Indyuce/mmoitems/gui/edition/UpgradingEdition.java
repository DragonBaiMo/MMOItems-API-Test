package net.Indyuce.mmoitems.gui.edition;

import io.lumine.mythic.lib.api.util.AltChar;
import io.lumine.mythic.lib.api.util.ItemFactory;
import io.lumine.mythic.lib.gui.Navigator;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.edition.StatEdition;
import net.Indyuce.mmoitems.api.item.template.MMOItemTemplate;
import net.Indyuce.mmoitems.util.MMOUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class UpgradingEdition extends EditionInventory {
	private static final ItemStack notAvailable = ItemFactory.of(Material.RED_STAINED_GLASS_PANE).name("&cNot Available").build();

	public UpgradingEdition(Navigator navigator, MMOItemTemplate template) {
		super(navigator, template);
	}

	@Override
	public String getName() {
		return "强化设置: " + template.getId();
	}

	@Override
	public void arrangeInventory() {
		boolean workbench = getEditedSection().getBoolean("upgrade.workbench");
		if (!template.getType().corresponds(Type.CONSUMABLE)) {

			ItemStack workbenchItem = new ItemStack(Material.CRAFTING_TABLE);
			ItemMeta workbenchItemMeta = workbenchItem.getItemMeta();
			workbenchItemMeta.setDisplayName(ChatColor.GREEN + "仅工作台强化");
			List<String> workbenchItemLore = new ArrayList<>();
			workbenchItemLore.add(ChatColor.GRAY + "启用后，玩家必须使用工作台");
			workbenchItemLore.add(ChatColor.GRAY + "配方才能强化武器。");
			workbenchItemLore.add("");
			workbenchItemLore.add(ChatColor.GRAY + "当前值: " + ChatColor.GOLD + workbench);
			workbenchItemLore.add("");
			workbenchItemLore.add(ChatColor.YELLOW + AltChar.listDash + " 点击切换此值");
			workbenchItemMeta.setLore(workbenchItemLore);
			workbenchItem.setItemMeta(workbenchItemMeta);
			inventory.setItem(20, workbenchItem);

			String upgradeTemplate = getEditedSection().getString("upgrade.template");
			ItemStack templateItem = new ItemStack(Material.OAK_SIGN);
			ItemMeta templateItemMeta = templateItem.getItemMeta();
			templateItemMeta.setDisplayName(ChatColor.GREEN + "强化模板");
			List<String> templateItemLore = new ArrayList<>();
			templateItemLore.add(ChatColor.GRAY + "此选项决定强化时哪些属性会被提升。");
			templateItemLore.add(ChatColor.GRAY + "更多信息请查看 Wiki。");
			templateItemLore.add("");
			templateItemLore.add(ChatColor.GRAY + "当前值: "
					+ (upgradeTemplate == null ? ChatColor.RED + "无模板" : ChatColor.GOLD + upgradeTemplate));
			templateItemLore.add("");
			templateItemLore.add(ChatColor.YELLOW + AltChar.listDash + " 点击输入模板。");
			templateItemLore.add(ChatColor.YELLOW + AltChar.listDash + " 右键重置。");
			templateItemMeta.setLore(templateItemLore);
			templateItem.setItemMeta(templateItemMeta);
			inventory.setItem(22, templateItem);

			int max = getEditedSection().getInt("upgrade.max");
			ItemStack maxItem = new ItemStack(Material.BARRIER);
			ItemMeta maxItemMeta = maxItem.getItemMeta();
			maxItemMeta.setDisplayName(ChatColor.GREEN + "最大强化等级");
			List<String> maxItemLore = new ArrayList<>();
			maxItemLore.add(ChatColor.GRAY + "物品可获得的最大强化等级");
			maxItemLore.add(ChatColor.GRAY + "（配方或消耗品）。");
			maxItemLore.add("");
			maxItemLore.add(ChatColor.GRAY + "当前值: " + (max == 0 ? ChatColor.RED + "无限制" : ChatColor.GOLD + "" + max));
			maxItemLore.add("");
			maxItemLore.add(ChatColor.YELLOW + AltChar.listDash + " 点击修改此值。");
			maxItemLore.add(ChatColor.YELLOW + AltChar.listDash + " 右键重置。");
			maxItemMeta.setLore(maxItemLore);
			maxItem.setItemMeta(maxItemMeta);
			inventory.setItem(40, maxItem);

			int min = getEditedSection().getInt("upgrade.min", 0);
			ItemStack minItem = new ItemStack(Material.BARRIER);
			ItemMeta minItemMeta = minItem.getItemMeta();
			minItemMeta.setDisplayName(ChatColor.GREEN + "最小强化等级");
			List<String> minItemLore = new ArrayList<>();
			minItemLore.add(ChatColor.GRAY + "物品最低可降至的等级");
			minItemLore.add(ChatColor.GRAY + "（掉级或碎裂时）。");
			minItemLore.add("");
			minItemLore.add(ChatColor.GRAY + "当前值: " + (min == 0 ? ChatColor.RED + "0" : ChatColor.GOLD + String.valueOf(min)));
			minItemLore.add("");
			minItemLore.add(ChatColor.YELLOW + AltChar.listDash + " 点击修改此值。");
			minItemLore.add(ChatColor.YELLOW + AltChar.listDash + " 右键重置。");
			minItemMeta.setLore(minItemLore);
			minItem.setItemMeta(minItemMeta);
			inventory.setItem(41, minItem);
		} else {
			inventory.setItem(20, notAvailable);
			inventory.setItem(22, notAvailable);
		}

		if (!workbench || template.getType().corresponds(Type.CONSUMABLE)) {

			String reference = getEditedSection().getString("upgrade.reference");
			ItemStack referenceItem = new ItemStack(Material.PAPER);
			ItemMeta referenceItemMeta = referenceItem.getItemMeta();
			referenceItemMeta.setDisplayName(ChatColor.GREEN + "强化参考标识");
            List<String> referenceItemLore = new ArrayList<>();
            referenceItemLore.add(ChatColor.GRAY + "此选项决定哪些消耗品可以强化此物品");
            referenceItemLore.add(ChatColor.AQUA + "消耗品的强化参考标识" + ChatColor.GRAY + "必须与目标");
            referenceItemLore.add(ChatColor.GRAY + "物品的强化参考标识匹配" + ChatColor.GRAY + "，否则无法强化");
            referenceItemLore.add("");
			referenceItemLore
					.add(ChatColor.GRAY + "当前值: " + (reference == null ? ChatColor.RED + "无参考标识" : ChatColor.GOLD + reference));
			referenceItemLore.add("");
			referenceItemLore.add(ChatColor.YELLOW + AltChar.listDash + " 点击输入参考标识");
			referenceItemLore.add(ChatColor.YELLOW + AltChar.listDash + " 右键重置");
			referenceItemMeta.setLore(referenceItemLore);
			referenceItem.setItemMeta(referenceItemMeta);
			inventory.setItem(38, referenceItem);
		} else
			inventory.setItem(38, notAvailable);

		double success = getEditedSection().getDouble("upgrade.success");
		ItemStack successItem = new ItemStack(Material.EXPERIENCE_BOTTLE);
		ItemMeta successItemMeta = successItem.getItemMeta();
		successItemMeta.setDisplayName(ChatColor.GREEN + "强化成功几率");
		List<String> successItemLore = new ArrayList<>();
		successItemLore.add(ChatColor.GRAY + "使用消耗品或配方强化时的成功几率");
		successItemLore.add("");
		successItemLore.add(ChatColor.GRAY + "当前值: " + ChatColor.GOLD + (success == 0 ? "100" : "" + success) + "%");
		successItemLore.add("");
		successItemLore.add(ChatColor.YELLOW + AltChar.listDash + " 左键点击修改此值");
		successItemLore.add(ChatColor.YELLOW + AltChar.listDash + " 右键重置");
		successItemMeta.setLore(successItemLore);
		successItem.setItemMeta(successItemMeta);
		inventory.setItem(24, successItem);

		// 惩罚和衰减配置只对非消耗品物品类型显示（不再依赖 success > 0 条件）
		if (!template.getType().corresponds(Type.CONSUMABLE)) {
			ItemStack destroyOnFail = new ItemStack(Material.FISHING_ROD);
			ItemMeta destroyOnFailMeta = destroyOnFail.getItemMeta();
			((Damageable) destroyOnFailMeta).setDamage(30);
			destroyOnFailMeta.setDisplayName(ChatColor.GREEN + "失败时销毁?");
			List<String> destroyOnFailLore = new ArrayList<>();
			destroyOnFailLore.add(ChatColor.GRAY + "启用后，强化失败时物品将被");
			destroyOnFailLore.add(ChatColor.GRAY + "销毁");
			destroyOnFailLore.add("");
			destroyOnFailLore.add(ChatColor.GRAY + "当前值: " + ChatColor.GOLD + getEditedSection().getBoolean("upgrade.destroy"));
			String destroyProtectKey = getEditedSection().getString("upgrade.destroy-protect-key", "");
			destroyOnFailLore.add(ChatColor.GRAY + "销毁保护标签: " + (destroyProtectKey.isEmpty() ? ChatColor.RED + "无" : ChatColor.GOLD + destroyProtectKey));
			destroyOnFailLore.add("");
			destroyOnFailLore.add(ChatColor.YELLOW + AltChar.listDash + " 左键切换此值");
			destroyOnFailLore.add(ChatColor.YELLOW + AltChar.listDash + " 右键设置销毁保护标签");
			destroyOnFailMeta.setLore(destroyOnFailLore);
			destroyOnFail.setItemMeta(destroyOnFailMeta);
			inventory.setItem(42, destroyOnFail);

			// ========== 成功率衰减配置 ==========
			boolean decayEnabled = getEditedSection().getBoolean("upgrade.decay-enabled", false);
			double decayFactor = getEditedSection().getDouble("upgrade.decay-factor", 1.0);

			ItemStack decayItem = new ItemStack(Material.REDSTONE_TORCH);
			ItemMeta decayItemMeta = decayItem.getItemMeta();
			decayItemMeta.setDisplayName(ChatColor.RED + "成功率衰减配置");
			List<String> decayItemLore = new ArrayList<>();
			decayItemLore.add(ChatColor.GRAY + "启用成功率衰减，基于强化等级");
			decayItemLore.add(ChatColor.GRAY + "公式：基础几率 × (衰减系数 ^ 等级)");
			decayItemLore.add("");
			decayItemLore.add(ChatColor.GRAY + "衰减启用: " + (decayEnabled ? ChatColor.GREEN + "是" : ChatColor.RED + "否"));
			decayItemLore.add(ChatColor.GRAY + "衰减系数: " + ChatColor.GOLD + decayFactor);
			decayItemLore.add("");
			decayItemLore.add(ChatColor.YELLOW + AltChar.listDash + " 左键切换开关");
			decayItemLore.add(ChatColor.YELLOW + AltChar.listDash + " 右键设置衰减系数");
			decayItemMeta.setLore(decayItemLore);
			decayItem.setItemMeta(decayItemMeta);
			inventory.setItem(28, decayItem);

			// ========== 掉级惩罚配置 ==========
			String downgradeRange = getEditedSection().getString("upgrade.downgrade-range", "");
			double downgradeChance = getEditedSection().getDouble("upgrade.downgrade-chance", 0);
			int downgradeAmount = getEditedSection().getInt("upgrade.downgrade-amount", 1);
			String downgradeProtectKey = getEditedSection().getString("upgrade.downgrade-protect-key", "");

			ItemStack downgradeItem = new ItemStack(Material.ANVIL);
			ItemMeta downgradeItemMeta = downgradeItem.getItemMeta();
			downgradeItemMeta.setDisplayName(ChatColor.GOLD + "掉级惩罚");
			List<String> downgradeItemLore = new ArrayList<>();
			downgradeItemLore.add(ChatColor.GRAY + "配置强化失败时的掉级惩罚。");
			downgradeItemLore.add("");
			downgradeItemLore.add(ChatColor.GRAY + "触发等级范围: " + (downgradeRange.isEmpty() ? ChatColor.RED + "未设置" : ChatColor.GOLD + downgradeRange));
			downgradeItemLore.add(ChatColor.GRAY + "触发几率: " + ChatColor.GOLD + downgradeChance + "%");
			downgradeItemLore.add(ChatColor.GRAY + "掉落等级数: " + ChatColor.GOLD + downgradeAmount + " 级");
			downgradeItemLore.add(ChatColor.GRAY + "保护标签: " + (downgradeProtectKey.isEmpty() ? ChatColor.RED + "无" : ChatColor.GOLD + downgradeProtectKey));
			downgradeItemLore.add("");
			downgradeItemLore.add(ChatColor.YELLOW + AltChar.listDash + " 左键设置等级范围（如 5-15）");
			downgradeItemLore.add(ChatColor.YELLOW + AltChar.listDash + " 右键设置几率/数量/保护标签");
			downgradeItemMeta.setLore(downgradeItemLore);
			downgradeItem.setItemMeta(downgradeItemMeta);
			inventory.setItem(30, downgradeItem);

			// ========== 碎裂惩罚配置 ==========
			String breakRange = getEditedSection().getString("upgrade.break-range", "");
			double breakChance = getEditedSection().getDouble("upgrade.break-chance", 0);
			String breakProtectKey = getEditedSection().getString("upgrade.break-protect-key", "");

			ItemStack breakItem = new ItemStack(Material.TNT);
			ItemMeta breakItemMeta = breakItem.getItemMeta();
			breakItemMeta.setDisplayName(ChatColor.DARK_RED + "碎裂惩罚");
			List<String> breakItemLore = new ArrayList<>();
			breakItemLore.add(ChatColor.GRAY + "配置强化失败时的碎裂（销毁）惩罚。");
			breakItemLore.add("");
			breakItemLore.add(ChatColor.GRAY + "触发等级范围: " + (breakRange.isEmpty() ? ChatColor.RED + "未设置" : ChatColor.GOLD + breakRange));
			breakItemLore.add(ChatColor.GRAY + "触发几率: " + ChatColor.GOLD + breakChance + "%");
			breakItemLore.add(ChatColor.GRAY + "保护标签: " + (breakProtectKey.isEmpty() ? ChatColor.RED + "无" : ChatColor.GOLD + breakProtectKey));
			breakItemLore.add("");
			breakItemLore.add(ChatColor.YELLOW + AltChar.listDash + " 左键设置等级范围（如 10-20）");
			breakItemLore.add(ChatColor.YELLOW + AltChar.listDash + " 右键设置几率/保护标签");
			breakItemMeta.setLore(breakItemLore);
			breakItem.setItemMeta(breakItemMeta);
			inventory.setItem(32, breakItem);

			// ========== 禁用背包强化配置 ==========
			boolean disableBackpack = getEditedSection().getBoolean("upgrade.disable-backpack", false);
			ItemStack disableBackpackItem = new ItemStack(Material.CHEST);
			ItemMeta disableBackpackMeta = disableBackpackItem.getItemMeta();
			disableBackpackMeta.setDisplayName(ChatColor.AQUA + "禁用背包强化");
			List<String> disableBackpackLore = new ArrayList<>();
			disableBackpackLore.add(ChatColor.GRAY + "启用后，玩家无法在背包中");
			disableBackpackLore.add(ChatColor.GRAY + "通过点击强化石来强化此物品。");
			disableBackpackLore.add(ChatColor.GRAY + "他们必须使用强化命令或工作台。");
			disableBackpackLore.add("");
			disableBackpackLore.add(ChatColor.GRAY + "当前状态: " + (disableBackpack ? ChatColor.GREEN + "已禁用" : ChatColor.RED + "未禁用"));
			disableBackpackLore.add("");
			disableBackpackLore.add(ChatColor.YELLOW + AltChar.listDash + " 点击切换此设置");
			disableBackpackMeta.setLore(disableBackpackLore);
			disableBackpackItem.setItemMeta(disableBackpackMeta);
			inventory.setItem(34, disableBackpackItem);
		}
	}

	@Override
	public void whenClicked(InventoryClickEvent event) {
		ItemStack item = event.getCurrentItem();

		event.setCancelled(true);
		if (event.getInventory() != event.getClickedInventory() || !MMOUtils.isMetaItem(item, false))
			return;

		// 强化成功几率
		if (item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "强化成功几率")) {
			if (event.getAction() == InventoryAction.PICKUP_ALL)
				new StatEdition(this, ItemStats.UPGRADE, "rate").enable("请在聊天栏输入成功几率（百分比）。");

			if (event.getAction() == InventoryAction.PICKUP_HALF && getEditedSection().contains("upgrade.success")) {
				getEditedSection().set("upgrade.success", null);
				registerTemplateEdition();
				player.sendMessage(MMOItems.plugin.getPrefix() + "成功几率已重置。");
			}
		}

		// 最大强化等级
		if (item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "最大强化等级")) {
			if (event.getAction() == InventoryAction.PICKUP_ALL)
				new StatEdition(this, ItemStats.UPGRADE, "max").enable("请在聊天栏输入最大等级。");

			if (event.getAction() == InventoryAction.PICKUP_HALF && getEditedSection().contains("upgrade.max")) {
				getEditedSection().set("upgrade.max", null);
				registerTemplateEdition();
				player.sendMessage(MMOItems.plugin.getPrefix() + "最大等级已重置。");
			}
		}

		// 最小强化等级
		if (item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "最小强化等级")) {
			if (event.getAction() == InventoryAction.PICKUP_ALL)
				new StatEdition(this, ItemStats.UPGRADE, "min").enable("请在聊天栏输入最小等级。");

			if (event.getAction() == InventoryAction.PICKUP_HALF && getEditedSection().contains("upgrade.min")) {
				getEditedSection().set("upgrade.min", null);
				registerTemplateEdition();
				player.sendMessage(MMOItems.plugin.getPrefix() + "最小等级已重置。");
			}
		}

		// 强化模板
		if (item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "强化模板")) {
			if (event.getAction() == InventoryAction.PICKUP_ALL)
				new StatEdition(this, ItemStats.UPGRADE, "template").enable("请在聊天栏输入强化模板ID。");

			if (event.getAction() == InventoryAction.PICKUP_HALF && getEditedSection().contains("upgrade.template")) {
				getEditedSection().set("upgrade.template", null);
				registerTemplateEdition();
				player.sendMessage(MMOItems.plugin.getPrefix() + "强化模板已重置。");
			}
		}

		// 强化参考标识
		if (item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "强化参考标识")) {
			if (event.getAction() == InventoryAction.PICKUP_ALL)
				new StatEdition(this, ItemStats.UPGRADE, "ref").enable("请在聊天栏输入强化参考标识。");

			if (event.getAction() == InventoryAction.PICKUP_HALF && getEditedSection().contains("upgrade.reference")) {
				getEditedSection().set("upgrade.reference", null);
				registerTemplateEdition();
				player.sendMessage(MMOItems.plugin.getPrefix() + "强化参考标识已重置。");
			}
		}

		// 仅工作台强化
		if (item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "仅工作台强化")) {
			boolean bool = !getEditedSection().getBoolean("upgrade.workbench");
			getEditedSection().set("upgrade.workbench", bool);
			registerTemplateEdition();
			player.sendMessage(MMOItems.plugin.getPrefix()
					+ (bool ? "物品现在只能通过配方强化。" : "物品现在可以使用消耗品强化。"));
		}

		// 失败时销毁
		if (item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "失败时销毁?")) {
			if (event.getAction() == InventoryAction.PICKUP_ALL) {
				boolean bool = !getEditedSection().getBoolean("upgrade.destroy");
				getEditedSection().set("upgrade.destroy", bool);
				registerTemplateEdition();
				player.sendMessage(MMOItems.plugin.getPrefix()
						+ (bool ? "强化失败时物品将被销毁。" : "强化失败时物品不会被销毁。"));
				return;
			}
			if (event.getAction() == InventoryAction.PICKUP_HALF) {
				new StatEdition(this, ItemStats.UPGRADE, "destroy-protect").enable("请输入销毁保护标签（留空清除）。");
				return;
			}
		}

		// ========== 成功率衰减配置处理 ==========
		if (item.getItemMeta().getDisplayName().equals(ChatColor.RED + "成功率衰减配置")) {
			if (event.getAction() == InventoryAction.PICKUP_ALL) {
				// 切换衰减开关
				boolean decayEnabled = !getEditedSection().getBoolean("upgrade.decay-enabled", false);
				getEditedSection().set("upgrade.decay-enabled", decayEnabled);
				registerTemplateEdition();
				player.sendMessage(MMOItems.plugin.getPrefix() + "成功率衰减已" + (decayEnabled ? "启用" : "禁用") + "。");
			}
			if (event.getAction() == InventoryAction.PICKUP_HALF) {
				new StatEdition(this, ItemStats.UPGRADE, "decay-factor").enable("请在聊天栏输入衰减系数（如 0.95）。");
			}
		}

		// ========== 掉级惩罚配置处理 ==========
		if (item.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "掉级惩罚")) {
			if (event.getAction() == InventoryAction.PICKUP_ALL) {
				new StatEdition(this, ItemStats.UPGRADE, "downgrade-range").enable("请输入掉级等级范围（如 5-15）。");
			}
			if (event.getAction() == InventoryAction.PICKUP_HALF) {
				new StatEdition(this, ItemStats.UPGRADE, "downgrade-config").enable("请输入：几率,数量,保护标签（如 30,1,sword-prot）");
			}
		}

		// ========== 碎裂惩罚配置处理 ==========
		if (item.getItemMeta().getDisplayName().equals(ChatColor.DARK_RED + "碎裂惩罚")) {
			if (event.getAction() == InventoryAction.PICKUP_ALL) {
				new StatEdition(this, ItemStats.UPGRADE, "break-range").enable("请输入碎裂等级范围（如 10-20）。");
			}
			if (event.getAction() == InventoryAction.PICKUP_HALF) {
				new StatEdition(this, ItemStats.UPGRADE, "break-config").enable("请输入：几率,保护标签（如 15,sword-break-prot）");
			}
		}

		// ========== 禁用背包强化配置处理 ==========
		if (item.getItemMeta().getDisplayName().equals(ChatColor.AQUA + "禁用背包强化")) {
			boolean bool = !getEditedSection().getBoolean("upgrade.disable-backpack", false);
			getEditedSection().set("upgrade.disable-backpack", bool);
			registerTemplateEdition();
			player.sendMessage(MMOItems.plugin.getPrefix()
					+ (bool ? "已禁用背包强化。玩家必须使用强化命令。"
							: "已启用背包强化。玩家可以在背包中使用强化石。"));
		}
	}
}
