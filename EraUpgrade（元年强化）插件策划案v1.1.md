# EraUpgrade（元年强化）插件策划案 v1.1（MMOItems 原生版）

**文档版本**：v1.1
**适用对象**：服务器技术开发 / 维护人员 / 策划
**核心依赖**：Paper（1.20+）、MMOItems（本仓库版本）、MythicLib
**可选依赖**：DrcomoCoreLib（经济消耗功能）、PlaceholderAPI（概率公式功能）

> 本文档将"EraUpgrade（元年强化）"从"独立原创插件方案"改写为"MMOItems 原生强化系统扩展方案"。
> 结论：不再单独发布 EraUpgrade 插件；强化能力作为 MMOItems 的原生功能入口（配置/指令/GUI/Stat）交付。

### 实现对齐约束（甲方硬性要求）

- 以下策划案中的功能定义、配置项含义、使用边界 = **最低约束标准**，实现不得削弱或违背其语义。
- 插件已实现的新增能力、优化实现或更合理的配置/交互，可在不破坏策划案核心目标与业务逻辑的前提下扩展。

### 对比原策划案的待做清单（实现状态）

> 来源：对照 `EraUpgrade元年强化策划案.md`，当前 MMOItems 原生实现覆盖情况。

| # | 功能 | 状态 | 实现说明 |
|---|------|------|----------|
| 1 | 强化石匹配体系 | ✅ 已有 | MMOItems 原生支持 `upgrade.reference` 匹配机制 |
| 2 | 额外概率加成（权限/PAPI 公式） | ✅ 已实现 | `UpgradeChanceBonusCalculator` 支持 PAPI 公式 + 权限分档 + 最大上限 |
| 3 | 经济消耗（Vault/PlayerPoints） | ✅ 已实现 | `UpgradeEconomyHandler` 通过 DrcomoCoreLib 集成，支持按等级段配置费用 |
| 4 | 失败惩罚梯度（全局级别） | ✅ 已实现 | `GlobalPenaltyConfig` 提供全局惩罚梯度，物品配置优先于全局配置 |
| 5 | 强化转移规则收紧 | ✅ 已实现 | `UpgradeTransferService` 支持 strict/loose/whitelist 三种模式 |
| 6 | 强化日志查询与持久化 | ✅ 已实现 | `UpgradeLogManager` 异步 YAML 存储，支持按玩家/时间查询 |
| 7 | 强化外观特效与权限 | ✅ 已实现 | `UpgradeEffectsPlayer` 支持成功/失败/碎裂/保底特效，等级段额外粒子，权限控制 |
| 8 | 每日限制动态上限（权限分档） | ✅ 已实现 | `DailyLimitManager` 支持权限分档配置不同上限，支持 MySQL 持久化 |
| 9 | UI 细节符合策划文案 | ✅ 已实现 | `UpgradeStationDisplay` 显示经济消耗、全局概率加成、保底进度等 |

---

## 0. 变更摘要（从独立插件 → 原生 MMOItems）

### 0.1 不再存在的内容（删除/弃用）

- 独立插件的数据目录、独立命令命名空间、独立数据库。
- 以"自研物品体系"为核心的强化对象解析。

### 0.2 替换后的承载点（MMOItems 原生）

- **目标装备**：在 MMOItems 物品配置中通过 `upgrade:` 段定义（属于装备类类型，如 `SWORD/ARMOR/...`）。
- **强化石/辅料石/保护符/转移石**：作为 MMOItems `CONSUMABLE` 类型物品，通过对应 Stat/NBT 生效。
- **核心逻辑入口**：MMOItems 内置 `UpgradeService`，被背包强化、命令强化、GUI 工作台等复用调用。
- **关键全局配置**：`plugins/MMOItems/config.yml` 的 `item-upgrading.*`。
- **GUI 配置**：`plugins/MMOItems/default/upgrade-station.yml`。

---

