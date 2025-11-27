# MMOItems 2025‑07‑13 → 2025‑11‑19 同步与魔改影响总结（基于 phoenix-dvpmt/mmoitems master）

> 本文档总结本项目在基线版本（约 2025‑07‑13）之后同步到上游最新提交（`a07737b1`，2025‑11‑19）的整体变更、风险点以及对现有魔改逻辑的影响，便于后续排查与二次魔改。

---

## 1. 同步范围与总体情况

- **上游范围**：`fd428b51` 起至 `a07737b1`（2025‑07‑13 之后 master 分支全部提交）。  
- **本地策略**：
  - 对于简单、冲突小的提交，直接使用 `git show <commit> | git apply` 自动合并。
  - 对于与本地魔改冲突的部分（尤其是玩家数据、背包解析、指令、安全相关逻辑），在阅读 diff 后手工合并，保证魔改逻辑尽量保留。
- **编译状态**：
  - 根目录执行 `mvn -q -DskipTests package` 已通过，三个模块均可编译。

---

## 2. 主要功能性更新汇总

### 2.1 RPG / 属性相关

- **HeroesHook 优化（Heroes 体力/法力缓存）**  
  文件：`MMOItems-API/src/main/java/net/Indyuce/mmoitems/comp/rpg/HeroesHook.java`  
  关键点：
  - 为 Heroes 玩家缓存 `MAX_MANA` 与 `MAX_STAMINA`，避免每 2tick 刷新时频繁写入。
  - MMOItems 不再在每次 `resolveInventory()` 时粗暴刷新，而是：
    - 注册 MythicLib 属性监听：`MAX_MANA`、`MAX_STAMINA`。
    - 当对应属性变化时，仅在数值变化时对 Heroes 进行 `removeMax*` + `addMax*`。
  - 你的魔改中原本已经提前接入了缓存逻辑，本次同步将该逻辑进一步对齐上游的 **StatInstance 监听** 模式。

- **MMOCore 属性联动修复**  
  文件：`MMOItems-API/src/main/java/net/Indyuce/mmoitems/comp/mmocore/MMOCoreHook.java`  
  关键点：
  - 为每个 MMOCore Attribute 注册对应的 MMOItems 扩展属性，并监听其变动。
  - 当扩展属性变化时，通过 MMOCore API 主动调用 `attributeInstance.updateStats()`，修复“持有物品时属性不刷”的问题。

- **AuraSkills / AureliumSkills Hook 改造**  
  文件：
  - `.../comp/rpg/AuraSkillsHook.java`  
  - `.../comp/rpg/AureliumSkillsHook.java`  
  关键点：
  - 以前是 MMOItems 每次 `refreshStats` 时调用 Aura/Aurelium API 写入所有 Trait/StatModifier。
  - 现在改为：
    - 每个“额外属性”对应一个 MythicLib Stat，注册 UpdateListener。
    - 当 Stat 变更时，直接对 Aura/Aurelium 中的对应 Trait/Stat 做一次更新。
  - 同时为 `MAX_MANA` 注册监听，将最大法力同步到 AuraSkills Trait 中。

- **RacesAndClassesHook / 其他 RPGHook 调整**  
  文件：`.../comp/rpg/RacesAndClassesHook.java` 等  
  关键点：
  - RacesAndClasses：监听 `MAX_MANA` 的 StatInstance，变更时同步 RnC Mana 最大值，而不是在每次背包刷新时统一刷新。
  - DefaultHook / BattleLevels / Skills / SkillsPro / McMMO / McRPG 等：删除空实现的 `refreshStats`，统一改造为“事件 + Stat 监听”模式。

### 2.2 背包解析与玩家数据生命周期

- **PlayerData 改为基于 MythicLib 会话的生命周期管理**  
  文件：`MMOItems-API/src/main/java/net/Indyuce/mmoitems/api/player/PlayerData.java`  
  关键点：
  - `PlayerData` 继承 `SynchronizedDataHolder`，不再实现 `Closeable`，而是：
    - 新增 `onSessionReady()`：在 MythicLib 会话准备好时初始化 `InventoryResolver` 并立即进行一次 `resolveInventory()`。
    - 新增 `onSaved(SaveReason reason)`：在非 `AUTOSAVE` 场景下清理当前档案的 MMOItems 增益，防止切档时残留。
  - `resolveInventory()`：
    - 不再依赖 `isOnline()` 与 `hasFullySynchronized()`，而是改为 `MMOPlayerData.isPlaying()` 判断。
    - 去掉“遍历所有 RPGHandler.refreshStats”逻辑。RPG 插件改为通过属性监听方式自行更新。

