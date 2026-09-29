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