## 1. 【契约】（强化系统对外行为）

### 1.1 输入

- 玩家：执行强化/转移操作的主体。
- 目标装备：必须是 MMOItems 物品，且带有 `UPGRADE` 数据（物品配置含 `upgrade:`）。
- 消耗物：
  - 强化石：`CONSUMABLE` 且带 `UPGRADE` 数据，且 `upgrade.reference` 与目标装备一致。
  - 辅料石：`CONSUMABLE`，通过 Stat 提供加成/保护/跳级效果。
  - 保护符：`CONSUMABLE`，通过 `upgrade-protection` 标签抵消惩罚。
  - 转移石：`CONSUMABLE`，通过 `transfer-stone: true` 作为转移消耗。

### 1.2 输出

- 成功：目标装备等级变化（普通 +1 / 直达 / 直达石额外跳级），并更新 ItemStack。
- 失败：可能触发惩罚（碎裂/掉级/销毁）或被保护符/保护模式拦截。
- 附加输出：保底提示、全服通报、强化后自动绑定提示、消耗提示、特效播放。

### 1.3 边界与失败语义

- 若目标物品不满足"MMOItems + 可强化"，操作失败并给出中文错误提示。
- 成功率统一裁剪到 `[0, 1]`；直达/强制等行为受权限与上限控制。
- **每日限制**：启用时，超过上限直接拒绝（不消耗材料）。
- **经济消耗**：余额不足时直接拒绝（不消耗材料）。

### 1.4 幂等性

- 非幂等：每次强化/转移都会消耗材料并改变物品状态（或触发惩罚）。

### 1.5 性能目标

- 单次操作仅扫描玩家背包（线性），不引入阻塞 IO。
- 日志写入采用异步队列，不阻塞主线程。

---

## 2. 核心玩法与玩家流程（原生 MMOItems 入口）

### 2.1 背包强化（右键强化石）

- 玩家在背包中对"目标装备"使用"强化石（消耗品）"触发强化。
- 若目标装备配置 `upgrade.disable-backpack: true`，则禁止背包强化，必须走命令/GUI。

### 2.2 命令强化（管理员/NPC/活动）

- 命令：`/mi item upgrade <common|protect> <chance> [-free] [-force] [-direct:等级]`
- 适用场景：NPC 扣费后执行、活动倍率、管理员补偿等。
- `protect` 模式：失败不触发惩罚（仍可能消耗强化石，除非 `-free`）。

### 2.3 强化工作台 GUI（可视化强化）

- 命令：`/mi item station`
- GUI 支持：目标装备/强化石槽、辅料槽（幸运/保护/直达）、成功率进度条、预览、信息面板、冷却与防刷保护。
- 信息面板显示：每日限制、失败风险、保护石效果、经济消耗、全局概率加成。
- GUI 配置文件：`plugins/MMOItems/default/upgrade-station.yml`

### 2.4 强化等级转移（物品间）

- 命令：`/mi item transfer [-free] [-ratio:0-1]`
- 操作：主手=源物品（提取等级），副手=目标物品（接收等级）。
- 规则（当前实现，支持三种模式）：
  - **strict 模式**：仅同类型物品可转移
  - **loose 模式**（默认）：同类型 / 同父类型 / 父子类型关系可转移
  - **whitelist 模式**：使用白名单配置 `transfer-compatibility.whitelist`
- 转移等级 = `floor(源等级 × ratio)`
- 叠加模式（`stack-mode: true`）：叠加到目标当前等级；覆盖模式：直接设置为转移等级
- 源物品等级处理（`reset-source: true`）：重置为 0；否则保留
- 非 `-free` 时消耗 1 个转移石（`transfer-stone: true`）

---

## 3. 配置规范（以 MMOItems 为唯一配置源）

### 3.1 目标装备：`upgrade:` 段（items/*.yml）

> 关键字段（以本仓库实现为准）：