- **InventoryResolver 重构与清理逻辑增强**  
  文件：`MMOItems-API/src/main/java/net/Indyuce/mmoitems/inventory/InventoryResolver.java`  
  关键点：
  - 不再实现 `Closeable`，改为：
    - `initialize()`：从 `PlayerInventoryManager` 获取并缓存所有 `InventoryWatcher`。
    - `onClose()`：
      - 清空 watcher 列表，防止会话结束后异步回调。
      - 通过 `StatMap.bufferUpdates` 统一撤销所有已应用的 `EquippedItem` 修饰器和套装修饰器。
  - 新增 `resetItemSetModifiers()`，以统一清理套装附加的 StatModifier。
  - `InventoryWatcher` 改造：
    - 移除 `@Deprecated` 标记。
    - 静态方法由 `callbackIfNotNull` 重命名为泛型 `callIfNotNull`，便于在不同场景下复用。
  - `PlayerInventoryManager.getWatchers`：使用 `stream().collect(Collectors.toList())` 避免旧 JDK 对 `toList()` 的兼容问题。

- **CraftingStatus 改造：多档案下的合成队列持久化**  
  文件：
  - `MMOItems-API/src/main/java/net/Indyuce/mmoitems/api/crafting/CraftingStatus.java`  
  - `.../manager/data/YAMLDataHandler.java` → 新引入 `YAMLDatabaseImpl.java`  
  关键点：
  - `CraftingStatus` 现在持有 `PlayerData` 引用，通过：
    - `loadFromYaml(ConfigurationSection)`
    - `saveToYaml(ConfigurationSection)`  
    来读写玩家各合成台的排队状态。
  - YAML 结构兼容旧格式（`started` + `delay`）与新格式（`start` + `completion`）。
  - 切换档案时不会互相覆盖，解决“换档后合成队列异常复用”的问题。

### 2.3 指令与消息展示

- **指令系统重构**  
  代表文件：
  - 新增：`MMOItems-API/src/main/java/net/Indyuce/mmoitems/command/Arguments.java`
  - 大量修改：`MMOItemsCommandTreeRoot` 及多级子命令节点（包括 `Give*`、`Update*`、`list`、`stations` 等）。  
  关键点：
  - 用统一的 `Arguments` 封装命令解析与 Tab 补全。
  - 删除旧的 `UpdateItemCompletion` 与部分旧版 `Help`/`RevisionID` 节点，改用新的结构：
    - 新增 `mmoitems/RevisionIDCommandTreeNode` 等。
  - 你的魔改如果在指令层（尤其是 `GiveCommandTreeNode`、`UpdateCommandTreeNode`、`ApplyCommandTreeNode` 等）有深度改动，本次重构对它们影响较大，后面第 4 节单独说明。

- **动作栏消息改为 MythicLib ActionBar 队列**  
  文件：`MMOItems-API/src/main/java/net/Indyuce/mmoitems/api/util/message/FormattedMessage.java`  
  关键点：
  - 原逻辑：直接调用 Bukkit Wrapper 的 `sendActionBar`，同时在启用 MMOCore 时打标记。
  - 新逻辑：使用 `MMOPlayerData.get(player).getActionBar().show(message)`，由 MythicLib 统一调度。
  - 你的魔改中对该类只添加了中文注释，没有更改行为，本次同步保留了行为变更与中文注释。

### 2.4 兼容性小修复

- **PlaceholderAPI 更新**  
  文件：
  - `MMOItems-API/pom.xml`
  - `.../comp/placeholders/MMOItemsPlaceholders.java`  
  关键点：
  - 仓库地址改为 `https://repo.extendedclip.com/releases/`。
  - 依赖版本更新为 `2.11.6`。
  - `MMOItemsPlaceholders` 构造 Type 时改用 `net.Indyuce.mmoitems.api.Type.get(t)`，并删除冗余 `Type` import。

