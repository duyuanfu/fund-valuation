import { useEffect, useState, useMemo } from 'react'
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
  Upload,
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
  SoundOutlined,
  FireOutlined,
  FundOutlined,
  SearchOutlined,
  ClockCircleOutlined,
  TeamOutlined,
  ClearOutlined,
  UploadOutlined,
  WechatOutlined,
  AlipayCircleOutlined,
} from '@ant-design/icons'
import { api } from '../api'
import type {
  UserView,
  CalendarHolidayView,
  SystemStats,
  EstimateResult,
  FundView,
} from '../api/types'
import { FUND_TYPE_COLOR, FUND_TYPE_LABEL } from '../api/types'
import { colorOf, fmtNav, fmtPct } from '../utils/format'
import dayjs from 'dayjs'

const { Title, Text, Paragraph } = Typography

export default function AdminPage() {
  const [activeTab, setActiveTab] = useState('users')

  /* ---------------- 1. 用户管理状态 ---------------- */
  const [users, setUsers] = useState<UserView[]>([])
  const [loadingUsers, setLoadingUsers] = useState(false)
  const pendingCount = users.filter((u) => u.status === 'PENDING').length
  const [selectedUser, setSelectedUser] = useState<UserView | null>(null)
  const [userWatchlist, setUserWatchlist] = useState<EstimateResult[]>([])
  const [loadingWatchlist, setLoadingWatchlist] = useState(false)
  const [watchlistModalOpen, setWatchlistModalOpen] = useState(false)

  // 用户搜索与筛选
  const [userKeyword, setUserKeyword] = useState('')
  const [userStatusFilter, setUserStatusFilter] = useState('ALL')
  const [userVipFilter, setUserVipFilter] = useState('ALL')
  const [userRoleFilter, setUserRoleFilter] = useState('ALL')

  // VIP 设置弹窗
  const [vipModalOpen, setVipModalOpen] = useState(false)
  const [vipUser, setVipUser] = useState<UserView | null>(null)
  const [vipEnabled, setVipEnabled] = useState(false)
  const [vipExpireDate, setVipExpireDate] = useState<dayjs.Dayjs | null>(null)

  const loadUsers = async (kw?: string) => {
    setLoadingUsers(true)
    try {
      const list = await api.listUsers(kw !== undefined ? kw : (userKeyword.trim() || undefined))
      setUsers(list)
    } catch (e) {
      message.error((e as Error).message)
    } finally {
      setLoadingUsers(false)
    }
  }

  // 客户端即时联动过滤
  const filteredUsers = useMemo(() => {
    return users.filter((u) => {
      if (userKeyword.trim()) {
        const kw = userKeyword.trim().toLowerCase()
        const matchName = u.username.toLowerCase().includes(kw)
        const matchSource = u.referralSource && u.referralSource.toLowerCase().includes(kw)
        const matchRole = u.role.toLowerCase().includes(kw)
        const matchStatus = u.status.toLowerCase().includes(kw)
        if (!matchName && !matchSource && !matchRole && !matchStatus) {
          return false
        }
      }
      if (userStatusFilter !== 'ALL' && u.status !== userStatusFilter) {
        return false
      }
      if (userVipFilter === 'VIP' && !u.isVip) {
        return false
      }
      if (userVipFilter === 'NORMAL' && u.isVip) {
        return false
      }
      if (userRoleFilter !== 'ALL' && u.role !== userRoleFilter) {
        return false
      }
      return true
    })
  }, [users, userKeyword, userStatusFilter, userVipFilter, userRoleFilter])

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

  const handleDeleteUser = async (username: string) => {
    try {
      await api.deleteUser(username)
      message.success(`用户 ${username} 及其自选关联已永久删除`)
      loadUsers()
    } catch (e) {
      message.error((e as Error).message)
    }
  }

  /* ---------------- 2. 监控基金库状态 ---------------- */
  const [funds, setFunds] = useState<FundView[]>([])
  const [loadingFunds, setLoadingFunds] = useState(false)
  const [fundSearch, setFundSearch] = useState('')

  const loadFunds = async () => {
    setLoadingFunds(true)
    try {
      const list = await api.listFunds()
      setFunds(list)
    } catch (e) {
      message.error((e as Error).message)
    } finally {
      setLoadingFunds(false)
    }
  }

  // 监控基金被关注用户列表弹窗
  const [watchersModalOpen, setWatchersModalOpen] = useState(false)
  const [selectedFundForWatchers, setSelectedFundForWatchers] = useState<FundView | null>(null)
  const [fundWatchers, setFundWatchers] = useState<UserView[]>([])
  const [loadingWatchers, setLoadingWatchers] = useState(false)

  const openFundWatchersModal = async (f: FundView) => {
    setSelectedFundForWatchers(f)
    setWatchersModalOpen(true)
    setLoadingWatchers(true)
    try {
      const list = await api.getFundWatchers(f.code)
      setFundWatchers(list)
    } catch (e) {
      message.error((e as Error).message)
    } finally {
      setLoadingWatchers(false)
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

  /* ---------------- 6. VIP会员价格与收款码配置 ---------------- */
  const [vipConfigForm] = Form.useForm()
  const [savingVipConfig, setSavingVipConfig] = useState(false)
  const [wechatQrPreview, setWechatQrPreview] = useState<string>('')
  const [alipayQrPreview, setAlipayQrPreview] = useState<string>('')

  const loadVipConfig = async () => {
    try {
      const cfg = await api.getAdminVipConfig()
      if (cfg) {
        vipConfigForm.setFieldsValue({
          monthlyPrice: cfg.monthlyPrice,
          quarterlyPrice: cfg.quarterlyPrice,
          quarterlyOrigPrice: cfg.quarterlyOrigPrice,
          yearlyPrice: cfg.yearlyPrice,
          yearlyOrigPrice: cfg.yearlyOrigPrice,
          wechatQrUrl: cfg.wechatQrUrl,
          alipayQrUrl: cfg.alipayQrUrl,
          payeeName: cfg.payeeName,
          paymentTip: cfg.paymentTip,
        })
        setWechatQrPreview(cfg.wechatQrUrl || '/wechat-pay.png')
        setAlipayQrPreview(cfg.alipayQrUrl || '/alipay.jpg')
      }
    } catch (e) {
      // 容错静默
    }
  }

  const handleSaveVipConfig = async () => {
    try {
      const vals = await vipConfigForm.validateFields()
      setSavingVipConfig(true)
      await api.setAdminVipConfig({
        ...vals,
        wechatQrUrl: wechatQrPreview,
        alipayQrUrl: alipayQrPreview,
      })
      message.success('VIP会员价格与收款码配置已成功保存并立即生效')
    } catch (e: any) {
      message.error(e?.error || e?.message || '保存失败')
    } finally {
      setSavingVipConfig(false)
    }
  }

  const handleQrUpload = (file: File, type: 'wechat' | 'alipay') => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const res = e.target?.result as string
      if (type === 'wechat') {
        setWechatQrPreview(res)
        vipConfigForm.setFieldValue('wechatQrUrl', res)
      } else {
        setAlipayQrPreview(res)
        vipConfigForm.setFieldValue('alipayQrUrl', res)
      }
      message.success(`${type === 'wechat' ? '微信' : '支付宝'}收款码图片已成功加载`)
    }
    reader.readAsDataURL(file)
    return false
  }

  /* ---------------- 7. 新用户注册审批开关 ---------------- */
  const [requireApproval, setRequireApproval] = useState<boolean>(true)
  const [loadingApproval, setLoadingApproval] = useState<boolean>(false)

  const loadRegistrationApproval = async () => {
    try {
      const res = await api.getRegistrationApproval()
      if (res && res.requireApproval !== undefined) {
        setRequireApproval(res.requireApproval)
      }
    } catch {
      // 容错
    }
  }

  const handleToggleApproval = async (checked: boolean) => {
    setLoadingApproval(true)
    try {
      await api.setRegistrationApproval(checked)
      setRequireApproval(checked)
      message.success(
        checked
          ? '已开启注册审核：新用户提交注册后需管理员手动审核通过方可登录'
          : '已关闭注册审核：新用户注册后无需审批，直接生成账号并自动登录！'
      )
    } catch (e: any) {
      message.error(e?.error || e?.message || '设置失败')
    } finally {
      setLoadingApproval(false)
    }
  }

  useEffect(() => {
    let cancelled = false
    const init = async () => {
      try {
        const [u, f, c, n, s] = await Promise.allSettled([
          api.listUsers(),
          api.listFunds(),
          api.getCalendarHolidays(),
          api.getAdminNotice(),
          api.getSystemStats(),
        ])
        if (cancelled) return
        if (u.status === 'fulfilled') setUsers(u.value)
        if (f.status === 'fulfilled') setFunds(f.value)
        if (c.status === 'fulfilled') {
          setCustomHolidays(c.value.customHolidays || [])
          setAllDynamicHolidays(c.value.allDynamicHolidays || [])
        }
        if (n.status === 'fulfilled' && n.value) {
          setNoticeMessage(n.value.message || '')
          setNoticeType(n.value.type || 'info')
          setNoticeClosable(n.value.closable ?? true)
          setNoticeEnabled(n.value.custom ?? true)
        }
        if (s.status === 'fulfilled') setStats(s.value)
        loadVipConfig()
        loadRegistrationApproval()
      } catch (e) {
        message.error((e as Error).message)
      }
    }
    init()
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div style={{ width: '100%', maxWidth: 1600, margin: '0 auto', padding: '16px 24px 60px' }}>
      {/* 顶部标题栏 */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
        <Space align="center" size={10}>
          <div
            style={{
              width: 32,
              height: 32,
              borderRadius: 8,
              background: 'linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 2px 6px rgba(124, 58, 237, 0.3)',
            }}
          >
            <DashboardOutlined style={{ color: '#ffffff', fontSize: 17 }} />
          </div>
          <Title level={4} style={{ margin: 0, fontWeight: 700, color: '#0f172a', letterSpacing: -0.2 }}>
            管理控制台
          </Title>
          <Tag color="purple" style={{ borderRadius: 6, fontWeight: 600, margin: 0 }}>
            ADMIN CONSOLE
          </Tag>
        </Space>
        <Button
          icon={<ReloadOutlined />}
          onClick={() => {
            loadUsers()
            loadFunds()
            loadCalendar()
            loadNotice()
            loadStats()
            message.success('已刷新管理数据')
          }}
          style={{ borderRadius: 8 }}
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
                <UserOutlined /> 用户治理与自选 ({users.length}人{pendingCount > 0 ? ` · ${pendingCount}待审` : ''})
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
                {pendingCount > 0 && (
                  <Alert
                    type="warning"
                    showIcon
                    icon={<ClockCircleOutlined />}
                    message={`新用户审批提醒：当前有 ${pendingCount} 位新用户提交了注册申请正在等待授权，请在操作列点击【通过授权】即可允许其登录。`}
                    style={{ marginBottom: 14, borderRadius: 10 }}
                  />
                )}

                {/* 新用户注册审批流程开关栏 */}
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '10px 14px',
                    background: requireApproval ? '#fffdf7' : '#f0fdf4',
                    border: `1px solid ${requireApproval ? '#fde68a' : '#bbf7d0'}`,
                    borderRadius: 10,
                    marginBottom: 14,
                    flexWrap: 'wrap',
                    gap: 10,
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <span style={{ fontSize: 13, fontWeight: 700, color: '#0f172a' }}>
                      新用户注册审批流程：
                    </span>
                    <Switch
                      checked={requireApproval}
                      loading={loadingApproval}
                      onChange={handleToggleApproval}
                      checkedChildren="开启审核"
                      unCheckedChildren="直接放行"
                    />
                    <Tag color={requireApproval ? 'warning' : 'success'} style={{ margin: 0, fontWeight: 600 }}>
                      {requireApproval ? '开启中 (注册后需管理员手动授权通过)' : '已关闭 (免审直接注册成功并自动登录)'}
                    </Tag>
                  </div>
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    {requireApproval
                      ? '开启时：新用户注册后账号为待授权状态，需在下方列表点击【通过授权】'
                      : '关闭时：已开放自由注册，用户提交后直接生成正常账号并自动登录系统'}
                  </Text>
                </div>

                {/* 用户搜索与多维状态筛选工具栏 */}
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    marginBottom: 16,
                    flexWrap: 'wrap',
                    gap: 12,
                  }}
                >
                  <Space wrap size={10}>
                    <Input
                      prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
                      placeholder="按用户名 / 获客渠道 / 角色搜索..."
                      allowClear
                      value={userKeyword}
                      onChange={(e) => setUserKeyword(e.target.value)}
                      onPressEnter={() => loadUsers(userKeyword)}
                      style={{ width: 250, borderRadius: 8 }}
                    />
                    <Select
                      value={userStatusFilter}
                      onChange={(val) => setUserStatusFilter(val)}
                      style={{ width: 120 }}
                      options={[
                        { label: '全部状态', value: 'ALL' },
                        { label: '待审核', value: 'PENDING' },
                        { label: '正常授权', value: 'NORMAL' },
                        { label: '已停用', value: 'DISABLED' },
                      ]}
                    />
                    <Select
                      value={userVipFilter}
                      onChange={(val) => setUserVipFilter(val)}
                      style={{ width: 120 }}
                      options={[
                        { label: '全部级别', value: 'ALL' },
                        { label: 'VIP 会员', value: 'VIP' },
                        { label: '普通会员', value: 'NORMAL' },
                      ]}
                    />
                    <Select
                      value={userRoleFilter}
                      onChange={(val) => setUserRoleFilter(val)}
                      style={{ width: 120 }}
                      options={[
                        { label: '全部角色', value: 'ALL' },
                        { label: '普通用户 (USER)', value: 'USER' },
                        { label: '管理员 (ADMIN)', value: 'ADMIN' },
                      ]}
                    />
                    {(userKeyword || userStatusFilter !== 'ALL' || userVipFilter !== 'ALL' || userRoleFilter !== 'ALL') && (
                      <Button
                        icon={<ClearOutlined />}
                        onClick={() => {
                          setUserKeyword('')
                          setUserStatusFilter('ALL')
                          setUserVipFilter('ALL')
                          setUserRoleFilter('ALL')
                          loadUsers('')
                        }}
                        style={{ borderRadius: 8 }}
                      >
                        重置筛选
                      </Button>
                    )}
                  </Space>

                  <Space size={10}>
                    <Text type="secondary" style={{ fontSize: 13 }}>
                      共筛选出 <Text strong style={{ color: '#2563eb' }}>{filteredUsers.length}</Text> 位用户 (总注册 {users.length} 人)
                    </Text>
                    <Button
                      size="small"
                      icon={<ReloadOutlined />}
                      onClick={() => loadUsers()}
                      style={{ borderRadius: 6 }}
                    >
                      刷新
                    </Button>
                  </Space>
                </div>

                <Table
                  dataSource={filteredUsers}
                  rowKey="username"
                  loading={loadingUsers}
                  pagination={{ pageSize: 10 }}
                  scroll={{ x: 'max-content' }}
                  columns={[
                    {
                      title: '用户名',
                      dataIndex: 'username',
                      key: 'username',
                      width: 120,
                      render: (name: string) => <Text strong style={{ color: '#0f172a' }}>{name}</Text>,
                    },
                    {
                      title: '角色',
                      dataIndex: 'role',
                      key: 'role',
                      width: 85,
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
                      width: 115,
                      render: (status: string) => {
                        if (status === 'PENDING') {
                          return (
                            <Tag color="orange" icon={<ClockCircleOutlined />} style={{ borderRadius: 6, fontWeight: 500 }}>
                              待授权审核
                            </Tag>
                          )
                        }
                        return (
                          <Tag
                            color={status === 'NORMAL' ? 'success' : 'error'}
                            icon={status === 'NORMAL' ? <CheckCircleOutlined /> : <CloseCircleOutlined />}
                            style={{ borderRadius: 6 }}
                          >
                            {status === 'NORMAL' ? '正常授权' : '已停用'}
                          </Tag>
                        )
                      },
                    },
                    {
                      title: '会员级别',
                      key: 'vip',
                      width: 135,
                      render: (_: unknown, record: UserView) => (
                        record.isVip ? (
                          <Text style={{ color: '#d97706', fontSize: 12, whiteSpace: 'nowrap' }}>
                            VIP {record.vipExpireAt ? `至 ${dayjs(record.vipExpireAt).format('YYYY-MM-DD')}` : '(永久)'}
                          </Text>
                        ) : (
                          <Text type="secondary" style={{ fontSize: 12 }}>普通会员</Text>
                        )
                      ),
                    },
                    {
                      title: '了解来源',
                      dataIndex: 'referralSource',
                      key: 'referralSource',
                      width: 105,
                      render: (src: string) => {
                        const source = src || '自己搜索'
                        let tagColor = 'default'
                        if (source.includes('闲鱼')) tagColor = 'orange'
                        else if (source.includes('B站')) tagColor = 'magenta'
                        else if (source.includes('朋友')) tagColor = 'green'
                        else if (source.includes('搜索')) tagColor = 'blue'
                        return <Tag color={tagColor} style={{ borderRadius: 6, margin: 0 }}>{source}</Tag>
                      },
                    },
                    {
                      title: '自选基金数',
                      dataIndex: 'watchlistCount',
                      key: 'watchlistCount',
                      width: 95,
                      render: (cnt: number, record: UserView) =>
                        record.role === 'ADMIN' ? (
                          <Text type="secondary" style={{ fontSize: 12 }}>-</Text>
                        ) : (
                          <Tag color="blue">{cnt} 只</Tag>
                        ),
                    },
                    {
                      title: '最后登录 / 活跃度',
                      key: 'loginActivity',
                      width: 185,
                      render: (_: unknown, record: UserView) => {
                        if (!record.lastLoginAt) {
                          return <Tag style={{ borderRadius: 6 }}>从未登录</Tag>
                        }
                        const last = dayjs(record.lastLoginAt)
                        const diffHours = dayjs().diff(last, 'hour')
                        const diffDays = dayjs().diff(last, 'day')
                        let tagColor = 'default'
                        let tagText = `${diffDays}天前`
                        if (diffHours < 1) {
                          tagColor = 'success'
                          tagText = '刚刚活跃'
                        } else if (diffHours < 24) {
                          tagColor = 'green'
                          tagText = '今日活跃'
                        } else if (diffDays <= 7) {
                          tagColor = 'blue'
                          tagText = `${diffDays}天内活跃`
                        }
                        return (
                          <div>
                            <div style={{ fontSize: 12, fontWeight: 500, color: '#334155', whiteSpace: 'nowrap' }}>
                              {last.format('YYYY-MM-DD HH:mm')}
                            </div>
                            <Space size={4} style={{ marginTop: 2, whiteSpace: 'nowrap' }}>
                              <Tag color={tagColor} style={{ borderRadius: 6, margin: 0, fontSize: 10 }}>
                                {tagText}
                              </Tag>
                              <span style={{ fontSize: 11, color: '#94a3b8' }}>
                                累计 {record.loginCount ?? 0} 次
                              </span>
                            </Space>
                          </div>
                        )
                      },
                    },
                    {
                      title: '注册时间',
                      dataIndex: 'createdAt',
                      key: 'createdAt',
                      width: 140,
                      render: (t: string) => (t ? <span style={{ whiteSpace: 'nowrap' }}>{dayjs(t).format('YYYY-MM-DD HH:mm')}</span> : '-'),
                    },
                    {
                      title: '操作',
                      key: 'action',
                      fixed: 'right',
                      width: 290,
                      render: (_: unknown, record: UserView) => (
                        <Space size={4} wrap={false}>
                          {record.status === 'PENDING' ? (
                            <Popconfirm
                              title={`确定通过用户【${record.username}】的账号申请并授权登录吗？`}
                              onConfirm={() => handleToggleStatus(record.username, 'PENDING')}
                              okText="通过授权"
                              cancelText="取消"
                            >
                              <Button
                                type="primary"
                                size="small"
                                icon={<CheckCircleOutlined />}
                                style={{
                                  background: 'linear-gradient(135deg, #16a34a 0%, #15803d 100%)',
                                  borderColor: '#15803d',
                                  padding: '0 8px',
                                  fontSize: 12,
                                  whiteSpace: 'nowrap',
                                }}
                              >
                                通过授权
                              </Button>
                            </Popconfirm>
                          ) : (
                            <>
                              {record.role !== 'ADMIN' && (
                                <Button
                                  type="link"
                                  size="small"
                                  icon={<EyeOutlined />}
                                  onClick={() => handleViewWatchlist(record)}
                                  style={{ padding: '0 4px', whiteSpace: 'nowrap' }}
                                >
                                  调阅自选
                                </Button>
                              )}
                              <Button
                                type="link"
                                size="small"
                                icon={<CrownOutlined />}
                                style={{ color: '#d97706', padding: '0 4px', whiteSpace: 'nowrap' }}
                                onClick={() => openVipModal(record)}
                              >
                                VIP 设置
                              </Button>
                              <Popconfirm
                                title={`确定要将用户设为 ${record.role === 'ADMIN' ? 'USER' : 'ADMIN'} 吗？`}
                                onConfirm={() => handleToggleRole(record.username, record.role)}
                              >
                                <Button type="link" size="small" style={{ padding: '0 4px', whiteSpace: 'nowrap' }}>
                                  {record.role === 'ADMIN' ? '降为用户' : '设为管理员'}
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
                                  style={{ padding: '0 4px', whiteSpace: 'nowrap' }}
                                >
                                  {record.status === 'NORMAL' ? '停用账号' : '恢复授权'}
                                </Button>
                              </Popconfirm>
                            </>
                          )}
                          {record.role !== 'ADMIN' && (
                            <Popconfirm
                              title={`确定要彻底删除用户【${record.username}】吗？`}
                              description="此操作将物理删除该账号及其所有自选关联，且不可恢复！"
                              onConfirm={() => handleDeleteUser(record.username)}
                              okText="删除"
                              cancelText="取消"
                              okButtonProps={{ danger: true }}
                            >
                              <Button
                                type="link"
                                size="small"
                                danger
                                icon={<DeleteOutlined />}
                                style={{ padding: '0 4px', whiteSpace: 'nowrap' }}
                              >
                                删除
                              </Button>
                            </Popconfirm>
                          )}
                        </Space>
                      ),
                    },
                  ]}
                />
              </Card>
            ),
          },
          {
            key: 'funds',
            label: (
              <span>
                <FundOutlined /> 监控基金总览 ({funds.length})
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
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16, flexWrap: 'wrap', gap: 12 }}>
                  <Input
                    prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
                    placeholder="按基金代码或基金名称快速筛选搜索..."
                    allowClear
                    value={fundSearch}
                    onChange={(e) => setFundSearch(e.target.value)}
                    style={{ maxWidth: 360, borderRadius: 8 }}
                  />
                  <Text type="secondary" style={{ fontSize: 13 }}>
                    系统当前共收录监控 <Text strong style={{ color: '#2563eb' }}>{funds.length}</Text> 只基金
                  </Text>
                </div>

                <Table
                  dataSource={funds.filter(
                    (f) =>
                      f.code.toLowerCase().includes(fundSearch.toLowerCase()) ||
                      f.name.toLowerCase().includes(fundSearch.toLowerCase())
                  )}
                  rowKey="code"
                  loading={loadingFunds}
                  pagination={{ pageSize: 10 }}
                  scroll={{ x: 1000 }}
                  columns={[
                    {
                      title: '基金代码',
                      dataIndex: 'code',
                      key: 'code',
                      width: 100,
                      render: (c: string) => <Text code style={{ fontWeight: 600 }}>{c}</Text>,
                    },
                    {
                      title: '基金名称',
                      dataIndex: 'name',
                      key: 'name',
                      width: 220,
                      render: (name: string) => <Text strong style={{ color: '#0f172a' }}>{name}</Text>,
                    },
                    {
                      title: '类型',
                      dataIndex: 'type',
                      key: 'type',
                      width: 110,
                      render: (t: string) => (
                        <Tag color={FUND_TYPE_COLOR[t] ?? 'default'} style={{ borderRadius: 6 }}>
                          {FUND_TYPE_LABEL[t] ?? t}
                        </Tag>
                      ),
                    },
                    {
                      title: '细分类型原文',
                      dataIndex: 'typeRaw',
                      key: 'typeRaw',
                      width: 140,
                      render: (tr: string) => (tr ? <span style={{ fontSize: 12, color: '#64748b' }}>{tr}</span> : '-'),
                    },
                    {
                      title: '跟踪标的指数',
                      dataIndex: 'trackIndex',
                      key: 'trackIndex',
                      width: 120,
                      render: (idx: string) => (idx ? <Text code style={{ fontSize: 11 }}>{idx}</Text> : '-'),
                    },
                    {
                      title: '昨日官方净值',
                      dataIndex: 'prevNav',
                      key: 'prevNav',
                      width: 110,
                      render: (nav: number | null) => fmtNav(nav),
                    },
                    {
                      title: '净值公布日期',
                      dataIndex: 'navDate',
                      key: 'navDate',
                      width: 120,
                      render: (d: string) => <span style={{ fontSize: 12 }}>{d}</span>,
                    },
                    {
                      title: '用户关注量',
                      dataIndex: 'userCount',
                      key: 'userCount',
                      width: 140,
                      render: (cnt: number, record: FundView) =>
                        cnt > 0 ? (
                          <Button
                            type="link"
                            size="small"
                            icon={<TeamOutlined />}
                            onClick={() => openFundWatchersModal(record)}
                            style={{ padding: 0, fontWeight: 550 }}
                          >
                            {cnt} 人自选 (查看列表)
                          </Button>
                        ) : (
                          <Tag style={{ borderRadius: 6, color: '#94a3b8' }}>0 人自选</Tag>
                        ),
                    },
                    {
                      title: '操作',
                      key: 'action',
                      fixed: 'right',
                      width: 120,
                      render: (_: unknown, record: FundView) => (
                        <Button
                          size="small"
                          type="primary"
                          ghost
                          icon={<EyeOutlined />}
                          disabled={record.userCount === 0}
                          onClick={() => openFundWatchersModal(record)}
                          style={{ borderRadius: 6 }}
                        >
                          关注用户
                        </Button>
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
            key: 'vipConfig',
            label: (
              <span>
                <CrownOutlined style={{ color: '#d97706' }} /> VIP与收款设置
              </span>
            ),
            children: (
              <Row gutter={[20, 20]}>
                {/* 左侧：价格与收款码设置表单 */}
                <Col xs={24} lg={14}>
                  <Card
                    title={
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <CrownOutlined style={{ color: '#d97706' }} />
                        <span style={{ fontWeight: 650 }}>VIP 套餐价格与收款设置</span>
                      </div>
                    }
                    bordered={false}
                    style={{ borderRadius: 14, boxShadow: '0 1px 3px 0 rgba(15, 23, 42, 0.05)' }}
                  >
                    <Alert
                      type="info"
                      showIcon
                      message="在此处配置 VIP 会员的价格阶梯、收款二维码及付款备注说明。配置保存后，前台持仓估值页的“开通 VIP”弹窗将实时同步展示。"
                      style={{ marginBottom: 20, borderRadius: 10 }}
                    />

                    <Form form={vipConfigForm} layout="vertical" onFinish={handleSaveVipConfig}>
                      <Title level={5} style={{ fontSize: 14, marginBottom: 12, color: '#334155' }}>
                        1. 会员套餐价格设置 (元)
                      </Title>
                      <Row gutter={16}>
                        <Col xs={24} sm={8}>
                          <Form.Item
                            name="monthlyPrice"
                            label="月度会员价格"
                            rules={[{ required: true, message: '请输入月度会员价格' }]}
                          >
                            <InputNumber style={{ width: '100%' }} precision={2} min={0.01} prefix="¥" />
                          </Form.Item>
                        </Col>
                        <Col xs={24} sm={8}>
                          <Form.Item
                            name="quarterlyPrice"
                            label="季度会员价格"
                            rules={[{ required: true, message: '请输入季度会员价格' }]}
                          >
                            <InputNumber style={{ width: '100%' }} precision={2} min={0.01} prefix="¥" />
                          </Form.Item>
                        </Col>
                        <Col xs={24} sm={8}>
                          <Form.Item name="quarterlyOrigPrice" label="季度原价 (划线展示)">
                            <InputNumber style={{ width: '100%' }} precision={2} min={0.01} prefix="¥" />
                          </Form.Item>
                        </Col>
                      </Row>

                      <Row gutter={16}>
                        <Col xs={24} sm={12}>
                          <Form.Item
                            name="yearlyPrice"
                            label="年度会员价格"
                            rules={[{ required: true, message: '请输入年度会员价格' }]}
                          >
                            <InputNumber style={{ width: '100%' }} precision={2} min={0.01} prefix="¥" />
                          </Form.Item>
                        </Col>
                        <Col xs={24} sm={12}>
                          <Form.Item name="yearlyOrigPrice" label="年度原价 (划线展示)">
                            <InputNumber style={{ width: '100%' }} precision={2} min={0.01} prefix="¥" />
                          </Form.Item>
                        </Col>
                      </Row>

                      <Divider style={{ margin: '12px 0 18px' }} />

                      <Title level={5} style={{ fontSize: 14, marginBottom: 12, color: '#334155' }}>
                        2. 收款人与备注指引
                      </Title>
                      <Row gutter={16}>
                        <Col xs={24} sm={12}>
                          <Form.Item
                            name="payeeName"
                            label="收款人显示名称"
                            rules={[{ required: true, message: '请输入收款人名称' }]}
                          >
                            <Input placeholder="例如: 管理员 / 客服小助手" />
                          </Form.Item>
                        </Col>
                        <Col xs={24} sm={12}>
                          <Form.Item
                            name="paymentTip"
                            label="付款备注提示"
                            rules={[{ required: true, message: '请输入备注提示' }]}
                          >
                            <Input placeholder="例如: 付款请务必备注用户名" />
                          </Form.Item>
                        </Col>
                      </Row>

                      <Divider style={{ margin: '12px 0 18px' }} />

                      <Title level={5} style={{ fontSize: 14, marginBottom: 12, color: '#334155' }}>
                        3. 收款二维码设置 (微信 / 支付宝)
                      </Title>
                      <Row gutter={16}>
                        <Col xs={24} sm={12}>
                          <Form.Item label="微信收款码" extra="支持直接选择本地图片或填写图片链接">
                            <Space direction="vertical" style={{ width: '100%' }}>
                              <Input
                                placeholder="图片URL或直接上传图片"
                                value={wechatQrPreview}
                                onChange={(e) => {
                                  setWechatQrPreview(e.target.value)
                                  vipConfigForm.setFieldValue('wechatQrUrl', e.target.value)
                                }}
                              />
                              <Upload
                                showUploadList={false}
                                beforeUpload={(file) => handleQrUpload(file, 'wechat')}
                                accept="image/*"
                              >
                                <Button icon={<UploadOutlined />}>选择本地微信收款码图片</Button>
                              </Upload>
                            </Space>
                          </Form.Item>
                        </Col>

                        <Col xs={24} sm={12}>
                          <Form.Item label="支付宝收款码" extra="支持直接选择本地图片或填写图片链接">
                            <Space direction="vertical" style={{ width: '100%' }}>
                              <Input
                                placeholder="图片URL或直接上传图片"
                                value={alipayQrPreview}
                                onChange={(e) => {
                                  setAlipayQrPreview(e.target.value)
                                  vipConfigForm.setFieldValue('alipayQrUrl', e.target.value)
                                }}
                              />
                              <Upload
                                showUploadList={false}
                                beforeUpload={(file) => handleQrUpload(file, 'alipay')}
                                accept="image/*"
                              >
                                <Button icon={<UploadOutlined />}>选择本地支付宝收款码图片</Button>
                              </Upload>
                            </Space>
                          </Form.Item>
                        </Col>
                      </Row>

                      <Form.Item style={{ marginTop: 16 }}>
                        <Button
                          type="primary"
                          htmlType="submit"
                          size="large"
                          loading={savingVipConfig}
                          style={{
                            background: 'linear-gradient(135deg, #d97706 0%, #b45309 100%)',
                            borderColor: '#b45309',
                            fontWeight: 650,
                            borderRadius: 8,
                            padding: '0 32px',
                          }}
                        >
                          保存配置并生效
                        </Button>
                      </Form.Item>
                    </Form>
                  </Card>
                </Col>

                {/* 右侧：前台效果实时预览卡片 */}
                <Col xs={24} lg={10}>
                  <Card
                    title="前台充值弹窗效果预览"
                    bordered={false}
                    style={{ borderRadius: 14, boxShadow: '0 1px 3px 0 rgba(15, 23, 42, 0.05)' }}
                  >
                    <div style={{ textAlign: 'center', marginBottom: 12 }}>
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        用户点击“开通 VIP”时看到的实时收款卡片：
                      </Text>
                    </div>

                    <div
                      style={{
                        border: '1px solid #e2e8f0',
                        borderRadius: 14,
                        padding: 16,
                        background: '#f8fafc',
                      }}
                    >
                      {/* 套餐标签预览 */}
                      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8, marginBottom: 14 }}>
                        <div style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: 8, padding: '8px 4px', textAlign: 'center' }}>
                          <div style={{ fontSize: 11, color: '#64748b' }}>月卡</div>
                          <div style={{ fontSize: 16, fontWeight: 700, color: '#b45309' }}>
                            ¥{vipConfigForm.getFieldValue('monthlyPrice') ?? '2.9'}
                          </div>
                        </div>
                        <div style={{ background: '#fffbeb', border: '2px solid #d97706', borderRadius: 8, padding: '8px 4px', textAlign: 'center' }}>
                          <div style={{ fontSize: 11, color: '#b45309', fontWeight: 650 }}>季卡 (推荐)</div>
                          <div style={{ fontSize: 16, fontWeight: 700, color: '#b45309' }}>
                            ¥{vipConfigForm.getFieldValue('quarterlyPrice') ?? '6.9'}
                          </div>
                        </div>
                        <div style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: 8, padding: '8px 4px', textAlign: 'center' }}>
                          <div style={{ fontSize: 11, color: '#64748b' }}>年卡</div>
                          <div style={{ fontSize: 16, fontWeight: 700, color: '#b45309' }}>
                            ¥{vipConfigForm.getFieldValue('yearlyPrice') ?? '19.9'}
                          </div>
                        </div>
                      </div>

                      {/* 二维码预览 */}
                      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 12 }}>
                        <div style={{ textAlign: 'center', background: '#f0fdf4', padding: 10, borderRadius: 10, border: '1px solid #bbf7d0' }}>
                          <div style={{ fontSize: 12, fontWeight: 600, color: '#166534', marginBottom: 6 }}>
                            <WechatOutlined style={{ color: '#16a34a', marginRight: 4 }} /> 微信收款码
                          </div>
                          <div style={{ width: 110, height: 110, margin: '0 auto', background: '#fff', borderRadius: 8, border: '1px solid #e2e8f0', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden' }}>
                            {wechatQrPreview ? (
                              <img src={wechatQrPreview} alt="微信收款码" style={{ width: '100%', height: '100%', objectFit: 'contain' }} />
                            ) : (
                              <Text type="secondary" style={{ fontSize: 11 }}>未上传微信码</Text>
                            )}
                          </div>
                        </div>

                        <div style={{ textAlign: 'center', background: '#f0f9ff', padding: 10, borderRadius: 10, border: '1px solid #bae6fd' }}>
                          <div style={{ fontSize: 12, fontWeight: 600, color: '#075985', marginBottom: 6 }}>
                            <AlipayCircleOutlined style={{ color: '#0284c7', marginRight: 4 }} /> 支付宝收款码
                          </div>
                          <div style={{ width: 110, height: 110, margin: '0 auto', background: '#fff', borderRadius: 8, border: '1px solid #e2e8f0', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden' }}>
                            {alipayQrPreview ? (
                              <img src={alipayQrPreview} alt="支付宝收款码" style={{ width: '100%', height: '100%', objectFit: 'contain' }} />
                            ) : (
                              <Text type="secondary" style={{ fontSize: 11 }}>未上传支付宝码</Text>
                            )}
                          </div>
                        </div>
                      </div>

                      <div style={{ marginTop: 12, background: '#fffbeb', border: '1px solid #fef3c7', borderRadius: 8, padding: '8px 10px', fontSize: 11, color: '#92400e' }}>
                        <div><b>收款人:</b> {vipConfigForm.getFieldValue('payeeName') || '管理员'}</div>
                        <div style={{ marginTop: 2 }}><b>提示:</b> {vipConfigForm.getFieldValue('paymentTip') || '付款请务必备注用户名'}</div>
                      </div>
                    </div>
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
                {/* 统计指标卡片: 6卡片严格等分 4/24 列网格，支持一键点击穿透切换 Tab */}
                <Row gutter={[16, 16]}>
                  <Col xs={12} sm={8} md={4}>
                    <Card
                      hoverable
                      bordered={false}
                      style={{ borderRadius: 12, cursor: 'pointer', transition: 'all 0.2s' }}
                      loading={loadingStats}
                      onClick={() => setActiveTab('users')}
                    >
                      <Statistic
                        title={<span style={{ whiteSpace: 'nowrap' }}>总注册用户</span>}
                        value={stats?.totalUsers ?? 0}
                        prefix={<UserOutlined />}
                      />
                      <div style={{ fontSize: 11, color: '#2563eb', marginTop: 4, whiteSpace: 'nowrap' }}>
                        点击查看用户列表 ➔
                      </div>
                    </Card>
                  </Col>
                  <Col xs={12} sm={8} md={4}>
                    <Card
                      hoverable
                      bordered={false}
                      style={{ borderRadius: 12, cursor: 'pointer', transition: 'all 0.2s' }}
                      loading={loadingStats}
                      onClick={() => setActiveTab('users')}
                    >
                      <Statistic
                        title={<span style={{ whiteSpace: 'nowrap' }}>今日活跃用户</span>}
                        value={stats?.activeUsersToday ?? 0}
                        valueStyle={{ color: '#dc2626', fontWeight: 700 }}
                        prefix={<FireOutlined />}
                      />
                      <div style={{ fontSize: 11, color: '#dc2626', marginTop: 4, whiteSpace: 'nowrap' }}>
                        查看活跃状态 ➔
                      </div>
                    </Card>
                  </Col>
                  <Col xs={12} sm={8} md={4}>
                    <Card
                      hoverable
                      bordered={false}
                      style={{ borderRadius: 12, cursor: 'pointer', transition: 'all 0.2s' }}
                      loading={loadingStats}
                      onClick={() => setActiveTab('users')}
                    >
                      <Statistic
                        title={<span style={{ whiteSpace: 'nowrap' }}>👑 VIP 会员</span>}
                        value={stats?.vipUsers ?? 0}
                        valueStyle={{ color: '#d97706', fontWeight: 650 }}
                        prefix={<CrownOutlined />}
                      />
                      <div style={{ fontSize: 11, color: '#d97706', marginTop: 4, whiteSpace: 'nowrap' }}>
                        查看VIP列表 ➔
                      </div>
                    </Card>
                  </Col>
                  <Col xs={12} sm={8} md={4}>
                    <Card
                      hoverable
                      bordered={false}
                      style={{ borderRadius: 12, cursor: 'pointer', transition: 'all 0.2s' }}
                      loading={loadingStats}
                      onClick={() => setActiveTab('funds')}
                    >
                      <Statistic
                        title={<span style={{ whiteSpace: 'nowrap' }}>监控基金总数</span>}
                        value={stats?.totalFunds ?? 0}
                        suffix="只"
                        prefix={<FundOutlined />}
                      />
                      <div style={{ fontSize: 11, color: '#2563eb', marginTop: 4, whiteSpace: 'nowrap' }}>
                        点击查看基金详情 ➔
                      </div>
                    </Card>
                  </Col>
                  <Col xs={12} sm={8} md={4}>
                    <Card bordered={false} style={{ borderRadius: 12 }} loading={loadingStats}>
                      <Statistic
                        title={<span style={{ whiteSpace: 'nowrap' }}>实时行情缓存</span>}
                        value={stats?.cachedQuotes ?? 0}
                        suffix="条"
                      />
                      <div style={{ fontSize: 11, color: '#64748b', marginTop: 4, whiteSpace: 'nowrap' }}>
                        内存快速毫秒级检索
                      </div>
                    </Card>
                  </Col>
                  <Col xs={12} sm={8} md={4}>
                    <Card
                      hoverable
                      bordered={false}
                      style={{ borderRadius: 12, cursor: 'pointer', transition: 'all 0.2s' }}
                      loading={loadingStats}
                      onClick={() => setActiveTab('calendar')}
                    >
                      <Statistic
                        title={<span style={{ whiteSpace: 'nowrap' }}>动态休市日期</span>}
                        value={stats?.activeDynamicHolidays ?? 0}
                        suffix="天"
                        prefix={<CalendarOutlined />}
                      />
                      <div style={{ fontSize: 11, color: '#d97706', marginTop: 4, whiteSpace: 'nowrap' }}>
                        点击配置休市日历 ➔
                      </div>
                    </Card>
                  </Col>
                </Row>

                {/* 用户推荐来源流量大盘 */}
                {stats?.referralStats && (
                  <Card
                    title="📊 用户获客与推荐来源流量分析"
                    bordered={false}
                    style={{ borderRadius: 14 }}
                  >
                    <Row gutter={[16, 16]}>
                      {['闲鱼', 'B站', '朋友推荐', '自己搜索', '其他'].map((src) => {
                        const count = stats.referralStats?.[src] ?? 0
                        const pct = stats.totalUsers > 0 ? ((count / stats.totalUsers) * 100).toFixed(1) : '0'
                        let tagColor = 'default'
                        let icon = '🌐'
                        if (src === '闲鱼') { tagColor = 'orange'; icon = '🐟' }
                        else if (src === 'B站') { tagColor = 'magenta'; icon = '📺' }
                        else if (src === '朋友推荐') { tagColor = 'green'; icon = '🤝' }
                        else if (src === '自己搜索') { tagColor = 'blue'; icon = '🔍' }

                        return (
                          <Col xs={12} sm={8} md={4} key={src} style={{ minWidth: 140 }}>
                            <div
                              style={{
                                background: '#f8fafc',
                                border: '1px solid #f1f5f9',
                                borderRadius: 10,
                                padding: '12px 14px',
                                textAlign: 'center',
                              }}
                            >
                              <Tag color={tagColor} style={{ borderRadius: 6, margin: '0 0 6px 0', fontSize: 13 }}>
                                {icon} {src}
                              </Tag>
                              <div style={{ fontSize: 20, fontWeight: 700, color: '#0f172a' }}>
                                {count} <span style={{ fontSize: 12, fontWeight: 400, color: '#64748b' }}>人</span>
                              </div>
                              <div style={{ fontSize: 11, color: '#94a3b8', marginTop: 2 }}>
                                占比 {pct}%
                              </div>
                            </div>
                          </Col>
                        )
                      })}
                    </Row>
                  </Card>
                )}

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

      {/* 监控基金被关注用户列表弹窗 */}
      <Modal
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <TeamOutlined style={{ color: '#2563eb', fontSize: 18 }} />
            <span>
              关注基金【{selectedFundForWatchers?.name} ({selectedFundForWatchers?.code})】的用户列表
            </span>
            <Tag color="blue" style={{ borderRadius: 6, marginLeft: 4 }}>
              共 {fundWatchers.length} 人关注
            </Tag>
          </div>
        }
        open={watchersModalOpen}
        onCancel={() => setWatchersModalOpen(false)}
        footer={[
          <Button key="close" type="primary" onClick={() => setWatchersModalOpen(false)} style={{ borderRadius: 6 }}>
            关闭
          </Button>,
        ]}
        width={850}
        destroyOnClose
      >
        <Table
          dataSource={fundWatchers}
          rowKey="username"
          loading={loadingWatchers}
          pagination={{ pageSize: 8 }}
          scroll={{ x: 'max-content' }}
          columns={[
            {
              title: '用户名',
              dataIndex: 'username',
              key: 'username',
              width: 120,
              render: (name: string) => <Text strong style={{ color: '#0f172a' }}>{name}</Text>,
            },
            {
              title: '角色',
              dataIndex: 'role',
              key: 'role',
              width: 80,
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
              width: 100,
              render: (status: string) => (
                <Tag
                  color={status === 'NORMAL' ? 'success' : status === 'PENDING' ? 'warning' : 'error'}
                  style={{ borderRadius: 6 }}
                >
                  {status === 'NORMAL' ? '正常' : status === 'PENDING' ? '待审核' : '已停用'}
                </Tag>
              ),
            },
            {
              title: '会员级别',
              key: 'vip',
              width: 130,
              render: (_: unknown, record: UserView) =>
                record.isVip ? (
                  <Text style={{ color: '#d97706', fontSize: 12 }}>
                    VIP {record.vipExpireAt ? `至 ${dayjs(record.vipExpireAt).format('YYYY-MM-DD')}` : '(永久)'}
                  </Text>
                ) : (
                  <Text type="secondary" style={{ fontSize: 12 }}>普通会员</Text>
                ),
            },
            {
              title: '了解来源',
              dataIndex: 'referralSource',
              key: 'referralSource',
              width: 95,
              render: (src: string) => {
                const s = src || '自己搜索'
                let c = 'default'
                if (s.includes('闲鱼')) c = 'orange'
                else if (s.includes('B站')) c = 'magenta'
                else if (s.includes('朋友')) c = 'green'
                else if (s.includes('搜索')) c = 'blue'
                return <Tag color={c} style={{ borderRadius: 6 }}>{s}</Tag>
              },
            },
            {
              title: '自选总数',
              dataIndex: 'watchlistCount',
              key: 'watchlistCount',
              width: 90,
              render: (cnt: number) => <Tag color="blue">{cnt} 只</Tag>,
            },
            {
              title: '注册时间',
              dataIndex: 'createdAt',
              key: 'createdAt',
              width: 135,
              render: (t: string) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'),
            },
            {
              title: '操作',
              key: 'action',
              fixed: 'right',
              width: 140,
              render: (_: unknown, record: UserView) => (
                <Space size={8}>
                  <Button
                    size="small"
                    type="link"
                    icon={<EyeOutlined />}
                    onClick={() => {
                      setWatchersModalOpen(false)
                      handleViewWatchlist(record)
                    }}
                    style={{ padding: 0 }}
                  >
                    调阅自选
                  </Button>
                  <Button
                    size="small"
                    type="link"
                    icon={<CrownOutlined />}
                    onClick={() => {
                      setWatchersModalOpen(false)
                      openVipModal(record)
                    }}
                    style={{ padding: 0 }}
                  >
                    VIP
                  </Button>
                </Space>
              ),
            },
          ]}
        />
      </Modal>
    </div>
  )
}