```yaml
upgrade:
  template: weapon-default
  reference: sword
  max: 15
  min: 0
  success: 80
  workbench: false
  destroy: false

  # 成功率衰减（实际成功率 = success × decay-factor^level）
  decay-enabled: true
  decay-factor: 0.95

  # 掉级惩罚（区间命中后才判定）
  downgrade-range: "5-15"
  downgrade-chance: 30
  downgrade-amount: 2
  downgrade-protect-key: DOWNGRADE_PROTECT

  # 碎裂惩罚（区间命中后才判定）
  break-range: "10-15"
  break-chance: 10
  break-protect-key: BREAK_PROTECT

  # 销毁失败（destroy: true 时生效，可用保护符拦截）
  destroy-protect-key: DESTROY_PROTECT

  # 禁用背包强化（仅允许命令/GUI）
  disable-backpack: false
```

### 3.2 强化石：同样使用 `upgrade:`（items/consumable.yml 或 consumables/*.yml）

```yaml
UPGRADE_STONE_SWORD:
  base:
    material: NETHER_STAR
    name: "&b武器强化石"
    upgrade:
      reference: sword
      success: 100
      workbench: false
      max: 0
```

### 3.3 辅料石：通过 Stat 提供额外效果（消耗品）

```yaml
LUCKY_STONE:
  base:
    material: EMERALD
    name: "&a幸运石"
    auxiliary-chance-bonus: 10

PROTECT_STONE:
  base:
    material: LAPIS_LAZULI
    name: "&b保护石"
    auxiliary-protection: 50

DIRECT_STONE:
  base:
    material: DIAMOND
    name: "&d直达石"
    auxiliary-direct-up-chance: 30
    auxiliary-direct-up-levels: 1
```

### 3.4 保护符：通过 `upgrade-protection` 抵消惩罚（消耗品）

```yaml
DOWNGRADE_PROTECT_SCROLL:
  base:
    material: PAPER
    name: "&a掉级保护符"
    upgrade-protection: DOWNGRADE_PROTECT
```

### 3.5 转移石：通过 `transfer-stone` 标记（消耗品）

```yaml
TRANSFER_STONE:
  base:
    material: ENDER_EYE
    name: "&d转移石"
    transfer-stone: true
```

### 3.6 全局配置：`config.yml` → `item-upgrading.*`