- **MythicMobs 合成触发改进**  
  文件：`.../comp/mythicmobs/crafting/MythicMobsSkillTrigger.java`  
  关键点：
  - 技能释放位置改为玩家脚下 `getLocation()`，而不是 `getEyeLocation()`。
  - 目标实体改为 `MythicUtil.getTargetedEntity(player)`（若无则空列表），避免技能总是作用在玩家自身。

- 其他 UI / GUI 小修：
  - 某些 GUI 中 `ItemMeta.addItemFlags(ItemFlag.values())` 被注释掉，防止新版客户端显示异常。
  - 浏览与编辑 GUI 的分页/按钮逻辑按上游修复，配合你之前的中文提示魔改，共同生效。

---

## 3. 安全与配置相关改动

### 3.1 物品指令安全控制

文件：
- `MMOItems-API/src/main/java/net/Indyuce/mmoitems/api/interaction/UseItem.java`
- `MMOItems-API/src/main/java/net/Indyuce/mmoitems/listener/ItemUse.java`
- `MMOItems-API/src/main/java/net/Indyuce/mmoitems/manager/ConfigManager.java`
- `MMOItems-Dist/src/main/resources/config.yml`

关键点：

- **新增配置选项**：
  - `enable_item_granted_permissions`：是否允许物品赋予权限（对应 `ItemStats.GRANTED_PERMISSIONS`）。
  - `item_commands.enabled`：是否启用物品指令。
  - `item_commands.whitelist`：物品指令白名单，支持正则表达式。
  - `op-item-stats.stats` 追加 `GRANTED_PERMISSIONS`，可强制要求“OP 级编辑权限”才能编辑相关属性。

- **执行逻辑变更**：
  - 在 `ItemUse` 中：
    - 消耗品与普通物品的指令执行处增加 `if (MMOItems.plugin.getLanguage().itemCommands) useItem.executeCommands()` 判断，可全局关闭物品指令。
  - 在 `UseItem.scheduleCommandExecution` 中：
    - 对指令字符串做占位符解析得到 `parsedCommand`。
    - 调用 `ConfigManager.isAllowed(parsedCommand)` 检查是否命中白名单；否则记录 `WARNING` 日志并跳过执行。
    - 延时执行场景下同样使用已检查过的 `parsedCommand`。
  - 在 `InventoryResolver` 中：
    - 只有在 `itemGrantedPermissions == true` 且物品包含 `GRANTED_PERMISSIONS` 时才注册权限。

**可能影响**：
- 如果你之前依赖物品指令做 **GM 工具 / 管理指令**，现在：
  - 默认开启，但如果后续你在配置中关闭，将导致所有物品指令失效。
  - 如启用白名单且配置不当，部分旧有物品指令会被“静默拦截”（只写 warn 日志，不执行）。

### 3.2 配置文件结构变化

文件：`MMOItems-Dist/src/main/resources/config.yml`  

- 删除了旧的 `fix-left-click-interact` 配置（上游认为已不再需要）。
- 新增 `enable_item_granted_permissions`、`item_commands.*` 以及对 `op-item-stats.stats` 的扩展（含 `GRANTED_PERMISSIONS`）。

你的魔改如果在外部运维脚本里依赖旧配置项，需要同步更新。

---

## 4. 对你现有魔改的具体影响

> 以下只列出**我们确认改动过且原本存在魔改的关键文件**；未列出的文件要么原本未魔改，要么本次未触及。

### 4.1 玩家登录属性刷新链路

相关文件：
- `MMOItems-API/src/main/java/net/Indyuce/mmoitems/api/player/PlayerData.java`
- `MMOItems-API/src/main/java/net/Indyuce/mmoitems/inventory/InventoryResolver.java`
- `MMOItems-Dist/src/main/java/net/Indyuce/mmoitems/listener/PlayerListener.java`
- `MMOItems-API/src/main/java/net/Indyuce/mmoitems/inventory/LoginRefreshSession.java`（你的魔改文件，未再改动）

变化与影响：

