# EraUpgrade（元年强化）插件策划案

**文档版本**：V1.1 

**适用对象**：服务器技术开发 / 维护人员  

**核心依赖**：MMOItems (v6.0+)、PlaceholderAPI (PAPI)、Vault、Paper (1.21.8)  

## 1. 插件概述

### 1.1 插件名称

- 英文：EraUpgrade  

- 中文：元年强化  

### 1.2 核心定位

基于 MMOItems 强化系统的高拓展性 RPG 强化插件，主打「精细化强化策略 + 动态概率 + 风险收益平衡」，支持强化石匹配、辅料增益、保底机制等核心功能，适配我的世界 MMORPG 服务器的装备成长需求，提供可视化 UI 与实时结果反馈。

### 1.3 依赖插件（强制）

|插件名称|作用|最低版本要求|
|---|---|---|
|MMOItems|提供强化物品与强化石的物品体系，对接其强化属性系统|v6.0|
|PlaceholderAPI|解析玩家条件（等级/声望/成就）与额外概率加成|v2.11.3|
|Vault|对接经济系统（金币消耗）|v1.7.3|
|PlayerPoints|可选，对接点券系统（特殊强化消耗）|v3.3.3|
### 1.4 设计目标

1. 操作轻量化：玩家强化路径≤5步，UI 交互直观无冗余；  

2. 配置灵活性：强化规则、概率公式、UI 布局全通过 YAML 自定义；  

3. 策略多样性：支持「激进强化」（高风险高收益）与「稳健强化」（辅料保安全）两种模式；  

4. 性能优化：玩家数据异步存储，高频操作（如概率计算）本地缓存；  

5. 兼容性：适配 MMOItems 强化系统，支持其物品属性、强化等级字段同步。  

## 2. 核心功能与流程

### 2.1 基础强化流程（玩家视角）

|步骤|操作描述|
|---|---|
|1|通过指令 `/eraupgrade open` 打开强化菜单（支持绑定 NPC 点击触发）|
|2|将需要强化的 MMOItems 物品放入「强化槽」，UI 自动匹配该物品可用的强化石类型（如武器只能用「武器强化石」）|
|3|强化预览区自动显示：当前强化等级（+n）、目标等级（+n+1）、所需强化石数量、金币消耗、基础成功率、失败惩罚（掉级/损毁概率）、等级上限|
|4|（可选）在「辅料槽」放入辅助强化石：<br>- 幸运石：提升成功率<br>- 保护石：降低失败损毁概率<br>- 直达石：跳过当前等级直接+1（概率触发）|
|5|点击「开始强化」按钮，插件校验条件：<br>① 强化石/辅料数量足够<br>② 金币余额满足<br>③ 物品未达强化上限<br>④ 玩家满足 PAPI 条件（如等级/权限）<br>⑤ 未触发每日强化次数限制|
|6|条件满足则扣除材料/金币，立即执行强化判定：<br>- 成功：等级+1，UI 实时更新物品显示<br>- 失败：按规则触发掉级/损毁，同步更新物品|
|7|强化结果反馈：<br>- 高等级强化（如+12、+15）触发全服通报<br>- 普通结果通过动作栏/聊天栏提示|
|8|玩家可直接关闭菜单（物品自动保存强化结果）或手动取出物品，流程结束|
### 2.2 核心拓展功能