```yaml
item-upgrading:
  # ===== 保底机制配置 =====
  guarantee:
    enabled: true
    threshold: 30
    reset-on-success: true
    expire-hours: 0  # <=0 表示不过期

  # ===== 每日限制配置 =====
  daily-limit:
    enabled: false
    default-max: 50
    reset-hour: 0
    bypass-permission: "mmoitems.upgrade.bypass-daily"
    persist-enabled: true
    # 权限分档配置（优先级高的先匹配）
    tiers:
      svip:
        permission: "mmoitems.upgrade.limit.svip"
        max: 200
        priority: 30
      vip:
        permission: "mmoitems.upgrade.limit.vip"
        max: 100
        priority: 20
      default:
        permission: ""
        max: 50
        priority: 0

  # ===== 经济消耗配置 =====
  economy-cost:
    enabled: false
    type: "vault"  # vault 或 playerpoints
    tiers:
      "0-5": 100
      "6-10": 500
      "11-15": 2000
      "16-20": 5000
      "default": 100

  # ===== 强化后自动绑定配置 =====
  auto-bind-on-upgrade:
    enabled: false
    level: 1
    show-message: true

  # ===== 全服通报配置 =====
  broadcast:
    enabled: false
    levels: [12, 15, 20, 25, 30]
    message: "&6[强化通报] &a{player} 的 &e{item}&a 强化到 &c+{level}&a 级！"

  # ===== 强化特效配置 =====
  effects:
    enabled: true
    success:
      particle: "VILLAGER_HAPPY"
      particle-count: 30
      sound: "ENTITY_PLAYER_LEVELUP"
      volume: 1.0
      pitch: 1.5
    failure:
      particle: "SMOKE_NORMAL"
      particle-count: 20
      sound: "ENTITY_ITEM_BREAK"
      volume: 1.0
      pitch: 1.0
    break:
      particle: "EXPLOSION_LARGE"
      particle-count: 1
      sound: "ENTITY_GENERIC_EXPLODE"
      volume: 1.0
      pitch: 0.5
    guarantee:
      particle: "TOTEM"
      particle-count: 50
      sound: "UI_TOAST_CHALLENGE_COMPLETE"
      volume: 1.0
      pitch: 1.0
    particle-permission: ""  # 空字符串表示无权限限制
    level-effects:
      "15+":
        particle: "FIREWORKS_SPARK"
        particle-count: 50
      "20+":
        particle: "END_ROD"
        particle-count: 100

  # ===== 强化日志配置 =====
  upgrade-log:
    enabled: false
    retention-days: 30

  # ===== 额外概率加成配置 =====
  chance-bonus:
    enabled: false
    formula: "0"  # 支持 PAPI 变量和数学表达式
    permission-bonus:
      svip:
        permission: "mmoitems.upgrade.bonus.svip"
        bonus: 15
        priority: 30
      vip:
        permission: "mmoitems.upgrade.bonus.vip"
        bonus: 10
        priority: 20
      default:
        permission: ""
        bonus: 0
        priority: 0
    max-bonus: 50

  # ===== 全局惩罚梯度配置 =====
  global-penalty:
    enabled: false
    tiers:
      "0-5":
        type: "none"
      "6-10":
        type: "downgrade"
        chance: 0.1
        amount: 1
      "11-15":
        type: "downgrade"
        chance: 0.2
        amount: 1
        break-chance: 0.05
      "16-20":
        type: "downgrade"
        chance: 0.3
        amount: 2
        break-chance: 0.1
      "21+":
        type: "downgrade"
        chance: 0.5
        amount: 3
        break-chance: 0.2
      "default":
        type: "none"

  # ===== 强化等级转移配置 =====
  transfer:
    enabled: true
    mode: "loose"  # strict | loose | whitelist
    ratio: 0.8
    max-level: -1  # -1 表示无限制
    stack-mode: true
    reset-source: true

  # ===== 转移类型兼容矩阵（whitelist 模式）=====
  transfer-compatibility:
    enabled: false
    allow-legacy-auto: true
    whitelist: []  # 格式: "FROM->TO"，如 "SWORD->LONG_SWORD"

# ===== 数据库设置（每日限制持久化）=====
database:
  daily-limit:
    enabled: false
    host: "127.0.0.1"
    port: 3306
    database: "mmoitems"
    user: "root"
    password: ""
    table: "mmoitems_daily_limit"
    pool-size: 5
    params: "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
```

---

## 4. 权限与命令（原生 /mi 命名空间）

### 4.1 指令

- `/mi item station`：打开强化工作台 GUI
- `/mi item upgrade <common|protect> <chance> [-free] [-force] [-direct:等级]`：命令强化
- `/mi item transfer [-free] [-ratio:0-1]`：强化等级转移

### 4.2 权限节点（当前实现）

| 权限节点 | 说明 |
|----------|------|
| `mmoitems.command.item.station` | 使用强化工作台 |
| `mmoitems.command.item.upgrade` | 使用命令强化 |
| `mmoitems.command.item.upgrade.protect` | 使用保护模式 |
| `mmoitems.command.item.upgrade.free` | 免费强化（不消耗材料） |
| `mmoitems.command.item.upgrade.force` | 强制强化（忽略等级上限） |
| `mmoitems.command.item.upgrade.direct` | 直达强化 |
| `mmoitems.command.item.transfer` | 使用等级转移 |
| `mmoitems.command.item.transfer.free` | 免费转移 |
| `mmoitems.command.item.transfer.ratio` | 自定义转移比例 |
| `mmoitems.upgrade.bypass-daily` | 绕过每日限制 |
| `mmoitems.upgrade.limit.vip` | VIP 每日上限分档 |
| `mmoitems.upgrade.limit.svip` | SVIP 每日上限分档 |
| `mmoitems.upgrade.bonus.vip` | VIP 概率加成 |
| `mmoitems.upgrade.bonus.svip` | SVIP 概率加成 |

