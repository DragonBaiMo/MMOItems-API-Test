# MMOItems 强化系统扩展指南

## 一、功能概述

本次扩展在 MMOItems 原有强化系统基础上，新增以下功能：

| 功能模块 | 描述 |
|---------|------|
| 辅料系统 | 幸运石（成功率+）、保护石（降低惩罚）、直达石（跳级） |
| 保底机制 | 连续失败达到阈值后必定成功 |
| 每日限制 | 限制玩家每日强化次数 |
| 全服通报 | 达到指定等级时全服广播 |
| 强化工作台 | 可视化 GUI 强化界面 |
| 等级转移 | 物品间强化等级转移 |

---

## 二、用户使用指南

### 2.1 辅料物品配置

在 `consumable.yml` 中配置辅料消耗品：

```yaml
# 幸运石 - 增加成功率
LUCKY_STONE:
  base:
    material: EMERALD
    name: '&a幸运石'
    lore:
      - '&7强化时使用，提升成功率 +10%'
    auxiliary-chance-bonus: 10

# 保护石 - 降低惩罚概率
PROTECT_STONE:
  base:
    material: LAPIS_LAZULI
    name: '&b保护石'
    lore:
      - '&7强化时使用，降低惩罚概率 50%'
    auxiliary-protection: 50

# 直达石 - 成功时有概率跳级
DIRECT_STONE:
  base:
    material: DIAMOND
    name: '&d直达石'
    lore:
      - '&7强化时使用，30%概率额外升1级'
    auxiliary-direct-up-chance: 30
    auxiliary-direct-up-levels: 1

# 转移石 - 等级转移消耗材料
TRANSFER_STONE:
  base:
    material: ENDER_EYE
    name: '&d转移石'
    lore:
      - '&7用于转移强化等级'
    transfer-stone: true
```

### 2.2 config.yml 配置

在 `config.yml` 的 `item-upgrading` 节下添加：

```yaml
item-upgrading:
  # === 现有配置保持不变 ===
  name-suffix: ' &8(&e+#lvl#&8)'
  display-in-name: true

  # === 新增：保底机制 ===
  guarantee:
    enabled: true
    threshold: 30              # 连续失败30次后必定成功
    reset-on-success: true     # 成功后重置计数

  # === 新增：每日限制 ===
  daily-limit:
    enabled: true
    default-max: 50            # 默认每日最大强化次数
    reset-hour: 0              # 每日0点重置
    bypass-permission: "mmoitems.upgrade.bypass-daily"

  # === 新增：全服通报 ===
  broadcast:
    enabled: true
    levels: [10, 15, 20, 25, 30]  # 达到这些等级时全服通报
    message: "&6[强化通报] &a{player} 的 &e{item}&a 强化到 &c+{level}&a 级！"
```

### 2.3 强化工作台 GUI 界面

打开命令：`/mi item station`

**GUI 布局（6行54格）**：

```
┌─────────────────────────────────────────────────────┐
│ 第1行：紫色玻璃边框 + 中央标题（✦ 强化工作台 ✦）      │
├─────────────────────────────────────────────────────┤
│ 第2行：[装备槽] ─→ 强化预览 ─→ [强化石槽]           │
├─────────────────────────────────────────────────────┤
│ 第3行：分隔线 + 槽位标签                            │
├─────────────────────────────────────────────────────┤
│ 第4行：[成功率进度条] ████░░░ 65.3%                 │
├─────────────────────────────────────────────────────┤
│ 第5行：[幸运石] [保护石] [直达石] ─ 辅料槽          │
├─────────────────────────────────────────────────────┤
│ 第6行：[关闭] ─ [强化信息] ─ [点击强化按钮]        │
└─────────────────────────────────────────────────────┘
```

**视觉特色**：

1. **成功率可视化进度条**
   - 7格彩色进度条，实时显示成功率
   - 绿色（≥80%）→ 黄色（≥50%）→ 橙色（≥30%）→ 红色（<30%）

