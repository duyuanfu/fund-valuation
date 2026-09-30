export function colorOf(pct: number | string | null | undefined): string {
  if (pct === null || pct === undefined || pct === '') return '#999'
  const n = Number(pct)
  if (isNaN(n) || n === 0) return '#999'
  return n > 0 ? '#d93026' : '#0a7a3f'
}

export function fmtPct(pct: number | string | null | undefined): string {
  if (pct === null || pct === undefined || pct === '') return '--'
  const n = Number(pct)
  if (isNaN(n)) return '--'
  return `${n > 0 ? '+' : ''}${n.toFixed(2)}%`
}

export function fmtNav(nav: number | string | null | undefined): string {
  if (nav === null || nav === undefined || nav === '') return '--'
  const n = Number(nav)
  if (isNaN(n)) return '--'
  return n.toFixed(4)
}

export function fmtMoney(val: number | string | null | undefined, showPlus = false): string {
  if (val === null || val === undefined || val === '') return '--'
  const n = Number(val)
  if (isNaN(n)) return '--'
  const prefix = showPlus && n > 0 ? '+' : ''
  return `${prefix}¥${n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}