- 你之前的魔改：
  - 为了修复进服时属性刷新不完整/乱序的问题，引入了 `LoginRefreshSession`，并在 `PlayerListener` 中：
    - 监听 `PlayerJoinEvent` + `SynchronizedDataLoadEvent`，用“延迟批刷新 + 去抖动”方案调用 `resolveInventory` 与相关刷新。
    - `PlayerQuitEvent` 中清理 pending 的任务与 Session。
- 本次同步：
  - 保留了你在 `PlayerJoinEvent` / `PlayerQuitEvent` 中的 **登录刷新调度逻辑**（仍然存在，且日志保持中文）。
  - **移除了** `resolveInvWhenDataLoaded(SynchronizedDataLoadEvent)` 事件监听（上游认为由 MythicLib 会话回调 + PlayerJoin 链路就够）。
  - 新增 `PlayerData.onSessionReady()` 中的自动 `resolveInventory()`，保证在 MythicLib 认为会话 ready 时做一次解析。

综合效果：
- 现在“进服属性刷新链路”是：
  1. MythicLib 会话 ready → 回调 `PlayerData.onSessionReady()` → 初始化 `InventoryResolver` 并解析一次背包。
  2. 你的 `PlayerListener.onPlayerJoin` 会继续安排一次“登录刷新批任务”，做更重/更完整的刷新。
  3. 切档 / 退出时通过 `onSaved` + `LoginRefreshSession.close` 清理残留。
- 相比你之前的设计，`SynchronizedDataLoadEvent` 的那条兜底路径被删掉；但通过 `onSessionReady` 替代，只要 MythicLib 行为符合预期，就不会少刷。

**潜在需要你确认的点**：
- 如果你之前是靠 `SynchronizedDataLoadEvent` 中的“额外一次调度”来修一个特定的竞态（比如某些高延迟环境下 Join 早于数据 ready），现在建议：
  - 在测试服上重点观察：首登 / 频繁切档 / 大量属性加减时，是否仍然出现“属性未刷”或“背包限制未生效”的情况。
  - 如果仍有问题，可以考虑：
    - 保留当前结构，再额外监听更精确的 MythicLib 事件（比如 Profile 完成切换时）并触发一次 `LoginRefreshSession`。

### 4.2 背包解析与套装逻辑

相关文件：
- `InventoryResolver.java`
- `InventoryWatcher.java`
- `PlayerInventoryManager.java`

对魔改的影响：

- 你之前有对 **登录/切档时重复应用/未清理的增益** 做分析与修正，本次上游更新在同一方向上加强：
  - `onClose` 中统一通过 `bufferUpdates` 撤销所有增益、套装修饰器，避免在切档或数据卸载时残留。
  - 套装逻辑拆出 `resetItemSetModifiers()`，更易于你在未来自己扩展。
- 由于我们是基于 upstream 结构重写而非简单 merge，你之前若在这些类里做了**局部变量或细节逻辑魔改**，可能被整体覆盖：
  - 特别是你若对“哪些槽位参与解析”、“某些物品不参与套装统计”做过特殊判断，需要重新核对。

建议：
- 针对你自己关心的“登录装备限制、两手重量、饰品栏（MMOInventory/Ornament）”逻辑，建议再通读一次当前版本的 `InventoryResolver` 与三个 `InventorySupplier`，确认所有特殊规则仍然存在。

### 4.3 物品命令与权限相关魔改

相关文件：
- `UseItem.java`
- `ItemUse.java`
- `ConfigManager.java`
- `config.yml`

你的魔改与本次上游更新目标基本一致：加强对“物品执行命令”的控制与审计。本次同步：

- 没有删除你的中文日志与安全注释，而是在其基础上：
  - 统一用 `itemCommands` + `itemCommandWhitelist` 做开关与白名单。
  - 将部分安全检查从事件层（`ItemUse`）下沉到 `UseItem.scheduleCommandExecution`。

需要注意：

- 如果你之前对“哪些指令可执行”是手写逻辑（如手动判断开头是否是 `/spawn`、`/warp` 等），这些逻辑若写在旧的 `UseItem`/`ItemUse` 中，会被新的统一实现替换掉。  
  现在需要改为在 `config.yml` 的 `item_commands.whitelist` 中用正则表达式表达。

### 4.4 指令树相关魔改

