import { useEffect, useRef, useCallback } from 'react'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  MarkLineComponent,
  MarkPointComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { EChartsCoreOption } from 'echarts/core'

echarts.use([
  LineChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  MarkLineComponent,
  MarkPointComponent,
  CanvasRenderer,
])

/**
 * 健壮的 ECharts 实例 hook:
 * 支持动态 DOM 切换、ResizeObserver 实时响应容器宽度、自动销毁脱落节点、按需自适应尺寸。
 */
export function useECharts() {
  const chartRef = useRef<echarts.ECharts | null>(null)
  const containerElRef = useRef<HTMLDivElement | null>(null)
  const resizeObserverRef = useRef<ResizeObserver | null>(null)

  const renderChart = useCallback((el: HTMLDivElement | null, option: EChartsCoreOption) => {
    if (!el) return
    containerElRef.current = el

    // 检查传入的 DOM 元素上是否已有 ECharts 实例
    let chart = echarts.getInstanceByDom(el)

    // 如果 hook 当前持有的实例属于另一个已脱离或不同的 DOM 节点，安全释放它
    if (chartRef.current && chartRef.current !== chart) {
      try {
        chartRef.current.dispose()
      } catch {
        /* ignore */
      }
      chartRef.current = null
    }

    if (!chart) {
      chart = echarts.init(el)
    }
    chartRef.current = chart

    chart.setOption(option, true)

    // 绑定 ResizeObserver 确保骨架屏/标签页切换/动态加载完毕后自动拉伸至完整宽度
    if (resizeObserverRef.current) {
      resizeObserverRef.current.disconnect()
    }
    if (typeof ResizeObserver !== 'undefined') {
      const ro = new ResizeObserver(() => {
        chartRef.current?.resize()
      })
      ro.observe(el)
      resizeObserverRef.current = ro
    }

    // 立即 resize 并结合 microtask/timer 防止布局未定型时的 0 宽度塌陷
    chart.resize()
    requestAnimationFrame(() => {
      chart?.resize()
    })
    setTimeout(() => {
      chart?.resize()
    }, 60)
  }, [])

  useEffect(() => {
    const onResize = () => {
      chartRef.current?.resize()
    }
    window.addEventListener('resize', onResize)
    return () => {
      window.removeEventListener('resize', onResize)
      resizeObserverRef.current?.disconnect()
      if (chartRef.current) {
        chartRef.current.dispose()
        chartRef.current = null
      }
    }
  }, [])

  return { renderChart, chartRef }
}
