import { useEffect, useRef, useState } from 'react'
import type { AddFundResult, EstimateResult } from '../api/types'
import { api, sseUrl } from '../api'

const STORAGE_KEY = 'fund-valuation-cached-watchlist'

// 模块级内存缓存: 组件切换卸载后依然保留，实现 0ms 瞬间恢复无闪烁
let memoryEstimatesCache: EstimateResult[] = (() => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) return JSON.parse(raw) as EstimateResult[]
  } catch {
    /* ignore */
  }
  return []
})()

let inFlightPromise: Promise<EstimateResult[]> | null = null

/**
 * 自选估值列表 hook: SWR (Stale-While-Revalidate) 体验架构
 * 1. 页面进入/详情页返回时 0ms 瞬间用已有缓存铺设视图，杜绝“先空屏闪烁再蹦出来”；
 * 2. 静默在后台发起网络刷新并替换；
 * 3. 请求级去重（In-flight Promise Sharing）：并发或快速重新挂载时共享同一网络请求，防止重复请求；
 * 4. 伴随 SSE 实施增量同步与持久化。
 */
export function useWatchlist() {
  const [estimates, setEstimates] = useState<EstimateResult[]>(() => memoryEstimatesCache)
  // 如果已有缓存，不显示全屏阻断 loading；仅在全新空白时提示
  const [loading, setLoading] = useState(() => memoryEstimatesCache.length === 0)
  const [error, setError] = useState<string | null>(null)
  const esRef = useRef<EventSource | null>(null)

  const updateAndCache = (data: EstimateResult[]) => {
    memoryEstimatesCache = data
    setEstimates(data)
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
    } catch {
      /* ignore storage quota */
    }
  }

  const refresh = () => {
    // 请求去重：如果当前已有相同的获取请求正在进行，直接复用该 Promise，不重复发网络请求
    if (inFlightPromise) {
      return inFlightPromise
    }
    if (memoryEstimatesCache.length === 0) {
      setLoading(true)
    }
    setError(null)
    inFlightPromise = api
      .listWatchlist()
      .then((data) => {
        updateAndCache(data)
        return data
      })
      .catch((e: Error) => {
        setError(e.message)
        if (e.message.includes('停用') || e.message.includes('未登录')) {
          memoryEstimatesCache = []
          setEstimates([])
          localStorage.removeItem(STORAGE_KEY)
        }
        throw e
      })
      .finally(() => {
        inFlightPromise = null
        setLoading(false)
      })
    return inFlightPromise
  }

  // 初始加载与后台静默刷新
  useEffect(() => {
    refresh()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // SSE 长连接通道
  useEffect(() => {
    const es = new EventSource(sseUrl())
    esRef.current = es
    es.addEventListener('estimate', (ev) => {
      try {
        const data = JSON.parse((ev as MessageEvent).data) as EstimateResult[]
        updateAndCache(data)
      } catch {
        /* ignore bad payload */
      }
    })
    es.onerror = () => {
      // EventSource 会自动重连
    }
    return () => {
      es.close()
      esRef.current = null
    }
  }, [])

  const add = async (fundCode: string) => {
    await api.addFund(fundCode)
    refresh()
  }

  // 批量添加: 返回逐条结果，调用方据此提示成功/失败明细
  const addBatch = async (fundCodes: string[]): Promise<AddFundResult[]> => {
    const results = await api.addFunds(fundCodes)
    refresh()
    return results
  }

  const remove = async (fundCode: string) => {
    await api.removeFund(fundCode)
    updateAndCache(estimates.filter((e) => e.fundCode !== fundCode))
    refresh()
  }

  const reorder = async (fundCodes: string[]) => {
    const byCode = new Map(estimates.map((e) => [e.fundCode, e]))
    const reordered = fundCodes.map((c) => byCode.get(c)).filter((e): e is EstimateResult => e != null)
    updateAndCache(reordered)
    await api.reorder(fundCodes)
  }

  return { estimates, loading, error, refresh, add, addBatch, remove, reorder }
}