|拓展功能|功能描述|
|---|---|
|强化保底机制|同一物品连续失败 N 次（默认 30 次，可配置）后，下一次强化必成功；失败次数记录随物品绑定，换物品重置|
|额外概率加成|通过权限/PAPI 变量配置概率公式（如「玩家等级每级+0.5%」「VIP 权限+5%」），与基础成功率乘算，UI 实时显示加成明细|
|强化等级上限|全局默认上限（如+20），支持按物品类型/品质单独配置（如神器上限+30，普通装备+10）|
|失败惩罚梯度|低等级强化（如+1+5）失败无惩罚，中等级（+6+10）失败掉 1 级，高等级（+11+）失败可能损毁物品|
|强化外观特效|强化等级达阈值（如+10）后，物品显示自定义发光特效（通过 MMOItems 粒子效果配置）|
|每日强化限制|可配置玩家每日最大强化次数（如普通玩家 50 次，VIP 100 次），通过 PAPI 变量动态调整|
|强化转移|消耗特殊道具，将 A 物品的强化等级转移到同类型 B 物品（保留 80%等级，可配置）|
|强化日志查询|玩家可查看当前物品的强化历史（成功/失败次数、时间），管理员可查询全服记录|
### 2.3 流程总览（文字流程图）

```Plain Text
玩家打开强化菜单 → 放入物品 → UI 匹配强化石/显示预览信息 → 放入辅料（可选）→ 点击强化

→ 条件不满足 → 提示缺失条件（材料/金币/权限等）

→ 条件满足 → 扣除材料/金币 → 执行强化判定

   → 成功 → 等级+1 → UI 更新物品 → 高等级触发全服通报

   → 失败 → 执行惩罚（掉级/损毁）→ UI 更新物品 → 记录失败次数（累计保底）

→ 玩家取出物品/关闭菜单（结果自动保存）
```

## 3. 配置文件规范

插件根目录结构：

```Plain Text
EraUpgrade/
├─ config.yml          # 核心配置（数据库、概率、权限等）
├─ language.yml        # 多语言配置
├─ upgrade_stone.yml   # 强化石与辅料配置
├─ user_data/          # 玩家强化临时数据（按 UUID 分文件）
│  └─ 86fa2a7a-xxxx-xxxx-xxxx-xxxxxxxxxxxx.yml  # 示例：玩家数据
└─ menus/
   └─ gui.yml          # 强化菜单 UI 配置
```

### 3.1 核心配置（config.yml）

```YAML
# 数据库配置（存储玩家强化记录、保底次数等）
database:
  type: MySQL  # MySQL/SQLite
  host: 127.0.0.1
  port: 3306
  database: eraupgrade
  username: root
  password: 123456
  table_prefix: eraupgrade_

# 强化基础配置
upgrade:
  default_max_level: 20  # 全局默认强化上限
  max_daily_attempts: 50  # 每日最大强化次数（PAPI 可覆盖）
 保底机制:
    consecutive_fail_threshold: 30  # 连续失败 N 次后必成功
    reset_on_success: true  # 成功后重置失败计数

# 额外概率加成配置（与基础成功率乘算，格式：权限/条件: 加成公式）
chance_additional:
  # 示例1：VIP 权限加成
  "permission:group.vip": "5"  # VIP 直接+5%
  # 示例2：玩家等级加成（每级+0.5%）
  "papi:%player_level%": "0.5 * %player_level%"
  # 示例3：成就加成（完成特定成就+3%）
  "papi:%player_achievements_unlocked% contains '强化大师'": "3"

# 失败惩罚配置（按等级区间）
failure_punish:
  - level_range: 1-5    # 强化等级范围
    drop_level: 0       # 失败掉级数（0=不掉级）
    destroy_chance: 0   # 失败损毁概率（%）
  - level_range: 6-10
    drop_level: 1
    destroy_chance: 5
  - level_range: 11-20
    drop_level: 2
    destroy_chance: 20

# 权限配置
permission:
  prefix: eraupgrade
  open_menu: "eraupgrade.open"  # 打开菜单权限
  bypass_daily_limit: "eraupgrade.bypass.daily"  # 跳过每日限制
  transfer: "eraupgrade.transfer"  # 强化转移权限

# 特效配置（对接 MMOItems 粒子效果）
effects:
  level_10: "PARTICLE:REDSTONE:1:1:1:0.1:10"  # +10 级发光特效
  level_15: "PARTICLE:ENCHANTMENT_TABLE:1:1:1:0.2:20"  # +15 级特效

# 全服通报配置（高等级强化）
broadcast:
  levels: [12, 15, 20, 30]  # 需要通报的等级
  message: "&6[元年强化] &a玩家 {player} 的 {item_name} 强化到 +{level} 级！欧皇附体！"

# 调试模式
debug: false
```

