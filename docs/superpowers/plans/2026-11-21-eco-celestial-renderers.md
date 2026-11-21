# 双星渲染实施计划

> For agentic workers: use superpowers:executing-plans for inline execution. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在真实 F9 与 C9 建筑上演出铸星和吞星。

**Architecture:** 两套程序化场景共用一个仅提交四边形的画笔；MMCR 客户端回调负责可见性、局部坐标和批次。实体表面写深度，光丝深度测试但不写深度。

**Tech Stack:** Java 21、NeoForge 1.21.1、MMCR `RegisterControllerRenderersEvent`、Minecraft `PoseStack`/`MultiBufferSource`。

## Global Constraints

使用相邻工程固定 `0.0.0.jar`，不更换依赖。结构和订单不变；不新增服务端 tick、同步包或粒子。朝向以 SOUTH 为原始朝向；垂直机器额外对齐控制器的 roll。视距有限，远景省略细节。发行包不含验收脚本和布局。

### 1. 锚点与渲染层

- [x] `src/gametest/java/cn/howxu/mmcr_tweaks/client/CelestialSceneTest.java`：先测变换与 MMCR `BlockRotator.rotateSouthTo` 一致、两台主星实体范围、远景减面和动画变化。
- [x] `src/main/java/cn/howxu/mmcr_tweaks/client/CelestialMesh.java`：在机器中心轴局部坐标中提交带颜色的四边形、球面、朝向镜头的光带和星芒。
- [x] `CelestialRenderers.java`（同包）：仅 `Dist.CLIENT` 注册；`formed && structureAreaLoaded` 决定是否绘制。用玩家镜头计算局部视线，避免朝向或半格误差。

### 2. 两套演出

- [x] `StellarForge.java`：依次绘制半径 3 的恒星、顶部磁弧、腰部点火、中轴、反转断环、卫星与 320 tick 合环扫波。
- [x] `EventHorizon.java`：半径 2.5 的不透明黑心、倾斜吸积盘和透镜亮弧；底座光脉、八极加速引流、400 tick 周期伴星与双喷流。

### 3. 验收与交付

- [x] `../mmcr-1.21.1/gradlew --offline build runGameTestServer`：原 8 项及新增几何回归通过；`jar tf` 不含测试和结构。
- [x] 用两份原脚本及临时存档副本实际启动客户端，检查成型、近景、周期与远景，留截图；不在玩家原存档做破坏性验收。视觉检查与服务端验收分开报告。
