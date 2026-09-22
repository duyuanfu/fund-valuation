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
