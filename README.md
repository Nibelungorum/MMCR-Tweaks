# MMCR Tweaks

Some features I need but will not done in MMCR

integration of Neo AE ECO
integration of AE2LT

## ECO 合成子系统（`mmcr:eco`）

机器控制器为 `mmcr:eco_controller`。在 MMCR 为 `mmcr:eco` 定义的结构中放入以下部件：

| 部件 | 方块 ID | 数量 |
| --- | --- | --- |
| F9 合成通讯口（沿用首版方块） | `mmcr_tweaks:eco_me_port` | 1 |
| FD 智能样板总线 | `neoecoae:crafting_pattern_bus` | 至少 1 |
| FX 工作核心 | `neoecoae:crafting_worker` | 至少 1 |

只给通讯口接 ME 网络；FD、FX 不用挨着通讯口或彼此连接。把普通合成样板放进 FD，从有电、有存储和合成 CPU 的 ME 网络下单。这里提供的是合成设备，不提供 CPU。

成型后免冷却，不需要流体仓。每个 FX 是一条通道：对于 ECO 能验证的合成样板，配合下面的计算子系统，同一配方可整批执行，单批最多 `Long.MAX_VALUE` 次。足电时一批在首个工作 tick 完成；整个合成子系统每个工作 tick 只付一次 **10,000,000 AE** 基础工作费，受 AE 用电倍率影响。缺电等待，输出阻塞时不保证立即交付。普通 AE2 CPU 也可使用 FD，但不要指望它像 ECO CPU 一样整批派单。

## ECO 计算子系统（`mmcr:eco_cpu`）

在 `mmcr:eco_cpu_controller` 对应的 MMCR 结构中放 **一个 `mmcr_tweaks:eco_cpu_port`**，把它接到 ME 网络即可。可以和合成通讯口共用网络，不需要其他合成 CPU、原生计算主机、线程核心、驱动器或晶阵。

成型的 C9 通讯口提供 `Long.MAX_VALUE` 字节的总存储、`Integer.MAX_VALUE` 的线程和并行上限。终端中的空闲 CPU 是接单入口；订单占用的字节在结清后才释放。右击计算通讯口使用 ECO 计算面板。新网络默认接受玩家和自动化下单，开启深度规划、循环规划和 ECO 快速规划。接到已配置的网络则沿用原设置。

**两台机器都不附正式形状。** 给这两个已注册的机器 ID 配置结构即可，不要重复注册机器定义。验收布局在 [src/gametest](src/gametest)，不随模组 jar 分发。

### 构建与测试

Java 21、Gradle 9.6.0，在项目根目录运行：

```sh
gradle -p ../mmcr-1.21.1 jar
gradle build runGameTestServer
```

MMCR 固定读取 `../mmcr-1.21.1/build/libs/mmcr-neoforge-mc1.21.1-0.0.0.jar`。需要包含提交 `1eb11da9a` 的本地构建：F9 和 C9 使用该版本的 `serverTick(Supplier)`，每个控制器独立维护结构绑定。旧开发包也可能显示 `0.0.0`，更新源码后请重新运行上面的 `jar` 任务。开发用的 KubeJS、Rhino、Jade，以及客户端的 JEI、Sodium、ModernUI 等，跟随相邻工程 `gradle.properties` 的版本，不是玩家必装依赖。ECO 仍使用 `21.2.19-beta`：相邻工程的 `21.2.6` 缺少 C9 所用的 API。可用 `-PecoJar=/path/to/neoecoae.jar` 测试兼容的本地 ECO 包。

GameTest 实际下单验证单 FX 的 513 次整批、缺电后恢复、双 FX 同 tick 扣费、65 单并发和退料。重载测试保存持有订单的 CPU 和已收料 FX 的 NBT，调用卸载回调并重建实体；不强卸 GameTest 共用的区块，也不模拟整个服务器重启。