2. **强化预览（中央预览位置）** ⭐ 核心功能
   - **真实物品预览**：构建完整的强化后物品，继承所有原属性
   - **属性变化对比**：清晰展示每个属性的变化
     - 格式：`攻击力: 100.0 → 115.0 (+15.0)`
     - 绿色表示增加，红色表示减少
   - 等级变化：当前等级 → 强化后等级
   - 直达石效果预览（如：30% 概率直接到 +12）
   - 已达最大等级提示
   - **完整属性列表**：展示强化后物品的完整 lore

3. **成功率详细分解**（悬浮进度条查看）
   - 基础成功率
   - 等级衰减后成功率
   - 幸运石加成
   - 保底进度（如：12/30 次失败）
   - 保底触发提示（★ 已触发保底！必定成功 ★）

4. **强化信息面板**（底部书本图标）
   - 今日强化次数：已用 23/50，剩余 27 次
   - 失败风险：
     - ☠ 碎裂: 5.0%
     - ↓ 掉级: 15.0% (-1级)
   - 保护石效果：惩罚概率 -50%

5. **强化按钮状态**
   - 绿色（可强化）：显示"▶ 点击执行强化"
   - 红色（无法强化）：显示具体原因列表

**安全特性**：

- 防止 Shift+点击复制物品
- 防止数字键交换物品
- 防止快速连点刷物品（1秒冷却）
- 关闭/断线自动返还物品
- 物品一致性验证

**配置文件**：`plugins/MMOItems/default/upgrade-station.yml`

所有 GUI 内容均可配置：
- 标题、行数
- 所有槽位位置
- 所有物品（材质、名称、描述）
- 进度条颜色
- 所有消息文本
- 音效配置
- 安全选项

```yaml
# 示例配置
gui:
  title: "&5&l强化工作台 &8- &7放入物品开始强化"
  rows: 6

slots:
  target-item: 11
  upgrade-stone: 15
  preview: 13
  lucky-stone: 37
  protect-stone: 39
  direct-stone: 41
  upgrade-button: 43

# 预览消息配置
messages:
  preview-title: "&e⚡ 强化预览 ⚡"
  preview-original-item: "&f原物品: {name}"
  preview-stat-changes: "&6&l✦ 属性变化 ✦"
  preview-full-stats: "&7&o强化后完整属性:"
  preview-separator: "&8─────────────────"
  current-level: "&e当前等级: &f+{level}"
  after-level: "&a强化后: &f+{level}"

security:
  upgrade-cooldown: 1000        # 强化冷却（毫秒）
  return-items-on-close: true   # 关闭时返还物品
  return-items-on-quit: true    # 退出时返还物品
  block-shift-click: true       # 阻止Shift点击
  block-number-key: true        # 阻止数字键
  block-drag: false             # 阻止拖拽
```

**预览物品展示示例**：
```
⚡ 强化预览 ⚡

原物品: §6无尽之刃 +5

当前等级: +5
强化后: +6
最大等级: +15

✦ 属性变化 ✦
攻击力: 100.0 → 115.0 (+15.0)
攻击速度: 1.2 → 1.25 (+0.05)
暴击率: 15.0 → 17.5 (+2.5)

─────────────────
强化后完整属性:
[原物品完整lore...]
```

### 2.4 命令使用

#### 强化命令
```
/mi item upgrade [-protect] [-free] [-force] [-chance:X] [-direct:X]
```

| 参数 | 说明 | 权限 |
|-----|------|------|
| `-protect` | 防护模式，失败无惩罚 | `mmoitems.command.item.upgrade.protect` |
| `-free` | 免费模式，不消耗强化石 | `mmoitems.command.item.upgrade.free` |
| `-force` | 强制模式，突破等级上限 | `mmoitems.command.item.upgrade.force` |
| `-chance:X` | 设置成功率系数（0-1） | `mmoitems.command.item.upgrade.chance` |
| `-direct:X` | 直达指定等级 | `mmoitems.command.item.upgrade.direct` |

