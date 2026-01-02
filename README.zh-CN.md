# MLLM

[简体中文](README.zh-CN.md) | [English](README.md)

在手机上实现 LLM 操作手机，支持复杂任务交互。本项目通过三类 agent 协同完成任务。支持百度语音唤醒和输入、提示词自建、子任务标签、启动应用自添加。需要开启无障碍模式，Android 11+。

## Operator（操作者）
- 执行具体操作，逐步完成任务。
- 依据操作历史与当前屏幕状态给出下一步动作。
- 作为设备控制的主模型。

## Planner（计划者）
- 根据用户需求选择最合适的 Subtask 提示词。
- 启用时在 Operator 之前运行，用于生成更清晰的子任务或规划。
- 关闭时直接由 Operator 执行。

## Screen Check（引导者）
- 在每步执行后检查动作结果与屏幕状态。
- 发现偏差时生成纠正任务。
- 启用时在 Operator 步骤之后运行，用于优化下一步动作。

## Demo

[演示视频](https://www.bilibili.com/video/BV1qei8BMEYu)

演示内容如下：

1.请帮我打开笔记添加一个待办，早间会议，时间为今天9点

2.请帮我打开美团搜索瑞幸咖啡点一杯咖啡，点击抢购即可

## ⭐Star History

[![Star History Chart](https://api.star-history.com/svg?repos=R1ckyZ/MLLM&type=date&legend=top-left)](https://www.star-history.com/#R1ckyZ/MLLM&type=date&legend=top-left)
