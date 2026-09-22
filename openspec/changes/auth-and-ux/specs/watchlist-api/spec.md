## MODIFIED Requirements

### Requirement: 自选估值接口
系统 SHALL 提供自选基金估值列表接口,用户身份由 JWT 解析,返回该用户的全部自选基金估值。

#### Scenario: 鉴权获取列表
- **WHEN** 客户端携带有效 token 请求自选估值列表
- **THEN** 返回该 token 对应用户的全部自选基金估值

#### Scenario: 未授权访问
- **WHEN** 请求无有效 token
- **THEN** 返回 401

### Requirement: 自选管理接口
系统 SHALL 提供自选基金的添加、删除与排序接口,用户身份由 JWT 解析。

#### Scenario: 添加自选
- **WHEN** 携带有效 token 添加基金
- **THEN** 为用户关联基金(基金不存在时先采集元数据)

#### Scenario: 删除与排序
- **WHEN** 携带有效 token 删除或重排
- **THEN** 仅影响该用户的自选数据
