# ECO 终局子系统实施计划

目标：按已确认的第二版设计实现，仅改变 MMCR 自有的 ECO 实例。使用 Java 21、AE2 19.2.17 和 ECO 21.2.19-beta，在现有功能分支内直接执行。

1. **F9**：在 `EcoGameTest` 对成型通讯口断言 `batchPerFx().unlimited()`、最大执行超频和按 tick 付费。先确认旧 L4 断言失败。`EcoPortBlockEntity` 提供终局能力快照，复用 ECO 的全额扣电路径，不接入冷却液库存；保留 FD、FX 绑定和第一版实体 ID。
2. **C9**：先在无原生 CPU 的网络上查询 C9 空闲 CPU、提交 513 次原木配方和 65 笔独立订单。`EcoMachine` 注册独立机器；计算通讯口拥有订单，稀疏分配原生执行槽，`NEComputationCluster` 复用提交、退料、存储预留、调度和序列化。共用 MMCR 的解绑及区块清理；CPU 复检仅拦截自定义端口。
3. **生命周期与交付**：验证拆机、拔线、三开关和在途订单重载。对照 `run/gameTestServer/logs/latest.log` 核实实际通过数，不用 Gradle 退出码代替 GameTest 断言。运行 `../mmcr-1.21.1/gradlew -p . --offline build runGameTestServer`，检查 jar 没有测试结构，更新 README 和版本。正式结构不在本变更中。
