# GuideMea

本项目目标是完整迁移 GuideME 到 Minecraft 1.12.2，尽可能复用其原始代码。

# 技术栈

项目使用 Unimined 构建，Cleanroom 环境开发。

Cleanroom 是 Minecraft 1.12.2 的 Forge 分叉，使用 Java 25，与 Forge API 兼容，添加了诸如 JOML 等一系列实用库。

# 源码位置

你应该优先用 IDEA MCP 查看 GuideME 和 GuideMea 的代码，以及 MC 源码。

1.20.1 的 MC 源码需要你把 MCP 切到 GuideME 后才能阅读。

若确认无法访问 MCP，再使用本地路径。路径如下：

1.12.2 MCP 反编译 + Cleanroom Patch：.gradle\caches\unimined\net\minecraft\minecraft\1.12.2\Cleanroom-FG3

1.20.1 Parchment 映射 + Forge Patch：GuideME\build\moddev\artifacts

若非明确指出，GuideME 源码应在 GuideMea 项目同级。

# 迁移要求

1.20.1 与 1.12.2 的原版类、Forge API 调用、GL 调用等都有较大区别，面对新旧方法，切忌望文生义，要求完整分析调用链路，理清差别，给出长久可用的解决方案。

GuideMea 与 GuideME 虽尽可能相近，但移植时也会做出更改，需要逐个对照。

允许你在不懂的地方留下问题，切忌不懂装懂。
