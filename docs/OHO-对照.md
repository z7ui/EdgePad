# OHO+（One Hand Operation+）能力对照：能不能在小米上复刻

对象：`com.samsung.android.sidegesturepad` v9.3.13.0（minSdk 34，targetSdk 37，8.5 MB），从三星 SM-S9380 的 `/data/app` 拉取。

本机：小米 25098PN5AC，1220×2656 @520dpi，已 root（KSU），导航模式 `navigation_mode=2`（全面屏手势）。

## 一、先给结论

OHO+ 一共 **73 个可绑定的动作**（不算动画、颜色、灵敏度这些设置项）。逐条判完：

| 等级 | 数量 | 含义 |
| :--- | ---: | :--- |
| A 直接可做 | 35 | 公开 API 或无障碍就能实现，不需要额外系统权限 |
| B 要 root | 28 | 走 `svc` / `cmd` / `settings`，本机已有 root |
| C 做不到 | 10 | 依赖 One UI、三星硬件或 Google 服务，普通应用没有接口 |

也就是说 **63 / 73 能复刻**，其中 28 项要 root。真正做不了的只有 10 项，而且大多是三星生态专属。

## 二、它靠什么跑起来

从清单和类结构看，OHO+ 不是「普通应用 + 悬浮窗」那么简单：

```
权限：INJECT_EVENTS、INTERNAL_SYSTEM_WINDOW、STATUS_BAR_SERVICE、
      WRITE_SECURE_SETTINGS、FORCE_STOP_PACKAGES、REMOVE_TASKS、
      MANAGE_ACTIVITY_STACKS、START_TASKS_FROM_RECENTS、DEVICE_POWER …
服务：SGPService（exported，且要求调用方持有 WRITE_SECURE_SETTINGS）
视图：ui.SGPHandleView（边缘手柄）、SGPSwipeAnimationView（滑动反馈）、
      controlpanel / fastaction / miniaction / taskswitcher / widget
对接：RoutineSdkContentProvider —— 12 个手势可以直接当日常程序的触发条件
```

`INJECT_EVENTS` 是关键：它让 OHO+ 能往别的应用里注入按键和触摸。这个权限只有系统签名能拿到，所以它能做「菜单键、方向键、键盘组合键」这类我们够不着的事。

另外它有一条 `gesture_policy_help` 文案：「将使用【单手操作 +】代替您在【设置】中选择的导航手势」——说明它会接管系统导航手势区域。这正是它能安静待在屏幕边缘、又不和返回手势打架的原因。

## 三、小米上的三个硬约束

**1. 原版装得上，跑不起来。** 实测：

```
adb install → Failure [INSTALL_FAILED_USER_RESTRICTED]（MIUI 拦 adb 安装）
su -c pm install -r → Success
am start .settings.SGPSettingsActivity → 进程创建后约 200ms 自行退出
日志：CleanupIncAot: appDiedLocked isCrashing=false fromBinderDied=true
```

没有抛异常，是自己退的——典型的「检测到不是三星环境就退出」。所以别指望装原版。

**2. 边缘是系统返回手势的地盘。** 本机 `navigation_mode=2`、`force_fsg_nav_bar=1`，屏幕左右边缘已经被系统返回占用。第三方悬浮层如果压在边缘 20dp 内，会把返回手势吃掉。OHO+ 靠系统权限让导航手势让位，我们没这个待遇，只能把触钮往里挪，或者让用户改三键导航。

**3. 悬浮窗权限只能手动开。** 实测 `appops set com.z7ui.ruletree SYSTEM_ALERT_WINDOW allow` 后读回仍是 `default`——MIUI 拦脚本授权。用户得自己去「设置 → 应用 → 特殊权限 → 显示在其他应用上层」点一次。

## 四、逐条对照

### 导航与窗口

| 功能 | 资源键 | 复刻途径 | 等级 | 备注 |
| :--- | :--- | :--- | :--- | :--- |
| 返回键 | `s_back_key` | 无障碍 GLOBAL_ACTION_BACK | A | 现成 |
| 主屏幕键 | `s_home_key` | GLOBAL_ACTION_HOME | A | 现成 |
| 最近键 | `s_recent_key` | GLOBAL_ACTION_RECENTS | A | 现成 |
| 菜单键 | `s_menu_key` | 注入 KEYCODE_MENU（无无障碍对应） | B | root input |
| 上一个应用程序 | `s_previous_app` | 无障碍记录前台包名，再重开上一个 | A | 近似，丢页面状态 |
| 关闭应用程序 | `s_kill_app` | am force-stop 当前包 | B | root |
| 全部关闭 | `s_close_all` | am kill-all / 脚本点最近任务清空 | B | root |
| 应用程序屏幕 | `s_application_key` | 注入 KEYCODE_ALL_APPS | B | root |
| 任务切换器 | `s_task_switcher` | 自家面板 + 无障碍事件记录的最近列表 | A | 近似 |
| 浮动导航按钮 | `s_show_softkey` | 悬浮窗画三个键 + 全局动作 | A | 现成能力拼装 |
| 显示/隐藏导航条 | `s_toggle_navibar` | 改动系统导航栏可见性 | B | WRITE_SECURE_SETTINGS / root |
| 隐藏触钮 | `s_handle_hide` | 自家手柄开关 | A | 现成 |