**示例**：
```
/mi item upgrade                    # 普通强化
/mi item upgrade -protect           # 防护模式强化
/mi item upgrade -direct:15 -free   # 免费直达+15
```

#### 强化工作台命令
```
/mi item station
```
权限：`mmoitems.command.item.station`

打开可视化强化界面，支持：
- 放入待强化物品
- 放入强化石
- 放入辅料（幸运石/保护石/直达石）
- 实时预览成功率
- 点击按钮执行强化

#### 等级转移命令
```
/mi item transfer [-free] [-ratio:X]
```

| 参数 | 说明 | 权限 |
|-----|------|------|
| `-free` | 免费模式，不消耗转移石 | `mmoitems.command.item.transfer.free` |
| `-ratio:X` | 自定义转移比例（0-1） | `mmoitems.command.item.transfer.ratio` |

**使用方法**：
1. 主手持有源物品（要提取等级的物品）
2. 副手持有目标物品（要接收等级的物品）
3. 执行命令

**转移规则**：
- 源物品与目标物品必须同类型（或同父类型）
- 目标等级 = 源等级 × 80%（默认，可通过 -ratio 修改）
- 源物品等级重置为 0
- 非免费模式需消耗转移石

### 2.4 权限节点

```yaml
# 基础权限
mmoitems.command.item.upgrade       # 使用强化命令
mmoitems.command.item.station       # 打开强化工作台
mmoitems.command.item.transfer      # 使用转移命令

# 强化命令扩展权限
mmoitems.command.item.upgrade.protect  # 使用 -protect 标志
mmoitems.command.item.upgrade.free     # 使用 -free 标志
mmoitems.command.item.upgrade.force    # 使用 -force 标志
mmoitems.command.item.upgrade.chance   # 使用 -chance 标志
mmoitems.command.item.upgrade.direct   # 使用 -direct 标志

# 转移命令扩展权限
mmoitems.command.item.transfer.free    # 使用 -free 标志
mmoitems.command.item.transfer.ratio   # 使用 -ratio 标志

# 每日限制绕过
mmoitems.upgrade.bypass-daily          # 绕过每日强化次数限制
```

---

## 三、开发者指南

### 3.1 架构概览

```
MMOItems-API/src/main/java/net/Indyuce/mmoitems/
├── api/upgrade/                         # 强化核心包
│   ├── UpgradeService.java              # 强化服务（核心逻辑）
│   ├── UpgradeContext.java              # 强化上下文（Builder模式）
│   ├── UpgradeResult.java               # 强化结果
│   ├── UpgradeMode.java                 # 强化模式枚举
│   ├── PenaltyResult.java               # 惩罚结果枚举
│   ├── guarantee/                       # 保底机制子包
│   │   ├── GuaranteeData.java           # 保底数据
│   │   └── GuaranteeManager.java        # 保底管理器
│   ├── limit/                           # 每日限制子包
│   │   ├── DailyLimitData.java          # 每日限制数据
│   │   └── DailyLimitManager.java       # 每日限制管理器
│   └── transfer/                        # 等级转移子包
│       ├── TransferResult.java          # 转移结果
│       └── UpgradeTransferService.java  # 转移服务
├── manager/
│   └── UpgradeManager.java              # 强化模板+子管理器
├── gui/
│   └── UpgradeStationGUI.java           # 强化工作台GUI
├── stat/
│   ├── AuxiliaryChanceBonus.java        # 幸运石属性
│   ├── AuxiliaryProtection.java         # 保护石属性
│   ├── AuxiliaryDirectUpChance.java     # 直达石概率属性
│   ├── AuxiliaryDirectUpLevels.java     # 直达石等级属性
│   └── TransferStone.java               # 转移石属性
└── command/mmoitems/item/
    ├── UpgradeCommandTreeNode.java      # 强化命令
    ├── UpgradeStationCommandTreeNode.java # 工作台命令
    └── TransferCommandTreeNode.java     # 转移命令
```

### 3.2 核心类说明

#### UpgradeService（强化服务）

