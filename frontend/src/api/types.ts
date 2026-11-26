export interface EstimateResult {
  fundCode: string
  fundName: string
  fundType: string
  estimateNav: number | null
  estimatePct: number | null
  prevNav: number | null
  navDate: string | null
  reportQt: string | null
  quoteTs: string | null
  stale: boolean
  message: string | null
  disclaimer: string | null
}

export interface AddFundResult {
  fundCode: string
  success: boolean
  message: string
}

export interface HoldingView {
  stockCode: string
  stockName: string
  weight: number
  pctChg: number | null
  reportQt: string | null
}

export interface EstimatePoint {
  estTime: string
  estNav: number
  estPct: number
}

export interface BondInfo {
  indexName: string
  indexPct: number | null
  duration: number
  referenceDuration: number
  formula: string
}

export interface FundDetailView {
  estimate: EstimateResult | null
  holdings: HoldingView[]
  history: EstimatePoint[]
  bondInfo?: BondInfo | null
}

export type PerformanceRange = '1m' | '3m' | '6m' | '1y' | '3y' | '5y' | 'all'

export interface NavPointView {
  date: string
  nav: number | null
  cumulativeNav: number | null
  dailyGrowthPct: number
  accumReturnPct: number
}

export interface AccuracyPointView {
  date: string
  actualPct: number
  estPct: number
  diffPct: number
  directionMatched: boolean
  estAccumReturnPct?: number | null
}

export interface ValuationAccuracyView {
  hasData: boolean
  sampleCount: number
  mae: number | null
  directionMatchRate: number | null
  points: AccuracyPointView[]
}

export interface FundPerformanceView {
  fundCode: string
  range: PerformanceRange
  totalReturnPct: number
  navPoints: NavPointView[]
  accuracy: ValuationAccuracyView
}

export const FUND_TYPE_LABEL: Record<string, string> = {
  index: '指数型',
  enhanced: '指数增强',
  active: '主动股票',
  mixed: '混合型',
  bond: '债券型',
  other: '其他',
}

export const FUND_TYPE_COLOR: Record<string, string> = {
  index: 'blue',
  enhanced: 'purple',
  active: 'red',
  mixed: 'orange',
  bond: 'green',
  other: 'default',
}

export interface NoticeView {
  message: string
  type?: 'info' | 'success' | 'warning' | 'error'
  closable?: boolean
  custom?: boolean
}

export interface UserView {
  id: number
  username: string
  role: string
  status: string
  isVip: boolean
  vipExpireAt: string | null
  lastLoginAt: string | null
  loginCount: number
  referralSource?: string
  createdAt: string
  watchlistCount: number
}

export interface CalendarHolidayView {
  id: number
  holidayDate: string
  description: string
  createdAt: string
}

export interface CalendarHolidaysResponse {
  customHolidays: CalendarHolidayView[]
  allDynamicHolidays: string[]
}

export interface FundView {
  code: string
  name: string
  type: string
  typeRaw?: string
  trackIndex?: string
  prevNav: number | null
  navDate: string
  userCount: number
  updatedAt: string
}

export interface SystemStats {
  totalUsers: number
  vipUsers: number
  activeUsersToday: number
  totalWatchlists: number
  totalFunds: number
  cachedQuotes: number
  lastValuationRunTime: string
  activeDynamicHolidays: number
  referralStats?: Record<string, number>
}

export interface PositionItemView {
  id: number
  fundCode: string
  fundName: string
  fundType: string | null
  holdingAmount: number
  yesterdayIncome: number
  holdingProfit: number
  holdingProfitRate: number | null
  costAmount: number
  holdingShares: number | null
  costPrice: number | null
  prevNav: number | null
  navDate: string | null
  estimateNav: number | null
  estimatePct: number | null
  quoteTs: string | null
  stale: boolean
  todayIncome: number
  dynamicMarketValue: number
  dynamicTotalProfit: number
  dynamicTotalProfitRate: number
}

export interface PortfolioSummaryView {
  totalMarketValue: number
  totalHoldingAmount: number
  totalYesterdayIncome: number
  totalHoldingProfit: number
  totalTodayIncome: number
  totalTodayIncomePct: number
  dynamicTotalProfit: number
  dynamicTotalProfitRate: number
}

export interface PortfolioView {
  summary: PortfolioSummaryView
  items: PositionItemView[]
}

export interface PositionSaveRequest {
  fundCode: string
  holdingAmount: number
  yesterdayIncome?: number | null
  holdingProfit?: number | null
  holdingProfitRate?: number | null
}

export interface VipConfigView {
  monthlyPrice: number
  quarterlyPrice: number
  quarterlyOrigPrice?: number | null
  yearlyPrice: number
  yearlyOrigPrice?: number | null
  wechatQrUrl?: string | null
  alipayQrUrl?: string | null
  payeeName?: string | null
  paymentTip?: string | null
  requireApproval?: boolean | null
  webhookUrl?: string | null
}

export interface VipOrderView {
  id: number
  username: string
  planKey: string
  planName: string
  amount: number
  payMethod: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | string
  createdAt: string
  processedAt: string | null
}

export interface UserProfileView {
  username: string
  role: string
  status: string
  isVip: boolean
  vipExpireAt: string | null
}

export interface RegisterResponse {
  status: string
  username: string
  message: string
  needApproval?: boolean
  token?: string | null
  role?: string | null
  isVip?: boolean
}

export interface NotificationChannelView {
  email?: string
  emailEnabled?: boolean
  wechatChannelType?: 'serverchan' | string
  serverchanKey?: string
  wechatEnabled?: boolean
  remindEnabled?: boolean
  remindTime?: string
  eveningRemindEnabled?: boolean
  eveningRemindTime?: string
  pushScope?: 'ALL_WATCHLIST' | 'PORTFOLIO_ONLY' | string
  hideAmount?: boolean
}

export interface EmailConfigView {
  smtpHost: string
  smtpPort: number
  smtpUsername?: string
  smtpPassword?: string
  senderName?: string
  senderAddress?: string
  sslEnabled?: boolean
  wxpusherAppToken?: string
  wxpusherQrUrl?: string
}

export interface FundSearchItem {
  code: string
  name: string
  type: string
}

export interface OcrFundItem {
  fundName: string
  fundCode: string | null
  holdingAmount: number
  yesterdayIncome?: number | null
  holdingProfit?: number | null
  holdingProfitRate?: number | null
  candidates?: FundSearchItem[]
  warning?: string | null
}

export interface ScreenshotImportResult {
  success: boolean
  message: string
  items: OcrFundItem[]
  remainingQuota?: number
}

export interface VisionConfigView {
  enabled: boolean
  apiBaseUrl: string
  apiKey?: string
  modelName: string
  dailyUserLimit: number
  hasApiKey?: boolean
}