### 面板与工具

| 功能 | 资源键 | 复刻途径 | 等级 | 备注 |
| :--- | :--- | :--- | :--- | :--- |
| 通知面板 | `s_open_notipanel` | GLOBAL_ACTION_NOTIFICATIONS | A | 现成 |
| 快捷面板 | `s_open_quickpanel` | GLOBAL_ACTION_QUICK_SETTINGS | A | 现成 |
| 电源键菜单 | `s_function_global_action` | GLOBAL_ACTION_POWER_DIALOG（API 30+） | A | 现成 |
| 快速工具 | `s_control_panel` | 自家悬浮面板：音量 / 亮度 / 播放控制 | A | 亮度那格要 B |
| 快速启动器 | `s_fast_action` | 手势末端拖到图标上松手，自家 UI | A | 纯自绘 |
| 小组件弹出窗口 | `s_widget_popup` | AppWidgetHost 嵌进悬浮窗 | A | 实现偏重 |
| 手动运行日常程序 | `s_manual_routine` | 规则树的「手动运行」 | A | 已有 |

### 屏幕与显示

| 功能 | 资源键 | 复刻途径 | 等级 | 备注 |
| :--- | :--- | :--- | :--- | :--- |
| 截屏 | `s_screenshot` | 无障碍 takeScreenshot | A | 现成 |
| 屏幕关闭 | `s_screen_off` | GLOBAL_ACTION_LOCK_SCREEN | A | 现成 |
| 录屏工具 | `s_screen_recorder` | MediaProjection | A | 每次要用户点一次授权 |
| 单手模式 | `s_reduce_screen` | MIUI 自带，无第三方入口 | C | 系统专属 |
| 移动屏幕 | `s_screen_free_move` | One UI 的显示变换 | C | 系统专属 |
| 向下拉屏幕 | `s_move_down_screen` | dispatchGesture 从上方拉 | A | 近似 |
| 向上移动 | `s_move_up_key` | 注入 KEYCODE_DPAD_UP | B | root |
| 向下移动 | `s_move_down_key` | 注入 KEYCODE_DPAD_DOWN | B | root |
| 向前移动 | `s_move_forward` | 注入 KEYCODE_DPAD_RIGHT | B | root |
| 向后移动 | `s_move_backward` | 注入 KEYCODE_DPAD_LEFT | B | root |
| 向右旋转 | `s_user_rotation` | wm user-rotation | B | root |
| 向左旋转 | `s_user_rotation2` | wm user-rotation | B | root |
| 自动旋转 | `s_quick_rotation` | Settings.System.ACCELEROMETER_ROTATION | B | 已实现 |
| 自动调节亮度 | `s_auto_brightness` | Settings.System.SCREEN_BRIGHTNESS_MODE | B | 已实现同类 |
| 颜色反转 | `s_color_inversion` | Settings.Secure 显示反色开关 | B | root |
| 护眼模式 | `s_toggle_bluelight` | 改显示色温 / 护眼开关 | B | MIUI 键名待验证 |
| 防窥模式 | `s_toggle_privacydisplay` | 靠屏幕硬件 | C | 三星硬件 |
| 显示/隐藏任务栏 | `s_toggle_taskbar` | 平板 / One UI 特性 | C | 系统专属 |
| 显示侧屏幕面板 | `s_show_edge` | One UI Edge 面板 | C | 系统专属 |
| 虚拟触控板 | `s_touch_assist` | 自绘光标 + dispatchGesture 点击 / 滑动 | A | 近似，不是真鼠标指针 |
| 智能多截图 | `s_smartcapture` | 截图 + 自绘裁剪编辑 | A | 近似，交互工作量大 |
| AI 多截图 | `s_smart_select` | 依赖三星 / Google 模型 | C | 无开放接口 |
| 圈定即搜 | `s_run_searcle` | 依赖 Google / 三星服务 | C | 无开放接口 |

### 应用与启动