---

## 5. 数据存储与可审计性

### 5.1 物品侧（NBT，随物品流转）

- 强化数据：由 MMOItems 的 `UPGRADE` NBT 记录（包含等级/模板等）。
- 保底数据（连续失败计数）：
  - `MMOITEMS_UPGRADE_CONSECUTIVE_FAILS`
  - `MMOITEMS_UPGRADE_LAST_TIME`

### 5.2 玩家侧（每日限制）

- **内存模式**：随重载/重启丢失
- **文件持久化**：`plugins/MMOItems/upgrade-daily-limit.yml`
- **数据库持久化**：MySQL 表 `mmoitems_daily_limit`（需配置 `database.daily-limit`）

### 5.3 强化日志（新增）

- **存储位置**：`plugins/MMOItems/upgrade-logs/{日期}.yml`
- **记录内容**：
  - 玩家 UUID / 名称
  - 物品类型 / ID / 名称
  - 强化前后等级
  - 成功/失败状态
  - 惩罚类型
  - 使用强化石数量
  - 经济消耗
  - 是否触发保底
  - 时间戳
- **自动清理**：超过 `retention-days` 的日志文件自动删除

---

## 6. 核心类说明

### 6.1 强化服务层

| 类名 | 职责 |
|------|------|
| `UpgradeService` | 强化核心逻辑入口，处理成功/失败判定、惩罚应用、特效播放、日志记录 |
| `UpgradeContext` | 强化上下文，封装玩家/目标/选项等参数 |
| `UpgradeResult` | 强化结果，包含状态/等级变化/消耗等信息 |

### 6.2 扩展管理器

| 类名 | 职责 |
|------|------|
| `GuaranteeManager` | 保底机制管理 |
| `DailyLimitManager` | 每日限制管理（含权限分档） |
| `DailyLimitTier` | 权限分档配置实体 |
| `UpgradeEconomyHandler` | 经济消耗处理（DrcomoCoreLib 集成） |
| `UpgradeChanceBonusCalculator` | 概率加成计算（PAPI 公式 + 权限） |
| `GlobalPenaltyConfig` | 全局惩罚梯度配置 |
| `UpgradeLogManager` | 日志管理（异步写入） |
| `UpgradeLogEntry` | 日志实体（Builder 模式） |
| `UpgradeEffectsPlayer` | 特效播放器 |
| `UpgradeTransferService` | 等级转移服务 |

### 6.3 GUI 层

| 类名 | 职责 |
|------|------|
| `UpgradeStationGUI` | 强化工作台主类 |
| `UpgradeStationDisplay` | GUI 展示逻辑（预览/进度条/信息面板） |

---

## 7. 参考文档（仓库内）

- `UPGRADE_COMMAND_GUIDE.md`：命令强化/禁用背包强化等使用说明
- `UPGRADE_EXTENSION_GUIDE.md`：GUI、辅料、保底、每日限制、通报、绑定、转移等扩展说明
- `UPGRADE_PENALTY_DESIGN.md`：惩罚机制字段与判定逻辑（注意以实现字段为准）

---

## 8. 【TODO】

- TODO(后续增强) 强化日志命令查询接口（`/mi item upgrade log [player] [days]`）
- TODO(后续增强) 转移操作二次确认 GUI
- TODO(后续增强) 强化石按权限限制（`eraupgrade.stone.{stone_id}`）