### 3.2 语言配置（language.yml）

所有文本支持 PAPI 变量，`{xxx}`为插件内置占位符：

```YAML
prefix: "&8[&6元年强化&8]&r "

# 菜单文本
menu_title: "&6装备强化 - {item_name}"
slot_item: "&7强化物品槽"
slot_upgrade_stone: "&7强化石槽（需 {count} 个）"
slot_辅料:
  lucky: "&e幸运石槽（提升成功率）"
  protect: "&b保护石槽（降低损毁率）"
  direct: "&c直达石槽（概率跳级）"
button_upgrade: "&a开始强化"
button_transfer: "&d强化转移"

# 条件提示
no_permission: "{prefix}&c无权限使用强化功能！"
item_not_supported: "{prefix}&c该物品不支持强化！"
level_reached_max: "{prefix}&c该物品已达强化上限（+{max_level}）！"
material_not_enough: "{prefix}&c材料不足：缺少 {stone_name}×{need}（当前{have}）"
gold_not_enough: "{prefix}&c金币不足：需要{need}，当前{have}"
daily_limit_reached: "{prefix}&c今日强化次数已用完（{used}/{max}）"
condition_not_met: "{prefix}&c未满足条件：{condition}"

# 强化结果提示
success: "{prefix}&a强化成功！{item_name} 变为 +{new_level} 级！"
fail_drop: "{prefix}&c强化失败！{item_name} 掉至 +{new_level} 级！"
fail_destroy: "{prefix}&c强化失败！{item_name} 已损毁！"
guarantee_trigger: "{prefix}&e保底触发！本次强化必成功！"

# 额外概率提示
chance_additional: "&7额外加成：{details}（总计+{total}%）"  # 如「VIP+5%，等级+3%（总计+8%）」
```

### 3.3 强化石与辅料配置（upgrade_stone.yml）

```YAML
# 强化石配置（用于提升等级，需与物品类型匹配）
upgrade_stones:
  weapon_stone:  # 强化石ID（自定义）
    name: "武器强化石"  # 显示名称
    mmo_type: "MATERIAL"  # MMOItems 物品类型
    mmo_id: "weapon_upgrade_stone"  # MMOItems 物品ID
    applicable_types: ["SWORD", "AXE", "BOW"]  # 适用的物品类型（MMOItems类型）
    level_requirement: 1-10  # 支持的强化等级范围（+1~+10可用）
    consume_amount: 1  # 每次强化消耗数量

  armor_stone:
    name: "防具强化石"
    mmo_type: "MATERIAL"
    mmo_id: "armor_upgrade_stone"
    applicable_types: ["HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS"]
    level_requirement: 1-10
    consume_amount: 1

# 辅料配置（辅助强化，不限制物品类型）
auxiliary_stones:
  lucky_stone:  # 幸运石
    name: "幸运石"
    mmo_type: "CONSUMABLE"
    mmo_id: "lucky_stone"
    effect: "chance + 10"  # 效果：成功率+10%
    consume_chance: 100  # 消耗概率（100=必消耗）

  protect_stone:  # 保护石
    name: "保护石"
    mmo_type: "CONSUMABLE"
    mmo_id: "protect_stone"
    effect: "destroy_chance - 50%"  # 效果：损毁概率降低50%
    consume_chance: 80  # 80%概率消耗

  direct_stone:  # 直达石
    name: "直达石"
    mmo_type: "CONSUMABLE"
    mmo_id: "direct_stone"
    effect: "skip_level: 30%"  # 30%概率直接跳过当前等级（+1变为+2）
    consume_chance: 100
```

### 3.4 玩家数据文件（user_data/[UUID].yml）

