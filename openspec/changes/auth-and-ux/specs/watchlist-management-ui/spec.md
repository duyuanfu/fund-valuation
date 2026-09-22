## MODIFIED Requirements

### Requirement: 排序自选
系统 SHALL 允许用户调整自选基金展示顺序。

#### Scenario: 拖拽调整顺序
- **WHEN** 用户拖拽某只自选基金行到新位置
- **THEN** 请求后端保存新顺序,列表按新顺序渲染

#### Scenario: 调整顺序
- **WHEN** 用户改变某只基金的顺序
- **THEN** 请求后端保存新顺序,列表按新顺序渲染
