# user-entry

## Purpose

用户入口与交易时段状态提示。

## Requirements

### Requirement: 用户入口
系统 SHALL 提供 userId 输入入口,并将 userId 持久化到 localStorage,用于后续 API 调用与路由。

#### Scenario: 输入 userId 进入
- **WHEN** 用户首次访问且本地无 userId
- **THEN** 页面展示 userId 输入框,输入后进入自选估值列表,并写入 localStorage

#### Scenario: 已有 userId 直接进入
- **WHEN** localStorage 中存在 userId
- **THEN** 页面直接进入自选估值列表,顶部展示当前 userId,可随时切换

### Requirement: 交易时段状态
系统 SHALL 判断当前是否处于 A 股交易时段,用于页面提示。

#### Scenario: 非交易时段提示
- **WHEN** 处于非交易时段
- **THEN** 页面展示"非交易时段,显示最近交易日估值"的提示

#### Scenario: 交易时段无提示
- **WHEN** 处于交易时段
- **THEN** 页面不展示该提示
