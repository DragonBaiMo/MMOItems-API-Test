# MMOItems 强化惩罚系统设计文档

## 一、需求概述

在物品的 `upgrade` 配置中新增以下选项：
1. 概率衰减启用 - 启用强化概率衰减机制
2. 概率衰减系数 - 成功率 = 基础成功率 × (衰减系数 ^ 当前等级)
3. 掉级惩罚区间 - 强化失败时有概率掉级的等级区间
4. 碎裂惩罚区间 - 强化失败时有概率损毁的等级区间
5. 掉级惩罚几率
6. 碎裂惩罚几率  
7. 惩罚保护字段 - 消耗品可配置相同字段来免除惩罚

---

## 二、配置示例

### 2.1 物品配置 (item/sword.yml)
```yaml
DRAGON_SWORD:
  base:
    upgrade:
      template: weapon-default
      reference: sword
      max: 15
      min: 0
      success: 100
      # ===== 新增配置 =====
      decay-enabled: true          # 启用概率衰减
      decay-rate: 0.95             # 衰减系数 (实际成功率 = success × 0.95^level)
      downgrade-range: "5-15"      # 5~15级失败会触发掉级判定
      downgrade-chance: 30         # 掉级几率 30%
      destroy-range: "10-15"       # 10~15级失败会触发碎裂判定
      destroy-chance: 10           # 碎裂几率 10%
      protection-tag: "sword-prot" # 保护标签
```

### 2.2 消耗品配置 (item/consumable.yml)
```yaml
UPGRADE_PROTECTION_SCROLL:
  base:
    material: PAPER
    name: "&e强化保护卷轴"
    upgrade-protection: "sword-prot"  # 匹配保护标签
```

---

## 三、核心代码修改

### 3.1 修改 UpgradeData.java

**路径**: `MMOItems-API/src/main/java/net/Indyuce/mmoitems/stat/data/UpgradeData.java`

新增字段:
```java
// 概率衰减
private final boolean decayEnabled;
private final double decayRate;

// 惩罚区间 [min, max]
private final int downgradeRangeMin, downgradeRangeMax;
private final int destroyRangeMin, destroyRangeMax;

// 惩罚几率
private final double downgradeChance;
private final double destroyChance;

// 保护标签
@Nullable private final String protectionTag;
```

新增方法:
```java
// 计算实际成功率(考虑衰减)
public double getActualSuccess() {
    if (!decayEnabled || decayRate <= 0) return getSuccess();
    return getSuccess() * Math.pow(decayRate, level);
}

// 判断是否在掉级区间
public boolean isInDowngradeRange() {
    return level >= downgradeRangeMin && level <= downgradeRangeMax;
}

// 判断是否在碎裂区间  
public boolean isInDestroyRange() {
    return level >= destroyRangeMin && level <= destroyRangeMax;
}

// Getter methods...
```

### 3.2 修改 UpgradeStat.java

**路径**: `MMOItems-API/src/main/java/net/Indyuce/mmoitems/stat/UpgradeStat.java`

修改 `handleConsumableEffect` 方法中的强化失败逻辑:

```java
// 计算实际成功率(含衰减)
double actualSuccess = consumableSharpening.getSuccess() * targetSharpening.getActualSuccess();

if (RANDOM.nextDouble() > actualSuccess) {
    // 强化失败
    UpgradePenaltyResult result = handleUpgradePenalty(
        player, event, targetMMO, targetSharpening
    );
    
    switch (result) {
        case PROTECTED:
            Message.UPGRADE_FAIL_PROTECTED.format(ChatColor.YELLOW).send(player);
            break;
        case DOWNGRADED:
            Message.UPGRADE_FAIL_DOWNGRADE.format(ChatColor.RED).send(player);
            break;
        case DESTROYED:
            Message.UPGRADE_FAIL_DESTROY.format(ChatColor.RED).send(player);
            event.getCurrentItem().setAmount(0);
            break;
        case NONE:
        default:
            Message.UPGRADE_FAIL.format(ChatColor.RED).send(player);
            break;
    }
    return true;
}
```

### 3.3 新增 UpgradePenaltyHandler.java

**路径**: `MMOItems-API/src/main/java/net/Indyuce/mmoitems/api/upgrade/UpgradePenaltyHandler.java`