| 功能 | 资源键 | 复刻途径 | 等级 | 备注 |
| :--- | :--- | :--- | :--- | :--- |
| 启动应用程序 | `s_launch_app` | getLaunchIntentForPackage | A | 现成 |
| 主屏幕快捷方式 | `s_launch_shortcut` | LauncherApps.startShortcut | C | 普通应用被 SecurityException 拒（已实测） |
| 弹出窗口启动 | `s_launch_popup` | am start --windowingMode 5 | B | root；MIUI 小窗兼容性待测 |
| 最小化弹出窗口 | `s_min_popup_view` | 系统窗口管理 | B | root，稳定性一般 |
| 快速弹出视图 | `s_run_popup_view` | 同上 | B | root |
| 分屏视图 | `s_run_split_view` | ActivityOptions 分屏 / root 兜底 | A | 部分 App 不配合 |
| 辅助应用程序 | `s_run_assist` | Intent.ACTION_ASSIST | A | 现成 |
| 搜索查找 | `s_run_finder` | ACTION_WEB_SEARCH 或唤起桌面搜索 | A | 近似 |
| 在应用程序中搜索 | `s_search_key` | 注入 KEYCODE_SEARCH | B | root |
| 前进（浏览器） | `s_forward` | 注入 KEYCODE_FORWARD | B | root |
| 键盘快捷方式 | `s_context_menu` | 需要组合键注入 | C | 本机 input 无 keycombination，只剩 sendevent 硬拼 |
| 模式（启动方式） | `s_launch_modes` | 启动参数选择 | A | 纯配置项 |

### 声音与系统开关

| 功能 | 资源键 | 复刻途径 | 等级 | 备注 |
| :--- | :--- | :--- | :--- | :--- |
| 静音开关 | `s_volume_mute` | AudioManager 调铃声模式 | A | 现成 |
| 音量增加键 | `s_volume_up_key` | adjustStreamVolume + | A | 现成 |
| 音量减小键 | `s_volume_down_key` | adjustStreamVolume - | A | 现成 |
| 下一首 | `s_key_media_nextsong` | dispatchMediaKeyEvent | A | 现成 |
| 播放/暂停 | `s_key_media_playpause` | dispatchMediaKeyEvent | A | 现成 |
| 上一首 | `s_key_media_prevsong` | dispatchMediaKeyEvent | A | 现成 |
| 声音模式 | `s_quick_soundmode` | 改铃声模式 | A | 需勿扰访问，已有 |
| 免打扰 | `s_quick_dnd` | setInterruptionFilter | A | 已实现 |
| 手电筒 | `s_toggle_flash` | CameraManager.setTorchMode | A | 已实现 |
| WiFi | `s_quick_wifi` | svc wifi / Settings | B | root，已验证 |
| 蓝牙 | `s_quick_bluetooth` | svc bluetooth | B | root，已验证 |
| 飞行模式 | `s_quick_airplane` | cmd connectivity airplane-mode | B | root，已验证 |
| 移动数据 | `s_quick_mobiledata` | svc data | B | root，已验证 |
| 移动热点 | `s_quick_hotspot` | cmd wifi start-softap | B | root，已验证 |
| NFC | `s_quick_nfc` | svc nfc | B | root，已验证 |
| 夜间模式 | `s_quick_nightmode` | cmd uimode night | B | root，已验证 |
| 自动息屏 | `s_quick_timeout` | screen_off_timeout | B | 已实现 |
| 游戏助推器 | `s_quick_gametools` | 三星 Game Launcher | C | 系统专属 |
| 无 | `s_none` | 不绑定动作 | A | 现成 |

## 五、如果要做，缺的其实只有一层壳

规则树现在已经有 51 个动作、17 个条件，上面 A/B 两档里的大部分动作它都会做。真正缺的是 **边缘手柄 + 12 种手势识别 + 动作绑定界面**，其余是现成的：

- 手势识别：一侧 3 个方向（水平、斜上、斜下）× 短滑 / 长滑 = 6 种，两侧 12 种；滑完停留 300ms 进「快速启动器」，松手落在哪个图标就开哪个
- 手柄：`TYPE_APPLICATION_OVERLAY` 悬浮窗，宽 16–24dp、高度和垂直位置可调、透明度可调；默认往屏幕内侧缩 8dp，避开系统返回热区
- 动作：直接复用现有的 `Actions` 和 `Capabilities`，不用重写
- 保活：沿用现有前台服务 + 看门狗

工作量估计：手柄与手势识别约 400 行，配置页约 300 行，动作绑定与例外清单约 150 行。和之前一个版本的量级相当。

不建议做的部分：菜单键/方向键这类要注入按键的（要 `sendevent` 硬拼，机型相关）、单手模式与移动屏幕（系统专属）、主屏幕快捷方式（`LauncherApps.startShortcut` 对普通应用直接抛 SecurityException，之前实测过）。

## 六、实测记录

| 项目 | 结果 |
| :--- | :--- |
| root 安装 OHO+ 到小米 | Success |
| 启动设置页 | 进程 200ms 内退出，无 Activity，`isCrashing=false` |
| 脚本授予悬浮窗 | `appops set` 无效，读回 `default` |
| 系统导航 | `navigation_mode=2`，全面屏手势占用左右边缘 |
| `input` 子命令 | 有 keyevent / swipe / motionevent，**没有 keycombination** |
| 卸载清理 | 已卸载 OHO+，删除临时 APK |
