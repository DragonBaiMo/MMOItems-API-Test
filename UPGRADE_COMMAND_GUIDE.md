# MMOItems 强化命令系统使用指南

## 目录
1. [功能概述](#功能概述)
2. [配置示例](#配置示例)
3. [命令用法](#命令用法)
4. [权限节点](#权限节点)
5. [使用场景](#使用场景)

---

## 功能概述

强化命令系统提供了以下核心功能：

### 1. 禁用背包强化
在物品配置中添加 `disable-backpack: true`，强制玩家使用命令进行强化，而不是直接右键强化石。

### 2. 强化命令
管理员可以通过命令为玩家强化物品，支持多种模式和选项。

### 3. 强化模式
- **common（普通模式）**: 失败时触发惩罚（掉级、碎裂、销毁）
- **protect（防护模式）**: 失败时跳过所有惩罚（需要权限）

### 4. 命令选项
- `-free`: 不消耗强化石
- `-force`: 可以突破最大等级限制
- `-direct:XX`: 成功时直达指定等级（消耗等级差数量的强化石）

---

## 配置示例

### 示例 1：基础强化物品（禁用背包强化）
```yaml
# 在 items/ 目录下的物品配置文件
type: SWORD
material: DIAMOND_SWORD
name: '&6传奇之剑'
upgrade:
  workbench: false          # 允许背包强化（但被下面的选项禁用）
  disable-backpack: true    # 禁用背包强化，强制使用命令
  template: MY_UPGRADE
  reference: LEGENDARY
  max: 15                   # 最大强化等级
  min: 0                    # 最小强化等级（掉级下限）
  success: 80               # 基础成功率 80%
  destroy: false            # 失败不销毁

  # 成功率衰减
  decay-enabled: true       # 启用成功率衰减
  decay-factor: 0.95        # 每级衰减 5%（0.95^等级）

  # 掉级惩罚配置
  downgrade-range: 5-15     # 在 +5 到 +15 之间失败可能掉级
  downgrade-chance: 30      # 30% 概率触发掉级
  downgrade-amount: 2       # 每次掉 2 级
  downgrade-protect-key: DOWNGRADE_PROTECT  # 掉级保护物品标签

  # 碎裂惩罚配置
  break-range: 10-15        # 在 +10 到 +15 之间失败可能碎裂
  break-chance: 10          # 10% 概率触发碎裂
  break-protect-key: BREAK_PROTECT  # 碎裂保护物品标签
```

### 示例 2：强化石配置
```yaml
# 普通强化石
type: MATERIAL
material: NETHER_STAR
name: '&b普通强化石'
upgrade:
  reference: LEGENDARY      # 匹配 reference 为 LEGENDARY 的物品
  success: 100              # 提供 100% 基础成功率（会被目标物品的衰减影响）
  workbench: false
  max: 0
```

### 示例 3：保护物品配置
```yaml
# 掉级保护符
type: MATERIAL
material: EMERALD
name: '&a掉级保护符'
lore:
  - '&7强化失败时防止物品掉级'
  - '&7自动消耗'
upgrade-protection: DOWNGRADE_PROTECT  # 匹配物品配置中的 downgrade-protect-key

---

# 碎裂保护符
type: MATERIAL
material: DIAMOND
name: '&b碎裂保护符'
lore:
  - '&7强化失败时防止物品碎裂'
  - '&7自动消耗'
upgrade-protection: BREAK_PROTECT      # 匹配物品配置中的 break-protect-key
```

---

## 命令用法

### 基础语法
```
/mi item upgrade <mode> <chance> [flags...]
```

### 参数说明

| 参数 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `mode` | 必填 | 强化模式：`common` 或 `protect` | `common` |
| `chance` | 必填 | 成功率系数（浮点数） | `1.0` = 100%, `0.5` = 50% |
| `-free` | 可选 | 不消耗强化石 | `-free` |
| `-force` | 可选 | 可突破最大等级限制 | `-force` |
| `-direct:XX` | 可选 | 成功时直达指定等级 | `-direct:10` |

### 使用示例

#### 1. 普通强化（100% 成功率）
```bash
/mi item upgrade common 1.0
```
- 使用 common 模式
- 100% 成功率（实际成功率 = 基础成功率 × 衰减 × 1.0）
- 消耗 1 个强化石
- 失败会触发惩罚

#### 2. 防护模式强化（50% 成功率）
```bash
/mi item upgrade protect 0.5
```
- 使用 protect 模式（需要权限）
- 50% 成功率
- 消耗 1 个强化石
- 失败不会触发任何惩罚

#### 3. 免费强化
```bash
/mi item upgrade common 1.0 -free
```
- 不消耗强化石（需要权限）
- 其他行为与普通强化相同

#### 4. 强制突破等级上限
```bash
/mi item upgrade common 2.0 -force
```
- 200% 成功率（几乎必成功）
- 可以超过物品配置的 `max` 等级（需要权限）

#### 5. 直达指定等级
```bash
/mi item upgrade common 1.0 -direct:10
```
- 成功时直接升到 +10 级
- 消耗（10 - 当前等级）个强化石
- 需要权限

#### 6. 组合使用
```bash
/mi item upgrade protect 1.0 -free -direct:15
```
- 防护模式 + 免费 + 直达 15 级
- 需要所有相关权限

---

## 权限节点

| 权限节点 | 说明 | 默认 |
|----------|------|------|
| `mmoitems.command.item.upgrade` | 使用强化命令的基础权限 | OP |
| `mmoitems.command.item.upgrade.protect` | 使用 protect 模式 | OP |
| `mmoitems.command.item.upgrade.free` | 使用 `-free` 标志 | OP |
| `mmoitems.command.item.upgrade.force` | 使用 `-force` 标志 | OP |
| `mmoitems.command.item.upgrade.direct` | 使用 `-direct:XX` 标志 | OP |

### 权限配置示例（LuckPerms）
```bash
# 普通玩家：仅允许使用基础强化命令
lp group default permission set mmoitems.command.item.upgrade true

# VIP 玩家：额外允许使用防护模式
lp group vip permission set mmoitems.command.item.upgrade.protect true

# 管理员：允许所有功能
lp group admin permission set mmoitems.command.item.upgrade.* true
```

---

## 使用场景

### 场景 1：付费强化系统
**需求**: 玩家在 NPC 处使用金币付费强化，而不是直接右键强化石

**配置**:
1. 物品设置 `disable-backpack: true`
2. 使用 Citizens 或 MythicMobs 创建强化 NPC
3. NPC 点击时扣除金币，然后执行强化命令：
   ```
   /mi item upgrade common 1.0
   ```

### 场景 2：强化活动
**需求**: 在特定活动期间提高强化成功率

**方案**:
- 活动期间，管理员使用命令为玩家强化：
  ```bash
  /mi item upgrade common 2.0    # 双倍成功率
  ```

### 场景 3：防爆系统
**需求**: 高等级强化时提供防爆保护

**方案 1**: 使用保护物品
- 配置掉级/碎裂保护符
- 玩家购买保护符后强化

**方案 2**: 使用 protect 模式
- 授予 VIP 玩家 `mmoitems.command.item.upgrade.protect` 权限
- VIP 可使用防护模式强化

### 场景 4：直达系统
**需求**: 玩家可以一次性将装备强化到指定等级

**配置**:
```bash
# 将 +5 装备直接强化到 +10
/mi item upgrade common 1.0 -direct:10
```
- 消耗 5 个强化石（10 - 5 = 5）
- 失败时仍然只升 1 级或触发惩罚

---

## 配置检查清单

在使用强化命令系统前，请确保：

- [ ] 物品配置中设置了 `upgrade.disable-backpack: true`
- [ ] 物品配置了有效的 `upgrade.template` 强化模板
- [ ] 强化石配置了正确的 `reference` 标签
- [ ] 如果使用保护系统，配置了保护物品和对应的 `protect-key`
- [ ] 如果使用惩罚系统，配置了正确的区间和概率
- [ ] 玩家拥有必要的权限节点
- [ ] 在 plugin.yml 或权限插件中注册了权限节点

---

## 常见问题

### Q: 为什么玩家无法使用命令强化？
A: 检查以下几点：
1. 玩家是否手持可强化的物品？
2. 玩家是否拥有 `mmoitems.command.item.upgrade` 权限？
3. 物品是否配置了 `upgrade` 属性？
4. 强化模板是否存在且有效？

### Q: 为什么显示"强化石不足"？
A: 检查以下几点：
1. 是否使用了 `-free` 标志？如果没有，需要消耗强化石
2. 强化石的 `reference` 是否与目标物品的 `reference` 匹配？
3. 如果使用 `-direct:XX`，是否有足够数量的强化石？（需要等级差数量）

### Q: 成功率是如何计算的？
A: 实际成功率 = 强化石基础成功率 × 目标物品衰减系数^当前等级 × chance参数

例如：
- 强化石成功率：100%
- 目标物品当前等级：5
- 衰减系数：0.95
- chance 参数：1.0
- 实际成功率 = 1.0 × 0.95^5 × 1.0 ≈ 77.4%

### Q: 掉级和碎裂的优先级是什么？
A: 惩罚优先级（从高到低）：
1. 碎裂（如果在碎裂区间且触发）
2. 掉级（如果在掉级区间且触发）
3. 销毁（如果 `destroy: true`）
4. 无惩罚