```java
public class UpgradePenaltyHandler {
    
    public enum PenaltyResult {
        NONE,       // 无惩罚
        PROTECTED,  // 被保护卷轴抵消
        DOWNGRADED, // 掉级
        DESTROYED   // 碎裂
    }
    
    public static PenaltyResult handle(
        Player player, 
        MMOItem item,
        UpgradeData data
    ) {
        boolean shouldDestroy = data.isInDestroyRange() 
            && RANDOM.nextDouble() < data.getDestroyChance();
        boolean shouldDowngrade = data.isInDowngradeRange()
            && RANDOM.nextDouble() < data.getDowngradeChance();
        
        if (!shouldDestroy && !shouldDowngrade) {
            return PenaltyResult.NONE;
        }
        
        // 检查保护物品
        if (data.getProtectionTag() != null) {
            ItemStack protection = findProtectionItem(
                player, data.getProtectionTag()
            );
            if (protection != null) {
                protection.setAmount(protection.getAmount() - 1);
                return PenaltyResult.PROTECTED;
            }
        }
        
        // 执行惩罚
        if (shouldDestroy) {
            return PenaltyResult.DESTROYED;
        }
        if (shouldDowngrade) {
            // 执行掉级
            item.getUpgradeTemplate().upgradeTo(item, data.getLevel() - 1);
            return PenaltyResult.DOWNGRADED;
        }
        
        return PenaltyResult.NONE;
    }
    
    @Nullable
    private static ItemStack findProtectionItem(Player player, String tag) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            NBTItem nbt = NBTItem.get(item);
            String itemTag = nbt.getString("MMOITEMS_UPGRADE_PROTECTION");
            if (tag.equals(itemTag)) return item;
        }
        return null;
    }
}
```

### 3.4 新增 UpgradeProtection Stat

**路径**: `MMOItems-API/src/main/java/net/Indyuce/mmoitems/stat/UpgradeProtection.java`

```java
public class UpgradeProtection extends StringStat {
    public UpgradeProtection() {
        super("UPGRADE_PROTECTION", Material.PAPER, 
            "Upgrade Protection",
            new String[]{"Protection tag for upgrade failure"},
            new String[]{"consumable"});
    }
}
```

### 3.5 修改 Message.java

新增消息:
```java
UPGRADE_FAIL_PROTECTED("Your protection scroll saved your item!"),
UPGRADE_FAIL_DOWNGRADE("Upgrade failed! Your #item# has been downgraded."),
UPGRADE_FAIL_DESTROY("Upgrade failed! Your #item# has been destroyed!"),
```

### 3.6 修改 UpgradingEdition.java

在GUI中添加新配置项的编辑入口。

---

## 四、文件修改清单

| 文件 | 操作 |
|------|------|
| `UpgradeData.java` | 修改 - 添加新字段和方法 |
| `UpgradeStat.java` | 修改 - 修改强化失败逻辑 |
| `UpgradePenaltyHandler.java` | 新增 |
| `UpgradeProtection.java` | 新增 |
| `Message.java` | 修改 - 添加新消息 |
| `UpgradingEdition.java` | 修改 - 添加GUI编辑 |
| `ItemStats.java` | 修改 - 注册新Stat |
| `UpgradingRecipe.java` | 修改 - 工作台强化也需支持 |
| `messages.yml` | 修改 - 添加消息配置 |

---

## 五、实现优先级

1. **Phase 1**: UpgradeData 扩展 + 配置解析
2. **Phase 2**: UpgradePenaltyHandler 实现
3. **Phase 3**: UpgradeStat 强化逻辑修改  
4. **Phase 4**: UpgradeProtection Stat + 保护物品检测
5. **Phase 5**: UpgradingRecipe 工作台支持
6. **Phase 6**: UpgradingEdition GUI 编辑
7. **Phase 7**: 消息国际化

---

## 六、注意事项

1. 概率衰减计算: `实际成功率 = 基础成功率 × 衰减系数^当前等级`
2. 惩罚判定顺序: 先判定碎裂，再判定掉级
3. 保护物品优先级: 先检查保护物品，有则消耗并免除所有惩罚
4. 向后兼容: 所有新字段都有默认值，不影响现有配置
