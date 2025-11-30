<h1>
    <img src="worldguard-logo.svg" alt="WorldGuard" width="400" /> 
</h1>

WorldGuard lets you and players guard areas of land against griefers and undesirables, as well as tweak and disable various gameplay features of Minecraft.

* Block creeper and wither block damage, falling damage, etc.
* Disable fire spread, lava fire spread, ice formation, Endermen picking up blocks, etc.
* Blacklist certain items and blocks so they can't be used
* Warn moderators when certain items and blocks are used
* Protect areas of your world so only certain people can build in them
* Set areas where PVP, TNT, mob damage, and other features are disabled
* Protect your server from various 'exploits' like magical obsidian creation machines
* Disable, or enable, various Minecraft features, like sponges from classic
* Add useful commands like an immediate "STOP ALL FIRE SPREAD" command
* Enable only features you want! Everything is off by default

WorldGuard is open source and is available under the GNU Lesser
General Public License v3.

A Bukkit server implementation (such as [Paper](https://papermc.io)) and the [WorldEdit plugin](https://dev.bukkit.org/projects/worldedit) are required to use WorldGuard. You can get a release copy of WorldGuard from the [BukkitDev site](https://dev.bukkit.org/projects/worldguard).

## 🎉 Fork Features / Fork 新特性

### English

This fork integrates **all WorldGuardExtraFlags features** directly into WorldGuard core! You no longer need to install the WorldGuardExtraFlags plugin separately.

#### ✨ Built-in Features

**Movement & Speed**
- `walk-speed` - Control player walking speed
- `fly-speed` - Control player flying speed
- `fly` - Allow/deny flying in regions
- `glide` - Force/allow/deny elytra gliding

**Teleportation**
- `teleport-on-entry` - Teleport players when entering a region
- `teleport-on-exit` - Teleport players when leaving a region

**Commands**
- `command-on-entry` - Execute commands as player on region entry
- `command-on-exit` - Execute commands as player on region exit
- `console-command-on-entry` - Execute console commands on region entry
- `console-command-on-exit` - Execute console commands on region exit

**Player Effects**
- `blocked-effects` - Block specific potion effects in regions
- `give-effects` - Give potion effects to players in regions
- `play-sounds` - Play sounds periodically in regions

**Death & Respawn**
- `keep-inventory` - Keep inventory on death
- `keep-exp` - Keep experience on death
- `respawn-location` - Set custom respawn location

**Chat & Display**
- `chat-prefix` - Add prefix to player chat
- `chat-suffix` - Add suffix to player chat

**Protection**
- `godmode` - Enable invincibility in regions
- `worldedit` - Control WorldEdit usage in regions

**World Mechanics**
- `frostwalker` - Control frost walker enchantment
- `nether-portals` - Control nether portal creation
- `chunk-unload` - Control chunk unloading
- `item-durability` - Control item durability loss

**Other**
- `join-location` - Set spawn location when joining server

#### 🆕 Additional Features

**World Whitelist System**
- Control which worlds WorldGuard protections apply to
- Whitelist or blacklist specific worlds
- Improve performance by excluding unnecessary worlds

**MiniMessage Support**
- Modern text formatting with MiniMessage syntax
- Rich text colors, gradients, and decorations
- Support for hover text and click events
- Example: `<gradient:red:blue>Gradient Text</gradient>`

**Internationalization (i18n)**
- Multi-language support for all messages
- Configurable language settings
- Easy to add custom translations
- Supports: English, Chinese (Simplified), and more

#### 🔧 Migration from WorldGuardExtraFlags

If you previously used WorldGuardExtraFlags:
1. **No configuration changes needed** - All your existing flags will work automatically
2. **Remove WorldGuardExtraFlags.jar** - The plugin is no longer needed and will be automatically disabled
3. **Restart your server** - All features are now built-in!

---

### 中文

此Fork版本将 **WorldGuardExtraFlags 的所有功能** 直接集成到了WorldGuard核心中！你不再需要单独安装WorldGuardExtraFlags插件。

#### ✨ 内置功能

**移动与速度**
- `walk-speed` - 控制玩家行走速度
- `fly-speed` - 控制玩家飞行速度
- `fly` - 允许/禁止在区域内飞行
- `glide` - 强制/允许/禁止鞘翅滑翔

**传送**
- `teleport-on-entry` - 进入区域时传送玩家
- `teleport-on-exit` - 离开区域时传送玩家

**命令**
- `command-on-entry` - 进入区域时以玩家身份执行命令
- `command-on-exit` - 离开区域时以玩家身份执行命令
- `console-command-on-entry` - 进入区域时以控制台身份执行命令
- `console-command-on-exit` - 离开区域时以控制台身份执行命令

**玩家效果**
- `blocked-effects` - 阻止特定药水效果
- `give-effects` - 给予玩家药水效果
- `play-sounds` - 在区域内周期性播放声音

**死亡与重生**
- `keep-inventory` - 死亡时保留物品栏
- `keep-exp` - 死亡时保留经验
- `respawn-location` - 设置自定义重生位置

**聊天与显示**
- `chat-prefix` - 为玩家聊天添加前缀
- `chat-suffix` - 为玩家聊天添加后缀

**保护**
- `godmode` - 在区域内启用无敌模式
- `worldedit` - 控制WorldEdit在区域内的使用

**世界机制**
- `frostwalker` - 控制冰霜行者附魔
- `nether-portals` - 控制下界传送门创建
- `chunk-unload` - 控制区块卸载
- `item-durability` - 控制物品耐久度损失

**其他**
- `join-location` - 设置玩家加入服务器时的出生位置

#### 🆕 额外功能

**世界白名单系统**
- 控制WorldGuard保护应用于哪些世界
- 白名单或黑名单特定世界
- 通过排除不必要的世界提升性能

**MiniMessage支持**
- 使用MiniMessage语法的现代文本格式化
- 丰富的文本颜色、渐变和装饰
- 支持悬停文本和点击事件
- 示例: `<gradient:red:blue>渐变文本</gradient>`

**国际化支持 (i18n)**
- 所有消息支持多语言
- 可配置的语言设置
- 易于添加自定义翻译
- 支持: 英语、简体中文等多种语言

#### 🔧 从WorldGuardExtraFlags迁移

如果你之前使用了WorldGuardExtraFlags：
1. **无需修改配置** - 所有现有的flag设置将自动生效
2. **删除WorldGuardExtraFlags.jar** - 不再需要该插件，系统会自动禁用它
3. **重启服务器** - 所有功能现已内置！

#### 📝 使用示例

```bash
# 设置行走速度
/region flag spawn walk-speed 0.5

# 进入时传送
/region flag spawn teleport-on-entry 100,64,100,world

# 进入时执行命令
/region flag spawn command-on-entry "/say 欢迎 %username%"

# 给予药水效果
/region flag spawn give-effects speed 1 true

# 保持物品栏
/region flag spawn keep-inventory true
```

---

Compiling
---------

See [COMPILING.md](COMPILING.md).

Contributing
------------

We happily accept contributions, especially through pull requests on GitHub.

Please read CONTRIBUTING.md for important guidelines to follow.

Submissions must be licensed under the GNU Lesser General Public License v3.

Links
-----

* [Homepage](https://enginehub.org/worldguard)
* [Discord](https://discord.gg/enginehub)
* [Issue tracker](https://github.com/EngineHub/WorldGuard/issues)
* [Continuous integration](https://builds.enginehub.org) [![Build Status](https://ci.enginehub.org/app/rest/builds/buildType:bt11,branch:master/statusIcon.svg)](http://ci.enginehub.org/viewType.html?buildTypeId=bt11&guest=1)
* [End-user documentation](https://worldguard.enginehub.org/en/latest/)
