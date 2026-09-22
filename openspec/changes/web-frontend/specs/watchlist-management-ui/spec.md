## ADDED Requirements

### Requirement: 添加自选
系统 SHALL 允许用户输入基金代码添加自选基金。

#### Scenario: 成功添加
- **WHEN** 用户输入基金代码并提交
- **THEN** 请求后端添加,成功后刷新列表并清空输入

#### Scenario: 添加失败提示
- **WHEN** 基金已在自选或代码无效
- **THEN** 展示后端返回的错误提示,不清空列表

### Requirement: 删除自选
系统 SHALL 允许用户删除自选基金。

#### Scenario: 删除基金
- **WHEN** 用户确认删除某只自选基金
- **THEN** 请求后端删除并刷新列表

### Requirement: 排序自选
系统 SHALL 允许用户调整自选基金展示顺序。

#### Scenario: 调整顺序
- **WHEN** 用户改变某只基金的顺序
- **THEN** 请求后端保存新顺序,列表按新顺序渲染