```YAML
# 玩家基础信息
player_name: "Steve"
last_update: 1718236800000  # 最后更新时间戳（毫秒）

# 每日强化次数
daily_attempts:
  used: 23  # 今日已用次数
  reset_time: 1718284800000  # 重置时间（次日0点）

# 物品强化记录（按物品唯一标识存储）
item_records:
  "mmo:SWORD:dragon_sword:12345":  # 格式：mmo:类型:ID:持久化ID（确保唯一性）
    current_level: 8  # 当前强化等级
    consecutive_fails: 12  # 连续失败次数（用于保底）
    total_attempts: 45  # 总强化次数
    last_attempt_time: 1718236700000  # 最后强化时间
```

### 3.5 强化菜单配置（menus/gui.yml）

**核心布局**：6行9列（54槽），按功能划分区域：

```YAML
menu:
  id: eraupgrade_gui
  title: "{menu_title}"  # 引用language.yml的menu_title
  rows: 6  # 6行菜单
  update_interval: 20  # 20ticks刷新一次UI（实时更新概率/数量）

  # Layout 字符定义：
  # I：强化物品槽；S：强化石槽；L：幸运石槽；P：保护石槽；D：直达石槽；
  # U：强化按钮；T：转移按钮；C：关闭按钮
  layout:
    - "I        "
    - " SSSS    "
    - " L  P  D  "
    - "          "
    - "    U     "
    - "    T  C  "

  # 槽位配置
  slots:
    # 强化物品槽（I）
    I:
      slot: 0  # 第1行第1列（索引0）
      icon:
        material: AIR  # 放入物品后显示物品本身
        name: "{slot_item}"
        lore:
          - "&7当前等级：+{current_level}"
          - "&7最大等级：+{max_level}"
      click-actions:
        - "取放物品"  # 允许玩家放入/取出物品

    # 强化石槽（S）
    S:
      slot: 10-13  # 第2行第2-5列（索引10-13）
      icon:
        material: "{stone_material}"  # 显示匹配的强化石
        name: "{slot_upgrade_stone}"
        lore:
          - "&7适用等级：{stone_level_range}"
      click-actions:
        - "取放强化石"

    # 辅料槽（L/P/D）
    L:
      slot: 19  # 第3行第2列
      icon:
        material: "{lucky_stone_material}"
        name: "{slot_辅料.lucky}"
        lore:
          - "&7效果：{lucky_effect}"
      click-actions:
        - "取放辅料"
    P:
      slot: 21
      # 配置同L（保护石）
    D:
      slot: 23
      # 配置同L（直达石）

    # 强化按钮（U）
    U:
      slot: 31  # 第5行第4列
      icon:
        material: EMERALD_BLOCK
        name: "{button_upgrade}"
        lore:
          - "&e基础成功率：{base_chance}%"
          - "{chance_additional}"  # 额外加成提示
          - "&e消耗金币：{gold_cost}"
          - "&e失败惩罚：{punish_info}"  # 如「掉1级，5%损毁」
          - "&e连续失败：{consecutive_fails}/{threshold}（保底）"
      click-actions:
        - "execute as {player} run eraupgrade upgrade"
        - "refresh"  # 刷新UI

    # 转移按钮（T）
    T:
      slot: 46  # 第6行第4列
      icon:
        material: Ender_Pearl
        name: "{button_transfer}"
        lore:
          - "&7将当前物品的强化等级转移到同类型物品"
          - "&7消耗：转移石×1"
      click-actions:
        - "open-menu eraupgrade_transfer"  # 打开转移子菜单

    # 关闭按钮（C）
    C:
      slot: 50  # 第6行第6列
      icon:
        material: BARRIER
        name: "&c关闭菜单"
        click-actions:
          - "close-menu"
```

## 4. 菜单系统设计

### 4.1 强化主菜单核心逻辑

1. **动态匹配强化石**：物品放入后，插件读取其 MMOItems 类型（如 SWORD），自动从 `upgrade_stone.yml` 匹配适用的强化石，在 S 槽显示对应图标与数量要求；  