相关文件非常多，主要在：`MMOItems-API/src/main/java/net/Indyuce/mmoitems/command/**`  

影响：

- 由于上游进行了较大规模的指令 API 重构，我们是直接应用官方 patch：
  - 若你之前在这些命令节点内部插入了自定义逻辑（如：
    - 自定义 `mi give` 参数语法；
    - 在 `mi update` 里附加自动备份/打标；
    - 在 `mi list` 增加额外过滤条件），这些改动**有较大概率已被覆盖**。

我们目前无法在有限上下文中精确枚举每一处被覆盖的“旧魔改”细节，建议你按如下方式排查：

1. 使用 Git 对比你上一次的自定义提交与当前工作区，重点关注 `command/mmoitems/**` 目录。  
2. 若发现某些行为确实和你预期不符（例如：自动打版本号、额外权限检查不见了），可以在新 API 结构下重新补回逻辑。

---

## 5. 可能引入的问题与测试建议

### 5.1 潜在问题列表

- **RPG 属性监听过度或遗漏**：
  - 由于从“背包刷新时统一 `refreshStats`”改为“属性监听 + 分散更新”，如果某些 Stat 未被正确注册监听，则对应 RPG 插件中的最大值/加成可能更新不及时。
- **物品指令被错误拦截**：
  - 白名单配置不当或正则写错，会导致某些期望执行的物品指令静默失败（仅写警告日志）。
- **登录刷新链路竞态**：
  - `PlayerData.onSessionReady()` 与 `PlayerListener` 的登录批刷新任务之间存在顺序关系，如果某些服务端/代理环境下事件顺序与本地测试不同，可能会重新暴露你最初修的“登录属性未完全刷出”问题。
- **指令行为差异**：
  - 新的 `Arguments` 封装对错误提示、Tab 补全、参数顺序的处理与旧逻辑略有差异，可能导致某些玩家/运营脚本需要调整用法。

### 5.2 建议的验证场景

1. **登录与切档场景**：
   - 多次重登同一玩家，观察属性、装备限制、饰品栏效果是否稳定。
   - 在开启多档案的环境下频繁切换 Profile，确认不会出现旧档案的增益残留。
2. **RPG 插件联动**：
   - Heroes、MMOCore、AuraSkills/AureliumSkills、RacesAndClasses、Skills/SkillsPro 等常用对接插件，分别验证：
     - 最大血量/体力/法力是否随装备变化实时更新。
     - 升级/更换职业/技能等级变化时，装备限制与属性加成是否正确刷新。
3. **物品命令安全性**：
   - 准备一批带指令的物品（包括危险指令和安全指令），测试：
     - 在不配置白名单时是否都可执行。
     - 配置白名单后，未在白名单中的指令是否被拒绝且写入警告日志。
4. **指令树行为**：
   - 对你常用的 `mi give`、`mi update`、`mi list`、`mi stations`、`mi revid` 等进行回归测试，确认语法与补全符合预期。

---

## 6. 总结与后续工作建议

1. **当前状态**：项目已整体对齐至上游 2025‑11‑19 最新提交，核心模块通过 Maven 编译，现有的大部分魔改（登录属性刷新、中文日志、部分安全增强）仍然存在并已与上游架构合并。
2. **重点关注文件**（建议你手动再审一遍）：
   - `PlayerData.java` / `InventoryResolver.java` / `PlayerListener.java` / `LoginRefreshSession.java`：登录 & 切档属性刷新链路。
   - `UseItem.java` / `ItemUse.java` / `ConfigManager.java` / `config.yml`：物品命令与权限安全控制。
   - `comp/rpg/**`：与目标 RPG 插件联动的 Hook，尤其是 Heroes / MMOCore / Aura/Aurelium。
   - `command/**`：所有终端/游戏内指令的行为与补全。
3. **如果你后续希望**：
   - 我可以按功能模块再拆分更细的“变更清单 + 回滚策略”，例如专门出一份“仅 Heroes/MMOCore 联动”的技术说明，或为登录属性刷新链路画一张时序图。

> 如你有特定关注的魔改点（例如：某一两个类/方法），可以直接点名，我可以基于当前代码再做一次“魔改前后差异对账表”。

