package net.Indyuce.mmoitems.manager;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.ConfigFile;
import net.Indyuce.mmoitems.api.UpgradeTemplate;
import net.Indyuce.mmoitems.api.upgrade.guarantee.GuaranteeManager;
import net.Indyuce.mmoitems.api.upgrade.limit.DailyLimitManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * 强化模板管理器
 * <p>
 * 管理强化模板、保底机制和每日限制功能
 * </p>
 */
public class UpgradeManager implements Reloadable {
	private final Map<String, UpgradeTemplate> templates = new HashMap<>();

	/**
	 * 保底机制管理器
	 */
	private GuaranteeManager guaranteeManager;

	/**
	 * 每日限制管理器
	 */
	private DailyLimitManager dailyLimitManager;

	public UpgradeManager() {
		reload();
	}

	public void reload() {
		templates.clear();

		FileConfiguration config = new ConfigFile("upgrade-templates").getConfig();
		for (String key : config.getKeys(false)) {

			// Register
			registerTemplate(new UpgradeTemplate(config.getConfigurationSection(key)));
		}

		// 初始化或重载保底管理器
		if (guaranteeManager == null) {
			guaranteeManager = new GuaranteeManager();
		} else {
			guaranteeManager.reload();
		}

		// 初始化或重载每日限制管理器
		if (dailyLimitManager == null) {
			dailyLimitManager = new DailyLimitManager();
		} else {
			dailyLimitManager.reload();
		}
	}

	public Collection<UpgradeTemplate> getAll() {
		return templates.values();
	}

	/**
	 * Get the <code>UpgradeTemplate</code> of this name.
	 * @return <code>null</code> if there is no such template loaded.
	 */
	@Nullable public UpgradeTemplate getTemplate(@NotNull String id) {
		return templates.get(id);
	}

	public boolean hasTemplate(String id) {
		return templates.containsKey(id);
	}

	public void registerTemplate(UpgradeTemplate template) {
		templates.put(template.getId(), template);
	}

	/**
	 * 获取保底机制管理器
	 *
	 * @return 保底管理器实例
	 */
	@NotNull
	public GuaranteeManager getGuaranteeManager() {
		return guaranteeManager;
	}

	/**
	 * 获取每日限制管理器
	 *
	 * @return 每日限制管理器实例
	 */
	@NotNull
	public DailyLimitManager getDailyLimitManager() {
		return dailyLimitManager;
	}
}
