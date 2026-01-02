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

[Demo Video](https://www.bilibili.com/video/BV1qei8BMEYu)

The presentation is as follows:

1. Please help me open the note and add a to-do, morning meeting, time is 9 o'clock today

2. Please help me open Meituan, search for Luckin Coffee, order a cup of coffee without sugar, and click to buy it.

## ⭐Star History

[![Star History Chart](https://api.star-history.com/svg?repos=R1ckyZ/MLLM&type=timeline&legend=top-left)](https://www.star-history.com/#R1ckyZ/MLLM&type=timeline&legend=top-left)