核心入口方法：
```java
public static UpgradeResult performUpgrade(@NotNull UpgradeContext context)
```

**处理流程**：
1. 每日限制检查（DailyLimitManager）
2. 强化模板验证
3. 等级上限检查
4. 查找强化石
5. 计算成功率（含辅料加成）
6. 保底机制检查（GuaranteeManager）
7. 成功/失败判定
8. 记录每日次数
9. 处理成功（含直达石效果、全服通报）或失败（惩罚判定）

#### UpgradeContext（强化上下文）

使用 Builder 模式构建：
```java
UpgradeContext context = new UpgradeContext.Builder()
    .player(player)
    .targetItem(mmoItem)
    .targetData(upgradeData)
    .targetItemStack(itemStack)
    .mode(UpgradeMode.COMMON)
    .chanceModifier(1.0)
    .freeMode(false)
    .forceMode(false)
    .directLevel(0)
    // 辅料效果
    .auxiliaryChanceBonus(10)      // 幸运石加成
    .auxiliaryProtection(50)        // 保护石降低
    .auxiliaryDirectUpChance(30)    // 直达石概率
    .auxiliaryDirectUpLevels(1)     // 直达石跳级数
    .build();
```

#### GuaranteeManager（保底管理器）

保底数据存储在物品 NBT 中：
```java
// NBT 键
ItemStats.UPGRADE_CONSECUTIVE_FAILS.getNBTPath()  // MMOITEMS_UPGRADE_CONSECUTIVE_FAILS

// 检查是否触发保底
boolean isGuaranteed = guaranteeManager.isGuaranteed(itemStack);

// 记录失败/成功
guaranteeManager.recordFail(nbtItem);
guaranteeManager.recordSuccess(nbtItem);
```

#### DailyLimitManager（每日限制管理器）

```java
// 检查是否可以强化
boolean canUpgrade = dailyLimitManager.canUpgrade(player);

// 获取已用/最大次数
int used = dailyLimitManager.getUsedAttempts(player);
int max = dailyLimitManager.getMaxAttempts(player);

// 记录强化次数
dailyLimitManager.recordAttempt(player);
```

### 3.3 新增 Stat 属性

| Stat 类 | NBT 路径 | 类型 | 适用物品类型 |
|--------|---------|------|------------|
| AuxiliaryChanceBonus | MMOITEMS_AUXILIARY_CHANCE_BONUS | DoubleStat | consumable |
| AuxiliaryProtection | MMOITEMS_AUXILIARY_PROTECTION | DoubleStat | consumable |
| AuxiliaryDirectUpChance | MMOITEMS_AUXILIARY_DIRECT_UP_CHANCE | DoubleStat | consumable |
| AuxiliaryDirectUpLevels | MMOITEMS_AUXILIARY_DIRECT_UP_LEVELS | DoubleStat | consumable |
| TransferStone | MMOITEMS_TRANSFER_STONE | BooleanStat | consumable |

### 3.4 消息枚举

在 `Message.java` 中新增：
```java
// 保底相关
UPGRADE_GUARANTEE_TRIGGERED("&6保底触发！本次强化必定成功！")

// 直达石相关
UPGRADE_DIRECT_UP_TRIGGERED("&d直达石生效！额外升级 #levels# 级！")

// 每日限制
UPGRADE_DAILY_LIMIT_REACHED("&c今日强化次数已用尽 (#used#/#max#)")

// 全服通报
UPGRADE_BROADCAST("&6[强化通报] &a#player# 的 &e#item#&a 强化到 &c+#level#&a 级！")

// 转移相关
TRANSFER_SUCCESS("&a转移成功！&6#source#&a → &6#target#&a：+#from# → +#to#")
TRANSFER_NO_STONE("&c背包中没有转移石。")
TRANSFER_INCOMPATIBLE_TYPE("&c源物品与目标物品类型不兼容。")
TRANSFER_NO_LEVEL("&c源物品没有可转移的强化等级。")
```

### 3.5 扩展示例

#### 自定义辅料效果