2. **实时概率计算**：基础成功率（按等级动态调整，如+1为90%，+10为30%）+ 额外加成（权限/PAPI 公式），实时显示在强化按钮 lore 中；  

3. **状态联动**：  

    - 物品未放入时，强化石槽、辅料槽、强化按钮均锁定（显示灰色）；  

    - 材料不足时，强化按钮 lore 用红色标注缺失项；  

    - 触发保底时，强化按钮名称变为「&6保底强化」并高亮；  

4. **特效联动**：物品强化等级达阈值（如+10）时，物品图标显示 `config.yml` 配置的粒子特效。  

### 4.2 强化转移子菜单逻辑

1. **双槽设计**：左侧放「源物品」（含强化等级），右侧放「目标物品」（同类型，无强化等级）；  

2. **转移规则**：  

    - 目标物品必须与源物品同类型（如均为 SWORD）；  

    - 转移后源物品等级清零，目标物品等级 = 源等级 × 0.8（向下取整）；  

    - 消耗 1 个「转移石」（在 `upgrade_stone.yml` 中配置）；  

3. **确认机制**：点击「确认转移」按钮后，弹出二次确认提示，避免误操作。  

### 4.3 交互规则

1. **防误触**：强化按钮点击后锁定 1 秒，避免重复触发；  

2. **物品保护**：菜单关闭时，无论是否取出物品，强化结果均自动保存（防止意外关闭丢失数据）；  

3. **权限过滤**：无转移权限的玩家，转移按钮隐藏。  

## 5. 权限系统设计

### 5.1 权限前缀

所有权限前缀为 `eraupgrade`（可在 `config.yml` 中修改）。

### 5.2 玩家权限列表

|权限节点|描述|示例|
|---|---|---|
|eraupgrade.open|允许打开强化菜单|-|
|eraupgrade.stone.{stone_id}|允许使用指定强化石|eraupgrade.stone.weapon_stone|
|eraupgrade.transfer|允许使用强化转移功能|-|
|eraupgrade.bypass.daily|跳过每日强化次数限制|-|
|eraupgrade.effect.{level}|允许显示指定等级的强化特效|eraupgrade.effect.10|
### 5.3 管理员权限列表

|权限节点|描述|
|---|---|
|eraupgrade.admin|所有管理员权限（总权限）|
|eraupgrade.admin.reload|重载插件配置|
|eraupgrade.admin.reset|重置玩家每日强化次数|
|eraupgrade.admin.modify|修改物品强化等级/失败次数|
|eraupgrade.admin.log|查看玩家强化日志|
## 6. 指令系统设计

### 6.1 玩家指令

|指令格式|权限|描述|
|---|---|---|
|/eraupgrade open|eraupgrade.open|打开强化主菜单|
|/eraupgrade log [物品]|无|查看当前手持物品的强化日志|
### 6.2 管理员指令

|指令格式|权限|描述|
|---|---|---|
|/eraupgrade admin reload|eraupgrade.admin.reload|重载所有配置文件|
|/eraupgrade admin reset <玩家名>|eraupgrade.admin.reset|重置指定玩家的每日强化次数|
|/eraupgrade admin modify <玩家名> <等级>|eraupgrade.admin.modify|修改玩家手持物品的强化等级|
|/eraupgrade admin log <玩家名> [物品ID]|eraupgrade.admin.log|查看指定玩家的强化日志（可选物品筛选）|
|/eraupgrade admin debug <on/off>|eraupgrade.admin|开启/关闭调试模式|
## 7. 数据库设计

### 7.1 核心数据表结构

#### 7.1.1 玩家强化记录表（{table_prefix}player_records）

|字段名|类型|主键|描述|
|---|---|---|---|
|id|INT|是|自增ID|
|player_uuid|VARCHAR(36)|否|玩家UUID|
|item_identifier|VARCHAR(128)|否|物品唯一标识（mmo:类型:ID:持久化ID）|
|current_level|INT|否|当前强化等级|
|consecutive_fails|INT|否|连续失败次数|
|total_attempts|INT|否|总强化次数|
|last_attempt_time|BIGINT|否|最后强化时间戳（毫秒）|
#### 7.1.2 玩家每日次数表（{table_prefix}daily_limits）

