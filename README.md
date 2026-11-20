# MMCR Tweaks

Some features I need but will not done in MMCR

integration of Neo AE ECO
integration of AE2LT

## ECO 合成子系统（`mmcr:eco`）

机器控制器为 `mmcr:eco_controller`。在 MMCR 为 `mmcr:eco` 定义的结构中放入以下部件：

| 部件 | 方块 ID | 数量 |
| --- | --- | --- |
| 对外 ME 通讯口（固定 L4 主机） | `mmcr_tweaks:eco_me_port` | 1 |
| FD 智能样板总线 | `neoecoae:crafting_pattern_bus` | 至少 1 |
| FX 工作核心 | `neoecoae:crafting_worker` | 至少 1 |

只给通讯口接 ME 网络；FD、FX 不用挨着通讯口或彼此连接。把普通合成样板放进 FD，从有电、有存储和合成 CPU 的 ME 网络下单。这里提供的是合成设备，不提供 CPU。

**首版不含正式形状。** 给已注册的机器 ID 配置结构即可，不要重复注册机器定义。仅用于开发验证的结构见 [EcoGameTest.java](src/gametest/java/cn/howxu/mmcr_tweaks/EcoGameTest.java)，不随模组 jar 分发。

### 构建与测试

Java 21、Gradle 9.6.0，在项目根目录运行：

```sh
gradle build runGameTestServer
```

测试使用 MMCR `0.1.8+dev-258dabdf6`、AE2 `19.2.17`、ECO `21.2.19-beta`。可用 `-PmmcrJar=/path/to/mmcr.jar` 或 `-PecoJar=/path/to/neoecoae.jar` 测本地版本。GameTest 会在服务器里实际提交订单；重载用例在 FX 已收料时保存实体 NBT、调用卸载回调并重建实体，不会强卸 GameTest 共用的区块。
