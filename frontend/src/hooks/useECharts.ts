import { useEffect, useRef, useCallback } from 'react'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { EChartsCoreOption } from 'echarts/core'

echarts.use([LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

/**
 * 健壮的 ECharts 实例 hook: 
 * 解决因 Skeleton/条件渲染导致的 DOM 初始为 null 无法初始化的死锁问题，
 * 支持动态按需初始化并自适应容器尺寸。
 */
export function useECharts() {
  const chartRef = useRef<echarts.ECharts | null>(null)
  const containerElRef = useRef<HTMLDivElement | null>(null)

  const renderChart = useCallback((el: HTMLDivElement | null, option: EChartsCoreOption) => {
    if (!el) return
    containerElRef.current = el

    // 如果还没有初始化，或实例已被意外销毁，安全建立
    if (!chartRef.current) {
      const chart = echarts.init(el)
      chartRef.current = chart
    }

    chartRef.current.setOption(option, true)
    // 强制触发一次微任务 resize，避免刚消除骨架屏时的 0px 宽度塌陷
    requestAnimationFrame(() => {
      chartRef.current?.resize()
    })
  }, [])

  useEffect(() => {
    const onResize = () => {
      chartRef.current?.resize()
    }
    window.addEventListener('resize', onResize)
    return () => {
      window.removeEventListener('resize', onResize)
      if (chartRef.current) {
        chartRef.current.dispose()
        chartRef.current = null
      }
    }
  }, [])

  return { renderChart }
}
