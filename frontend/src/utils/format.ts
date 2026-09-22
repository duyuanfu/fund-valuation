export function colorOf(pct: number | null | undefined): string {
  if (pct === null || pct === undefined || pct === 0) return '#999'
  return pct > 0 ? '#d93026' : '#0a7a3f'
}

export function fmtPct(pct: number | null | undefined): string {
  if (pct === null || pct === undefined) return '--'
  return `${pct > 0 ? '+' : ''}${pct.toFixed(2)}%`
}

export function fmtNav(nav: number | null | undefined): string {
  if (nav === null || nav === undefined) return '--'
  return nav.toFixed(4)
}

/** 交易时段判断(浏览器本地时间近似;服务器有权威判断) */
export function isTradingTime(now: Date = new Date()): boolean {
  const day = now.getDay()
  if (day === 0 || day === 6) return false
  const t = now.getHours() * 100 + now.getMinutes()
  return (t >= 930 && t <= 1130) || (t >= 1300 && t <= 1500)
}