|字段名|类型|主键|描述|
|---|---|---|---|
|player_uuid|VARCHAR(36)|是|玩家UUID（唯一）|
|used_attempts|INT|否|今日已用次数|
|reset_timestamp|BIGINT|否|下次重置时间戳（毫秒）|
## 8. 技术实现要点

### 8.1 依赖插件 API 调用

- **MMOItems API**：  

    - 通过 `MMOItems.getAPI().getItem(type, id)` 获取物品实例；  

    - 调用 `item.setUpgradeLevel(level)` 更新强化等级；  

    - 通过 `item.getPersistentDataContainer()` 存储物品唯一标识（用于绑定失败次数）。  

- **PlaceholderAPI**：  

    - 调用 `PlaceholderAPI.setPlaceholders(player, formula)` 解析概率加成公式；  

    - 实时计算并缓存额外概率（减少重复解析消耗）。  

- **Vault API**：  

    - 通过 `Economy.withdrawPlayer(player, amount)` 扣减金币，确保操作原子性（失败时回滚）。  

### 8.2 性能优化

- **本地缓存**：玩家的强化记录、物品失败次数在打开菜单时加载到内存，关闭菜单后异步写入数据库；  

- **概率计算缓存**：同一物品的基础成功率+额外加成仅在打开菜单/放入辅料时计算，避免每秒重复计算；  

- **批量操作**：玩家离线时，自动将内存中的临时数据批量写入数据库，减少IO次数。  

### 8.3 事件监听

- `InventoryClickEvent`：监听菜单点击，区分物品槽/按钮，执行取放物品、强化、转移等逻辑；  

- `PlayerQuitEvent`：玩家离线时，异步保存其强化记录与每日次数；  

- `PlayerJoinEvent`：加载玩家数据到内存，推送未处理的强化结果提示（如上次离线时物品损毁）。  

### 8.4 异常处理

|异常场景|处理方式|
|---|---|
|物品无强化属性|菜单提示「该物品不支持强化」，隐藏强化石槽|
|强化石与物品类型不匹配|强化石槽显示红色边框，提示「不适用该物品」|
|数据库连接失败|临时使用本地文件存储（user_data文件夹），恢复后同步|
|并发强化冲突|加锁处理，同一物品同时强化时仅允许第一个请求执行|
## 9. 测试要点

### 9.1 功能测试

- 核心流程覆盖：放入物品→匹配强化石→强化成功/失败→结果保存；  

- 拓展功能测试：保底机制触发（连续30次失败）、强化转移、全服通报；  

- 辅料效果测试：幸运石提升成功率、保护石降低损毁率的实际生效情况。  

### 9.2 边界测试

- 强化等级达上限时，无法继续强化；  

- 每日次数用尽后，强化按钮锁定；  

- 转移时源物品与目标物品不同类型的拦截提示。  

### 9.3 性能测试

- 模拟50+玩家同时强化，监控TPS波动（需≥18）；  

- 连续1000次强化操作后，检查数据库性能与内存泄漏情况。  

### 9.4 兼容性测试

- 适配 MMOItems v6.0/v7.0 版本（API 差异处理）；  

- 测试不同编码的配置文件（确保 UTF-8 无乱码）。  

## 10. 兼容性说明

- 服务端核心：仅支持 Paper 1.21.8（不支持 Forge/Fabric）；  

- MMOItems 版本：v6.0+（v7.0 需适配其强化等级字段变更）；  

- 跨服兼容：多服集群需使用 MySQL 数据库，确保玩家数据同步；  

- 字符编码：所有配置文件必须采用 UTF-8 编码，避免乱码。
> （注：文档部分内容可能由 AI 生成）