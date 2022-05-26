安卓BLE模拟方向盘项目，无需任何硬件外设

## 硬件要求

手机和电脑均支持蓝牙低功耗技术（也就是BLE）4.0或以上（一般手机都支持）如果是没用蓝牙的台式机主板，可以购买一个蓝牙适配器

## 使用教程

1. 开启手机蓝牙（不开直接进APP会闪退）
2. 启动BLE广播，把手机离电脑近亿点。否则搜不到蓝牙设备
3. 打开Windows设置添加蓝牙设备，一般会扫描出2个同名的手机设备，一个是手机图标，一个是电脑手机的图标，点击后者进行配对（没搜到就把手机里电脑近亿点）
4. 配对完成后，打开【设备和打印机】界面，等待5s - 30s让Windows配置蓝牙设备
5. 配置设备完成后，电脑手机这个蓝牙设备会变成一个游戏手柄图标，右击这个手柄，【游戏控制器设置】，可以看到有一个【5轴130按钮驾驶控制器】，双击它打开测试界面
6. 当Windows配置蓝牙设备完成时，手机上的驾驶界面会自动弹出。如果没有弹出，也可以从通知栏手动进入
7. 测试
    1. 按钮：随便按手机上的任意按钮，看界面下面对应的红灯有没有亮起
    2. 陀螺仪：点击手机屏幕中间的按钮。启动陀螺仪数据上报。晃动手机，看电脑左边的【转向器/加速器】的箭头有没有运动

8. 目前只支持2个轴向的陀螺仪数据：翻滚角Roll和俯仰角Pitch
9. 在驾驶界面时，点击屏幕中间的按钮，可以暂停陀螺仪的数据报告和校准方向，如果方向不准了，或者临时有事需要离开可以善用这个按钮（点一下暂停，再点一次可以恢复）
10. 驾驶界面里的所有按钮都可以绑定到游戏里。另外左上角和右上角的坐标轴数据显示区域也是2个可用的按钮。顺便音量键也是2个可用的按钮，都可以绑定到游戏里

## 配置功能

同时按下音量键+-，进入编辑模式后（退出编辑模式同理）：

1. 点击按钮可以修改上面的文字
2. 点击左上角或者右上角的坐标数据轴显示区域，可以调整高级参数
   1. 【翻滚角范围】是翻滚角（Roll）方向的活动范围，值越大灵敏度越低，精度越高。通常绑定在方向盘上。竞速类游戏，建议设置为270度。如果是模拟驾驶类游戏，可以设置的更高，比如900度（900度和真实世界里的汽车方向盘旋转角度限制是一致的）
   2. 【俯仰角范围】是俯仰角（Pitch）方向的活动范围，值越大灵敏度越低，精度越高。通常绑定在油门上，建议45-90度（这个轴一般不建议绑定到游戏里，体验可能不太好）
   3. 【数据上报间隔】是将陀螺仪数据上报给PC的间隔，单位毫秒。间隔越小数据上报越频繁，延迟更低，但出现抖动的可能越大。竞速类游戏可以调到40或者30，模拟类游戏建议50左右。越高的值要求手机里电脑越近（信号越好），否则反而会因为数据拥堵造成延迟

## 一些问题

可能是受Android安全机制的限制，每次重启APP后，BLE广播地址都会变化，也就是说，手机无法重连回电脑，只能删掉重新配对。这个很麻烦，但实在是没有办法。所以只要不重启APP，就可以重连

如果短暂的离开电脑前，可以直接退出驾驶界面（此时与电脑保持蓝牙连接消耗的电量非常少，耗电多的其实是驾驶界面的传感器数据读取和数据上报，仅保持蓝牙连接耗电量非常低，不及微信待机消耗的十分之一）

只要不停止通知栏里的BLE广播，这样地址也就不会变，理论上电脑是可以重连的（重启电脑蓝牙开关，就会自动重连），如果尝试多次还是无法重连，就只能重启APP重新配对了