import { useEffect, useState } from 'react'
import {
  Card,
  Table,
  Tag,
  Button,
  Input,
  DatePicker,
  Select,
  Switch,
  Space,
  Modal,
  Typography,
  Popconfirm,
  message,
  Row,
  Col,
  Statistic,
  Tabs,
  Form,
  InputNumber,
  Divider,
  Alert,
} from 'antd'
import {
  UserOutlined,
  CalendarOutlined,
  NotificationOutlined,
  DashboardOutlined,
  ReloadOutlined,
  CrownOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  EyeOutlined,
  DeleteOutlined,
  PlusOutlined,
  ThunderboltOutlined,
  ArrowLeftOutlined,
  SoundOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import type {
  UserView,
  CalendarHolidayView,
  SystemStats,
  EstimateResult,
} from '../api/types'
import { colorOf, fmtNav, fmtPct } from '../utils/format'
import dayjs from 'dayjs'

const { Title, Text, Paragraph } = Typography

export default function AdminPage() {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState('users')

  /* ---------------- 1. 用户管理状态 ---------------- */
  const [users, setUsers] = useState<UserView[]>([])
  const [loadingUsers, setLoadingUsers] = useState(false)
  const [selectedUser, setSelectedUser] = useState<UserView | null>(null)
  const [userWatchlist, setUserWatchlist] = useState<EstimateResult[]>([])
  const [loadingWatchlist, setLoadingWatchlist] = useState(false)
  const [watchlistModalOpen, setWatchlistModalOpen] = useState(false)

  // VIP 设置弹窗
  const [vipModalOpen, setVipModalOpen] = useState(false)
  const [vipUser, setVipUser] = useState<UserView | null>(null)
  const [vipEnabled, setVipEnabled] = useState(false)
  const [vipExpireDate, setVipExpireDate] = useState<dayjs.Dayjs | null>(null)

  const loadUsers = async () => {
    setLoadingUsers(true)
    try {
      const list = await api.listUsers()
      setUsers(list)
    } catch (e) {
      message.error((e as Error).message)
    } finally {
      setLoadingUsers(false)
    }
  }

  const handleToggleStatus = async (username: string, currentStatus: string) => {
    const nextStatus = currentStatus === 'NORMAL' ? 'DISABLED' : 'NORMAL'
    try {
      await api.updateUserStatus(username, nextStatus)
      message.success(`已将用户 ${username} 设为 ${nextStatus === 'NORMAL' ? '正常' : '已停用'}`)
      loadUsers()
    } catch (e) {
      message.error((e as Error).message)
    }
  }

  const handleToggleRole = async (username: string, currentRole: string) => {
    const nextRole = currentRole === 'ADMIN' ? 'USER' : 'ADMIN'
    try {
      await api.updateUserRole(username, nextRole)
      message.success(`已将用户 ${username} 角色调整为 ${nextRole}`)
      loadUsers()
    } catch (e) {
      message.error((e as Error).message)
    }
  }

  const openVipModal = (u: UserView) => {
    setVipUser(u)
    setVipEnabled(u.isVip)
    setVipExpireDate(u.vipExpireAt ? dayjs(u.vipExpireAt) : null)
    setVipModalOpen(true)
  }

  const handleSaveVip = async () => {
    if (!vipUser) return
    try {
      const expireStr = vipExpireDate ? vipExpireDate.format('YYYY-MM-DDTHH:mm:ss') : null
      await api.updateUserVip(vipUser.username, vipEnabled, expireStr)
      message.success(`用户 ${vipUser.username} VIP 会员状态已保存`)
      setVipModalOpen(false)
      loadUsers()
    } catch (e) {
      message.error((e as Error).message)
    }
  }

  const handleViewWatchlist = async (u: UserView) => {
    setSelectedUser(u)
    setWatchlistModalOpen(true)
    setLoadingWatchlist(true)
    try {
      const list = await api.getUserWatchlist(u.username)
      setUserWatchlist(list)
    } catch (e) {
      message.error((e as Error).message)
    } finally {
      setLoadingWatchlist(false)
    }
  }

  /* ---------------- 2. 交易日历状态 ---------------- */
  const [customHolidays, setCustomHolidays] = useState<CalendarHolidayView[]>([])
  const [allDynamicHolidays, setAllDynamicHolidays] = useState<string[]>([])
  const [loadingCalendar, setLoadingCalendar] = useState(false)
  const [newHolidayDate, setNewHolidayDate] = useState<dayjs.Dayjs | null>(null)
  const [newHolidayDesc, setNewHolidayDesc] = useState('')

  const loadCalendar = async () => {
    setLoadingCalendar(true)
    try {
      const res = await api.getCalendarHolidays()
      setCustomHolidays(res.customHolidays || [])
      setAllDynamicHolidays(res.allDynamicHolidays || [])
    } catch (e) {
      message.error((e as Error).message)
    } finally {
      setLoadingCalendar(false)
    }
  }

  const handleAddHoliday = async () => {
    if (!newHolidayDate) {
      message.warning('请选择休市日期')
      return
    }
    try {
      await api.addCalendarHoliday(newHolidayDate.format('YYYY-MM-DD'), newHolidayDesc || '管理员添加休市日')
      message.success('休市日添加成功并已热重载交易日历')
      setNewHolidayDate(null)
      setNewHolidayDesc('')
      loadCalendar()
    } catch (e) {
      message.error((e as Error).message)
    }
  }

  const handleDeleteHoliday = async (date: string) => {
    try {
      await api.deleteCalendarHoliday(date)
      message.success(`休市日 ${date} 已删除并恢复`)
      loadCalendar()
    } catch (e) {
      message.error((e as Error).message)
    }
  }

  /* ---------------- 3. 系统公告状态 ---------------- */
  const [noticeMessage, setNoticeMessage] = useState('')
  const [noticeType, setNoticeType] = useState<'info' | 'success' | 'warning' | 'error'>('info')
  const [noticeEnabled, setNoticeEnabled] = useState(true)
  const [noticeClosable, setNoticeClosable] = useState(true)
  const [savingNotice, setSavingNotice] = useState(false)

  const loadNotice = async () => {
    try {
      const n = await api.getAdminNotice()
      if (n) {
        setNoticeMessage(n.message || '')
        setNoticeType(n.type || 'info')
        setNoticeClosable(n.closable ?? true)
        setNoticeEnabled(n.custom ?? true)
      }
    } catch (e) {
      message.error((e as Error).message)
    }
  }

  const handleSaveNotice = async () => {
    setSavingNotice(true)
    try {
      await api.setAdminNotice({
        message: noticeMessage,
        type: noticeType,
        enabled: noticeEnabled,
        closable: noticeClosable,
      })
      message.success('系统公告已更新并即时发布生效')
    } catch (e) {
      message.error((e as Error).message)
    } finally {
      setSavingNotice(false)
    }
  }

  /* ---------------- 4. 运维大盘状态 ---------------- */
  const [stats, setStats] = useState<SystemStats | null>(null)
  const [loadingStats, setLoadingStats] = useState(false)
  const [triggeringValuation, setTriggeringValuation] = useState(false)
  const [refreshingNav, setRefreshingNav] = useState(false)

  // 债券久期维护表单
  const [durationFundCode, setDurationFundCode] = useState('')
  const [durationReportQt, setDurationReportQt] = useState('2026-06-30')
  const [durationVal, setDurationVal] = useState<number>(2.5)

  const loadStats = async () => {
    setLoadingStats(true)
    try {
      const s = await api.getSystemStats()
      setStats(s)
    } catch (e) {
      message.error((e as Error).message)
    } finally {
      setLoadingStats(false)
    }
  }

  const handleTriggerIntraday = async () => {
    setTriggeringValuation(true)
    try {
      await api.triggerIntraday(true)
      message.success('实时估值任务已成功触发执行并推送最新快照')
      loadStats()
    } catch (e) {
      message.error((e as Error).message)
    } finally {
      setTriggeringValuation(false)
    }
  }

  const handleRefreshNav = async () => {
    setRefreshingNav(true)
    try {
      const res = await api.refreshNav()
      message.success(`官方净值全量自检完成，共更新 ${res.updatedFunds} 只基金净值`)
      loadStats()
    } catch (e) {
      message.error((e as Error).message)
    } finally {
      setRefreshingNav(false)
    }
  }

  const handleSetBondDuration = async () => {
    if (!durationFundCode.trim()) {
      message.warning('请输入基金代码')
      return
    }
    try {
      await api.setBondDuration(durationFundCode.trim(), durationReportQt.trim(), durationVal)
      message.success(`基金 ${durationFundCode} 久期已人工覆盖为 ${durationVal} 年`)
      setDurationFundCode('')
    } catch (e) {
      message.error((e as Error).message)
    }
  }

  useEffect(() => {
    loadUsers()
    loadCalendar()
    loadNotice()
    loadStats()
  }, [])

  return (
    <div style={{ maxWidth: 1140, margin: '0 auto', padding: '24px 16px 60px' }}>
      {/* 顶部标题与返回 */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
        <Space align="center" size={12}>
          <Button
            type="text"
            icon={<ArrowLeftOutlined />}
            onClick={() => navigate('/')}
            style={{ borderRadius: 8 }}
          >
            返回自选
          </Button>
          <Title level={4} style={{ margin: 0, fontWeight: 700, color: '#0f172a' }}>
            管理控制台
          </Title>
          <Tag color="purple" style={{ borderRadius: 6, fontWeight: 600 }}>ADMIN CONSOLE</Tag>
        </Space>
        <Button
          icon={<ReloadOutlined />}
          onClick={() => {
            loadUsers()
            loadCalendar()
            loadNotice()
            loadStats()
            message.success('已刷新管理数据')
          }}
        >
          刷新数据
        </Button>
      </div>

      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        type="card"
        style={{ marginBottom: 20 }}
        items={[
          {
            key: 'users',
            label: (
              <span>
                <UserOutlined /> 用户治理与自选 ({users.length})
              </span>
            ),
            children: (
              <Card
                bordered={false}
                style={{
                  borderRadius: 14,
                  boxShadow: '0 1px 3px 0 rgba(15, 23, 42, 0.05)',
                }}
              >
                <Table
                  dataSource={users}
                  rowKey="username"
                  loading={loadingUsers}
                  pagination={{ pageSize: 10 }}
                  columns={[
                    {
                      title: '用户名',
                      dataIndex: 'username',
                      key: 'username',
                      render: (name: string, record: UserView) => (
                        <Space>
                          <Text strong>{name}</Text>
                          {record.isVip && (
                            <Tag color="gold" style={{ borderRadius: 10, fontSize: 11 }}>
                              👑 VIP
                            </Tag>
                          )}
                        </Space>
                      ),
                    },
                    {
                      title: '角色',
                      dataIndex: 'role',
                      key: 'role',
                      render: (role: string) => (
                        <Tag color={role === 'ADMIN' ? 'purple' : 'default'} style={{ borderRadius: 6 }}>
                          {role}
                        </Tag>
                      ),
                    },
                    {
                      title: '账号状态',
                      dataIndex: 'status',
                      key: 'status',
                      render: (status: string) => (
                        <Tag
                          color={status === 'NORMAL' ? 'success' : 'error'}
                          icon={status === 'NORMAL' ? <CheckCircleOutlined /> : <CloseCircleOutlined />}
                          style={{ borderRadius: 6 }}
                        >
                          {status === 'NORMAL' ? '正常授权' : '已停用'}
                        </Tag>
                      ),
                    },
                    {
                      title: '会员级别',
                      key: 'vip',
                      render: (_: unknown, record: UserView) => (
                        record.isVip ? (
                          <Text style={{ color: '#d97706', fontSize: 12 }}>
                            VIP 有效 {record.vipExpireAt ? `至 ${dayjs(record.vipExpireAt).format('YYYY-MM-DD')}` : '(永久)'}
                          </Text>
                        ) : (
                          <Text type="secondary" style={{ fontSize: 12 }}>普通会员</Text>
                        )
                      ),
                    },
                    {
                      title: '自选基金数',
                      dataIndex: 'watchlistCount',
                      key: 'watchlistCount',
                      render: (cnt: number) => <Tag color="blue">{cnt} 只</Tag>,
                    },
                    {
                      title: '注册时间',
                      dataIndex: 'createdAt',
                      key: 'createdAt',
                      render: (t: string) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'),
                    },
                    {
                      title: '操作',
                      key: 'action',
                      render: (_: unknown, record: UserView) => (
                        <Space size={8}>
                          <Button
                            type="link"
                            size="small"
                            icon={<EyeOutlined />}
                            onClick={() => handleViewWatchlist(record)}
                          >
                            调阅自选
                          </Button>
                          <Button
                            type="link"
                            size="small"
                            icon={<CrownOutlined />}
                            style={{ color: '#d97706' }}
                            onClick={() => openVipModal(record)}
                          >
                            VIP 设置
                          </Button>
                          <Popconfirm
                            title={`确定要将用户设为 ${record.role === 'ADMIN' ? 'USER' : 'ADMIN'} 吗？`}
                            onConfirm={() => handleToggleRole(record.username, record.role)}
                          >
                            <Button type="link" size="small">
                              {record.role === 'ADMIN' ? '降为USER' : '设为管理员'}
                            </Button>
                          </Popconfirm>
                          <Popconfirm
                            title={`确定要${record.status === 'NORMAL' ? '停用禁用' : '恢复授权'}该用户吗？`}
                            onConfirm={() => handleToggleStatus(record.username, record.status)}
                          >
                            <Button
                              type="link"
                              size="small"
                              danger={record.status === 'NORMAL'}
                            >
                              {record.status === 'NORMAL' ? '停用账号' : '恢复授权'}
                            </Button>
                          </Popconfirm>
                        </Space>
                      ),
                    },
                  ]}
                />
              </Card>
            ),
          },
          {
            key: 'calendar',
            label: (
              <span>
                <CalendarOutlined /> 交易休市日历 ({customHolidays.length})
              </span>
            ),
            children: (
              <Row gutter={[16, 16]}>
                <Col xs={24} md={10}>
                  <Card title="新增/维护休市日" bordered={false} style={{ borderRadius: 14 }}>
                    <Form layout="vertical">
                      <Form.Item label="休市日期 (YYYY-MM-DD)" required>
                        <DatePicker
                          style={{ width: '100%' }}
                          value={newHolidayDate}
                          onChange={(d) => setNewHolidayDate(d)}
                        />
                      </Form.Item>
                      <Form.Item label="休市原因 / 说明">
                        <Input
                          placeholder="例如: 端午节调休闭市 / 临时交易所检修"
                          value={newHolidayDesc}
                          onChange={(e) => setNewHolidayDesc(e.target.value)}
                        />
                      </Form.Item>
                      <Button
                        type="primary"
                        icon={<PlusOutlined />}
                        block
                        onClick={handleAddHoliday}
                        style={{ borderRadius: 8 }}
                      >
                        添加并立即热生效
                      </Button>
                    </Form>

                    <Divider style={{ margin: '18px 0' }} />
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      注: 元旦(01-01~03)、五一(05-01~05)、国庆(10-01~07)及常规周末属于法定内置休市，无需重复录入。此处维护临时调休与法定长假闭市安排。
                    </Text>
                  </Card>
                </Col>

                <Col xs={24} md={14}>
                  <Card
                    title="已生效的自定义休市日"
                    bordered={false}
                    style={{ borderRadius: 14 }}
                  >
                    <Table
                      dataSource={customHolidays}
                      rowKey="id"
                      loading={loadingCalendar}
                      pagination={{ pageSize: 6 }}
                      columns={[
                        {
                          title: '休市日期',
                          dataIndex: 'holidayDate',
                          key: 'holidayDate',
                          render: (d: string) => <Tag color="volcano" style={{ fontWeight: 600 }}>{d}</Tag>,
                        },
                        {
                          title: '说明',
                          dataIndex: 'description',
                          key: 'description',
                        },
                        {
                          title: '操作',
                          key: 'action',
                          render: (_: unknown, record: CalendarHolidayView) => (
                            <Popconfirm
                              title="确定删除该休市日并恢复为正常交易日？"
                              onConfirm={() => handleDeleteHoliday(record.holidayDate)}
                            >
                              <Button type="link" danger size="small" icon={<DeleteOutlined />}>
                                删除
                              </Button>
                            </Popconfirm>
                          ),
                        },
                      ]}
                    />

                    <Divider style={{ margin: '16px 0' }} />
                    <div style={{ fontSize: 13, marginBottom: 8, fontWeight: 600, color: '#334155' }}>
                      当前所有生效的动态休市日期（共 {allDynamicHolidays.length} 天）:
                    </div>
                    <Space size={[6, 6]} wrap>
                      {allDynamicHolidays.map((d) => (
                        <Tag key={d} color="orange" style={{ borderRadius: 6, margin: 0, fontSize: 12 }}>
                          {d}
                        </Tag>
                      ))}
                    </Space>
                  </Card>
                </Col>
              </Row>
            ),
          },
          {
            key: 'notice',
            label: (
              <span>
                <NotificationOutlined /> 系统通用公告
              </span>
            ),
            children: (
              <Row gutter={[16, 16]}>
                <Col xs={24} md={14}>
                  <Card title="编辑系统通知公告" bordered={false} style={{ borderRadius: 14 }}>
                    <Form layout="vertical">
                      <Form.Item label="公告正文内容" required>
                        <Input.TextArea
                          rows={4}
                          maxLength={500}
                          showCount
                          value={noticeMessage}
                          onChange={(e) => setNoticeMessage(e.target.value)}
                          placeholder="例如: 系统公告：自选列表现已上线收益率正序/逆序快速排序功能！"
                        />
                      </Form.Item>

                      <Row gutter={16}>
                        <Col span={12}>
                          <Form.Item label="通知风格类型">
                            <Select
                              value={noticeType}
                              onChange={(val) => setNoticeType(val)}
                              options={[
                                { label: '信息提示 (info - 蓝色)', value: 'info' },
                                { label: '喜报更新 (success - 绿色)', value: 'success' },
                                { label: '重要警示 (warning - 橙色)', value: 'warning' },
                                { label: '紧急故障 (error - 红色)', value: 'error' },
                              ]}
                            />
                          </Form.Item>
                        </Col>
                        <Col span={6}>
                          <Form.Item label="启用自定义公告">
                            <Switch checked={noticeEnabled} onChange={setNoticeEnabled} />
                          </Form.Item>
                        </Col>
                        <Col span={6}>
                          <Form.Item label="允许用户关闭">
                            <Switch checked={noticeClosable} onChange={setNoticeClosable} />
                          </Form.Item>
                        </Col>
                      </Row>

                      <Button
                        type="primary"
                        loading={savingNotice}
                        onClick={handleSaveNotice}
                        style={{ borderRadius: 8, height: 40 }}
                        block
                      >
                        保存并立即推送生效
                      </Button>
                    </Form>
                  </Card>
                </Col>

                <Col xs={24} md={10}>
                  <Card title="客户端自选顶部渲染预览" bordered={false} style={{ borderRadius: 14 }}>
                    {noticeEnabled ? (
                      <Alert
                        type={noticeType}
                        showIcon
                        icon={<SoundOutlined style={{ color: noticeType === 'success' ? '#16a34a' : '#2563eb' }} />}
                        message={noticeMessage || '（尚未输入公告正文）'}
                        closable={noticeClosable}
                        style={{
                          borderRadius: 10,
                          fontSize: 13,
                        }}
                      />
                    ) : (
                      <Alert
                        type="info"
                        showIcon
                        message="自定义公告已停用，系统将在非交易时段展示默认的快照提示。"
                        style={{ borderRadius: 10 }}
                      />
                    )}
                  </Card>
                </Col>
              </Row>
            ),
          },
          {
            key: 'operations',
            label: (
              <span>
                <DashboardOutlined /> 运维控制大盘
              </span>
            ),
            children: (
              <Space direction="vertical" size={16} style={{ width: '100%' }}>
                {/* 统计指标卡片 */}
                <Row gutter={[16, 16]}>
                  <Col xs={12} sm={8} md={4}>
                    <Card bordered={false} style={{ borderRadius: 12 }} loading={loadingStats}>
                      <Statistic title="总注册用户" value={stats?.totalUsers ?? 0} prefix={<UserOutlined />} />
                    </Card>
                  </Col>
                  <Col xs={12} sm={8} md={4}>
                    <Card bordered={false} style={{ borderRadius: 12 }} loading={loadingStats}>
                      <Statistic
                        title="👑 VIP 会员"
                        value={stats?.vipUsers ?? 0}
                        valueStyle={{ color: '#d97706' }}
                        prefix={<CrownOutlined />}
                      />
                    </Card>
                  </Col>
                  <Col xs={12} sm={8} md={4}>
                    <Card bordered={false} style={{ borderRadius: 12 }} loading={loadingStats}>
                      <Statistic title="总自选基金条数" value={stats?.totalWatchlists ?? 0} />
                    </Card>
                  </Col>
                  <Col xs={12} sm={8} md={4}>
                    <Card bordered={false} style={{ borderRadius: 12 }} loading={loadingStats}>
                      <Statistic title="系统基金库总数" value={stats?.totalFunds ?? 0} />
                    </Card>
                  </Col>
                  <Col xs={12} sm={8} md={4}>
                    <Card bordered={false} style={{ borderRadius: 12 }} loading={loadingStats}>
                      <Statistic title="实时行情缓存" value={stats?.cachedQuotes ?? 0} suffix="只" />
                    </Card>
                  </Col>
                  <Col xs={12} sm={8} md={4}>
                    <Card bordered={false} style={{ borderRadius: 12 }} loading={loadingStats}>
                      <Statistic
                        title="动态休市日期"
                        value={stats?.activeDynamicHolidays ?? 0}
                        suffix="天"
                      />
                    </Card>
                  </Col>
                </Row>

                {/* 运维快捷触发与久期覆盖 */}
                <Row gutter={[16, 16]}>
                  <Col xs={24} md={12}>
                    <Card title="⚡ 核心流水线调度" bordered={false} style={{ borderRadius: 14 }}>
                      <Paragraph type="secondary" style={{ fontSize: 13 }}>
                        上次盘中估值计算时间：<Text code>{stats?.lastValuationRunTime ?? '尚未记录'}</Text>
                      </Paragraph>
                      <Space direction="vertical" style={{ width: '100%' }}>
                        <Button
                          type="primary"
                          icon={<ThunderboltOutlined />}
                          loading={triggeringValuation}
                          onClick={handleTriggerIntraday}
                          block
                          style={{ height: 42, borderRadius: 8 }}
                        >
                          立即强制执行实时盘中估值流水线
                        </Button>
                        <Button
                          icon={<ReloadOutlined />}
                          loading={refreshingNav}
                          onClick={handleRefreshNav}
                          block
                          style={{ height: 42, borderRadius: 8 }}
                        >
                          立即全量拉齐天天基金官方净值
                        </Button>
                      </Space>
                    </Card>
                  </Col>

                  <Col xs={24} md={12}>
                    <Card title="🛠️ 债券基金组合久期人工维护" bordered={false} style={{ borderRadius: 14 }}>
                      <Form layout="vertical">
                        <Row gutter={12}>
                          <Col span={10}>
                            <Form.Item label="基金代码" required>
                              <Input
                                placeholder="如 000001"
                                value={durationFundCode}
                                onChange={(e) => setDurationFundCode(e.target.value)}
                              />
                            </Form.Item>
                          </Col>
                          <Col span={8}>
                            <Form.Item label="报告期">
                              <Input
                                placeholder="2026-06-30"
                                value={durationReportQt}
                                onChange={(e) => setDurationReportQt(e.target.value)}
                              />
                            </Form.Item>
                          </Col>
                          <Col span={6}>
                            <Form.Item label="推断久期(年)">
                              <InputNumber
                                min={0.1}
                                max={30}
                                step={0.1}
                                style={{ width: '100%' }}
                                value={durationVal}
                                onChange={(v) => setDurationVal(v ?? 2.5)}
                              />
                            </Form.Item>
                          </Col>
                        </Row>
                        <Button
                          type="default"
                          onClick={handleSetBondDuration}
                          block
                          style={{ borderRadius: 8 }}
                        >
                          覆盖该债基组合久期
                        </Button>
                      </Form>
                    </Card>
                  </Col>
                </Row>
              </Space>
            ),
          },
        ]}
      />

      {/* 调阅用户自选列表弹窗 */}
      <Modal
        title={`调阅用户【${selectedUser?.username}】的自选基金估值`}
        open={watchlistModalOpen}
        onCancel={() => setWatchlistModalOpen(false)}
        footer={null}
        width={750}
      >
        <Table
          dataSource={userWatchlist}
          rowKey="fundCode"
          loading={loadingWatchlist}
          pagination={false}
          columns={[
            {
              title: '代码',
              dataIndex: 'fundCode',
              key: 'fundCode',
              render: (code: string) => <Text code>{code}</Text>,
            },
            {
              title: '基金名称',
              dataIndex: 'fundName',
              key: 'fundName',
            },
            {
              title: '类型',
              dataIndex: 'fundType',
              key: 'fundType',
              render: (t: string) => <Tag color="blue">{t}</Tag>,
            },
            {
              title: '实时估值',
              dataIndex: 'estimateNav',
              key: 'estimateNav',
              render: (nav: number | null) => fmtNav(nav),
            },
            {
              title: '估算涨跌',
              dataIndex: 'estimatePct',
              key: 'estimatePct',
              render: (pct: number | null) => (
                <Text style={{ color: colorOf(pct), fontWeight: 600 }}>
                  {fmtPct(pct)}
                </Text>
              ),
            },
          ]}
        />
      </Modal>

      {/* VIP 资格设置弹窗 */}
      <Modal
        title={`设置用户【${vipUser?.username}】的 VIP 会员资格`}
        open={vipModalOpen}
        onCancel={() => setVipModalOpen(false)}
        onOk={handleSaveVip}
        okText="保存 VIP 设置"
      >
        <Form layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item label="开通 VIP 会员权限">
            <Switch
              checked={vipEnabled}
              onChange={setVipEnabled}
              checkedChildren="已开通"
              unCheckedChildren="普通会员"
            />
          </Form.Item>
          {vipEnabled && (
            <Form.Item label="VIP 到期时间 (留空为永久有效)">
              <DatePicker
                showTime
                style={{ width: '100%' }}
                value={vipExpireDate}
                onChange={(d) => setVipExpireDate(d)}
                placeholder="留空即为永久有效 VIP"
              />
            </Form.Item>
          )}
        </Form>
      </Modal>
    </div>
  )
}
