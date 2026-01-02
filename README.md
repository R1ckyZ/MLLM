# MLLM

[English](README.md) | [简体中文](README.zh-CN.md)

Implements LLM-driven phone control with support for complex task interactions. The project uses three agents to collaborate on tasks. Supports Baidu voice wakeup and input, custom prompts, subtask tags, and configurable app launch mappings. Requires Accessibility enabled and Android 11+.

## Operator
- Executes concrete actions to complete tasks step-by-step.
- Uses operation history and current screen state to decide the next action.
- Serves as the primary device-control model.

## Planner
- Selects the most suitable Subtask prompt based on the user request.
- When enabled, runs before Operator to generate a clearer plan.
- If disabled, Operator runs directly.

## Screen Check
- Reviews action results and screen state after each step.
- Produces corrective tasks when it detects mismatches.
- When enabled, runs after Operator steps to refine the next action.

## Demo

请帮我打开笔记添加一个待办，早间会议，时间为今天9点

<video src="./计划待办.mp4"></video>

请帮我打开美团搜索瑞幸咖啡点一杯咖啡，不加糖，点击抢购即可

<video src="./美团外卖.mp4"></video>

## ⭐Star History

[![Star History Chart](https://api.star-history.com/svg?repos=R1ckyZ/MLLM&type=Date)](https://star-history.com/#R1ckyZ/MLLM&Date)