创建新的 DoubleStat：
```java
public class CustomAuxiliaryStat extends DoubleStat {
    public CustomAuxiliaryStat() {
        super("CUSTOM_AUX", Material.GOLD_INGOT, "自定义辅料",
              new String[]{"自定义效果描述"},
              new String[]{"consumable"});
    }
}
```

在 `ItemStats.java` 中注册：
```java
CUSTOM_AUX = new CustomAuxiliaryStat(),
```

在 `UpgradeContext.Builder` 中添加字段：
```java
private double customAuxValue = 0;

public Builder customAuxValue(double value) {
    this.customAuxValue = value;
    return this;
}
```

在 `UpgradeService` 中使用：
```java
double customEffect = context.getCustomAuxValue();
// 应用自定义效果...
```

---

## 四、文件清单

### 4.1 新增文件

| 文件路径 | 功能描述 |
|---------|---------|
| `api/upgrade/UpgradeService.java` | 强化服务核心类 |
| `api/upgrade/UpgradeContext.java` | 强化上下文（Builder） |
| `api/upgrade/UpgradeResult.java` | 强化结果类 |
| `api/upgrade/UpgradeMode.java` | 强化模式枚举 |
| `api/upgrade/PenaltyResult.java` | 惩罚结果枚举 |
| `api/upgrade/guarantee/GuaranteeData.java` | 保底数据类 |
| `api/upgrade/guarantee/GuaranteeManager.java` | 保底管理器 |
| `api/upgrade/limit/DailyLimitData.java` | 每日限制数据类 |
| `api/upgrade/limit/DailyLimitManager.java` | 每日限制管理器 |
| `api/upgrade/transfer/TransferResult.java` | 转移结果类 |
| `api/upgrade/transfer/UpgradeTransferService.java` | 转移服务类 |
| `gui/UpgradeStationGUI.java` | 强化工作台 GUI |
| `stat/AuxiliaryChanceBonus.java` | 幸运石属性 |
| `stat/AuxiliaryProtection.java` | 保护石属性 |
| `stat/AuxiliaryDirectUpChance.java` | 直达石概率属性 |
| `stat/AuxiliaryDirectUpLevels.java` | 直达石等级属性 |
| `stat/TransferStone.java` | 转移石属性 |
| `stat/UpgradeConsecutiveFails.java` | 连续失败计数属性 |
| `command/.../UpgradeCommandTreeNode.java` | 强化命令 |
| `command/.../UpgradeStationCommandTreeNode.java` | 工作台命令 |
| `command/.../TransferCommandTreeNode.java` | 转移命令 |

### 4.2 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `ItemStats.java` | 注册新 Stat |
| `manager/UpgradeManager.java` | 添加 GuaranteeManager、DailyLimitManager |
| `api/util/message/Message.java` | 添加新消息枚举 |
| `command/.../ItemCommandTreeNode.java` | 注册新命令 |

---

## 五、常见问题

### Q1: 保底数据存储在哪里？
A: 存储在物品的 NBT 标签 `MMOITEMS_UPGRADE_CONSECUTIVE_FAILS` 中，跟随物品移动。

### Q2: 每日限制数据存储在哪里？
A: 存储在内存中，服务器重启后重置。可扩展为持久化存储。

### Q3: 辅料如何被识别？
A: 通过检查消耗品的对应 Stat 属性值（如 `AUXILIARY_CHANCE_BONUS > 0`）。

### Q4: 强化石和辅料有什么区别？
A: 强化石是执行强化的必要材料（有 `UPGRADE` 数据），辅料是可选的增强材料（有 `AUXILIARY_*` 属性）。

### Q5: 等级转移的类型兼容规则？
A:
- 同类型物品兼容
- 同父类型物品兼容（如 SWORD 和 LONG_SWORD）
- 父子类型关系兼容

---

## 六、版本信息

- **扩展版本**：1.0.0
- **基于 MMOItems 版本**：6.10.1-SNAPSHOT
- **作者**：MMOItems Team
- **更新日期**：2025-12-16
