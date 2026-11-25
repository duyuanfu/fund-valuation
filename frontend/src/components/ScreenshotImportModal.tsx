import { useState, useEffect, useRef } from 'react'
import {
  Modal,
  Button,
  Table,
  InputNumber,
  Select,
  Typography,
  Tag,
  Space,
  Spin,
  message,
  Checkbox,
  Card,
  Row,
  Col,
} from 'antd'
import {
  CameraOutlined,
  CloudUploadOutlined,
  ExclamationCircleOutlined,
  DeleteOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons'
import { api } from '../api'
import type { OcrFundItem, FundSearchItem, PositionSaveRequest } from '../api/types'

const { Text } = Typography

interface ScreenshotImportModalProps {
  open: boolean
  onClose: () => void
  onSuccess: () => Promise<void> | void
}

interface EditableOcrRow extends OcrFundItem {
  key: string
  selected: boolean
  remoteOptions?: FundSearchItem[]
  searching?: boolean
}

export function ScreenshotImportModal({ open, onClose, onSuccess }: ScreenshotImportModalProps) {
  const [analyzing, setAnalyzing] = useState(false)
  const [step, setStep] = useState<'upload' | 'review'>('upload')
  const [rows, setRows] = useState<EditableOcrRow[]>([])
  const [remainingQuota, setRemainingQuota] = useState<number | null>(null)
  const [importing, setImporting] = useState(false)
  const [isDragOver, setIsDragOver] = useState(false)
  const [isMobile, setIsMobile] = useState(() => window.innerWidth < 768)
  const fileInputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth < 768)
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  const resetState = () => {
    setAnalyzing(false)
    setStep('upload')
    setRows([])
  }

  // 客户端等比分辨率压缩
  const compressImage = async (fileOrBlob: Blob): Promise<{ base64: string; mimeType: string }> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = (e) => {
        const img = new Image()
        img.onload = () => {
          const maxDim = 1280
          let w = img.width
          let h = img.height
          if (w > maxDim || h > maxDim) {
            if (w > h) {
              h = Math.round((h * maxDim) / w)
              w = maxDim
            } else {
              w = Math.round((w * maxDim) / h)
              h = maxDim
            }
          }
          const canvas = document.createElement('canvas')
          canvas.width = w
          canvas.height = h
          const ctx = canvas.getContext('2d')
          if (!ctx) return reject(new Error('Canvas 上下文创建失败'))
          ctx.drawImage(img, 0, 0, w, h)
          const base64 = canvas.toDataURL('image/jpeg', 0.88)
          resolve({ base64, mimeType: 'image/jpeg' })
        }
        img.onerror = () => reject(new Error('图片加载解析失败'))
        img.src = e.target?.result as string
      }
      reader.onerror = () => reject(new Error('读取图片文件失败'))
      reader.readAsDataURL(fileOrBlob)
    })
  }

  const handleProcessFiles = async (files: File[]) => {
    if (!files || files.length === 0) return
    const validFiles = Array.from(files).slice(0, 3)

    setAnalyzing(true)
    const combinedRows: EditableOcrRow[] = []

    try {
      for (let i = 0; i < validFiles.length; i++) {
        const file = validFiles[i]
        const { base64, mimeType } = await compressImage(file)

        const result = await api.importPortfolioScreenshotBase64(base64, mimeType)
        if (result.remainingQuota !== undefined) {
          setRemainingQuota(result.remainingQuota)
        }

        if (!result.success || !result.items || result.items.length === 0) {
          message.warning(result.message || `第 ${i + 1} 张图片未识别到持仓信息`)
          continue
        }

        result.items.forEach((item: OcrFundItem, idx: number) => {
          combinedRows.push({
            ...item,
            key: `${Date.now()}_${i}_${idx}`,
            selected: Boolean(item.fundCode && item.holdingAmount > 0),
            remoteOptions: item.candidates || [],
          })
        })
      }

      if (combinedRows.length > 0) {
        setRows(combinedRows)
        setStep('review')
        message.success(`成功识别 ${combinedRows.length} 只基金持仓，请核对！`)
      } else {
        message.error('未能从所选截图中识别到基金持仓，请上传清晰的支付宝持仓页面')
      }
    } catch (e: any) {
      message.error(e?.message || '图片识别处理异常，请检查网络或配置')
    } finally {
      setAnalyzing(false)
    }
  }

  // 剪贴板粘贴监听
  useEffect(() => {
    if (!open || step !== 'upload') return

    const handlePaste = (e: ClipboardEvent) => {
      const items = e.clipboardData?.items
      if (!items) return

      const imageFiles: File[] = []
      for (let i = 0; i < items.length; i++) {
        if (items[i].type.indexOf('image') !== -1) {
          const file = items[i].getAsFile()
          if (file) imageFiles.push(file)
        }
      }

      if (imageFiles.length > 0) {
        e.preventDefault()
        handleProcessFiles(imageFiles)
      }
    }

    window.addEventListener('paste', handlePaste)
    return () => window.removeEventListener('paste', handlePaste)
  }, [open, step])

  const handleSearchFundCandidates = async (rowKey: string, query: string) => {
    if (!query || query.trim().length === 0) return
    setRows((prev) => prev.map((r) => (r.key === rowKey ? { ...r, searching: true } : r)))
    try {
      const list = await api.searchFunds(query.trim())
      setRows((prev) =>
        prev.map((r) => {
          if (r.key !== rowKey) return r
          const existing = r.remoteOptions || []
          const merged = [...existing]
          list.forEach((item) => {
            if (!merged.some((m) => m.code === item.code)) {
              merged.push(item)
            }
          })
          return { ...r, remoteOptions: merged, searching: false }
        })
      )
    } catch {
      setRows((prev) => prev.map((r) => (r.key === rowKey ? { ...r, searching: false } : r)))
    }
  }

  const updateRow = (key: string, patch: Partial<EditableOcrRow>) => {
    setRows((prev) =>
      prev.map((r) => {
        if (r.key !== key) return r
        const updated = { ...r, ...patch }
        if (patch.fundCode) {
          updated.warning = null
          updated.selected = true
        }
        return updated
      })
    )
  }

  const handleBatchImport = async () => {
    const selectedRows = rows.filter((r) => r.selected)
    if (selectedRows.length === 0) {
      message.warning('请至少勾选一只待导入的基金')
      return
    }

    for (const r of selectedRows) {
      if (!r.fundCode || !r.fundCode.match(/^\d{6}$/)) {
        message.error(`基金【${r.fundName}】尚未选定正确的 6 位基金代码`)
        return
      }
      if (!r.holdingAmount || r.holdingAmount <= 0) {
        message.error(`基金【${r.fundName}】持仓金额必须大于 0`)
        return
      }
    }

    const payload: PositionSaveRequest[] = selectedRows.map((r) => ({
      fundCode: r.fundCode!,
      holdingAmount: r.holdingAmount,
      yesterdayIncome: r.yesterdayIncome ?? 0,
      holdingProfit: r.holdingProfit ?? 0,
      holdingProfitRate: r.holdingProfitRate ?? null,
    }))

    setImporting(true)
    try {
      const res = await api.batchSavePortfolio(payload)
      message.success(`成功导入 ${res.count || selectedRows.length} 只持仓基金！`)
      onClose()
      resetState()
      if (onSuccess) {
        await onSuccess()
      }
    } catch (e: any) {
      message.error(e?.message || '批量导入持仓失败')
    } finally {
      setImporting(false)
    }
  }

  const selectedCount = rows.filter((r) => r.selected).length

  // 代码候选选择与搜索
  const renderCodeSelector = (r: EditableOcrRow) => {
    const opts = r.remoteOptions || []

    return (
      <div>
        <Select
          showSearch
          value={r.fundCode || undefined}
          placeholder="搜索或选择 6 位代码"
          loading={r.searching}
          filterOption={false}
          onSearch={(kw) => handleSearchFundCandidates(r.key, kw)}
          onChange={(newCode) => updateRow(r.key, { fundCode: newCode })}
          style={{ width: '100%' }}
          dropdownMatchSelectWidth={false}
          options={opts.map((c) => ({
            value: c.code,
            label: (
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <Text strong style={{ color: '#0f172a', fontFamily: 'monospace' }}>
                  {c.code}
                </Text>
                <span>{c.name}</span>
              </div>
            ),
          }))}
        />
      </div>
    )
  }

  return (
    <Modal
      open={open}
      onCancel={() => {
        onClose()
        resetState()
      }}
      title={
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8, paddingRight: 24 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <div
              style={{
                width: 28,
                height: 28,
                borderRadius: 6,
                background: 'linear-gradient(135deg, #0284c7 0%, #0369a1 100%)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <CameraOutlined style={{ color: '#fff', fontSize: 15 }} />
            </div>
            <span style={{ fontWeight: 650, fontSize: 16, color: '#0f172a' }}>
              支付宝持仓截图智能导入
            </span>
            <Tag color="gold" style={{ borderRadius: 6, fontWeight: 700, fontSize: 11, margin: 0 }}>
              VIP
            </Tag>
          </div>

          {/* 极简隐私承诺徽标 */}
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 4,
              padding: '2px 8px',
              borderRadius: 12,
              background: '#f8fafc',
              border: '1px solid #e2e8f0',
              fontSize: 11,
              color: '#64748b',
            }}
          >
            <SafetyCertificateOutlined style={{ color: '#16a34a' }} />
            <span>内存流转 · 不存盘</span>
            {remainingQuota !== null && (
              <span style={{ color: '#0284c7', marginLeft: 4 }}>
                (今日余 {remainingQuota} 次)
              </span>
            )}
          </div>
        </div>
      }
      width={step === 'upload' ? 520 : (isMobile ? '96vw' : 880)}
      footer={
        step === 'upload' ? null : (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8 }}>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => {
                setStep('upload')
                setRows([])
              }}
              size={isMobile ? 'small' : 'middle'}
            >
              重新上传
            </Button>
            <Space>
              <Button size={isMobile ? 'small' : 'middle'} onClick={onClose}>
                取消
              </Button>
              <Button
                type="primary"
                loading={importing}
                disabled={selectedCount === 0}
                onClick={handleBatchImport}
                size={isMobile ? 'small' : 'middle'}
                style={{
                  background: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)',
                  borderRadius: 8,
                  fontWeight: 550,
                  boxShadow: '0 2px 8px rgba(37, 99, 235, 0.25)',
                }}
              >
                导入选中持仓 ({selectedCount})
              </Button>
            </Space>
          </div>
        )
      }
      destroyOnClose
      centered
      styles={{
        body: {
          padding: isMobile ? '16px 12px' : '20px 24px',
        },
      }}
    >
      {step === 'upload' ? (
        <div style={{ marginTop: 12 }}>
          {analyzing ? (
            <div style={{ textAlign: 'center', padding: '48px 0' }}>
              <Spin size="large" />
              <div style={{ marginTop: 16, fontSize: 15, fontWeight: 600, color: '#0f172a' }}>
                AI 正在精准提取持仓数据...
              </div>
              <div style={{ marginTop: 6, fontSize: 12, color: '#64748b' }}>
                正在自动匹配基金代码与金额收益，请稍候
              </div>
            </div>
          ) : (
            <div>
              <div
                onDragOver={(e) => {
                  e.preventDefault()
                  setIsDragOver(true)
                }}
                onDragLeave={() => setIsDragOver(false)}
                onDrop={(e) => {
                  e.preventDefault()
                  setIsDragOver(false)
                  if (e.dataTransfer.files) {
                    handleProcessFiles(Array.from(e.dataTransfer.files))
                  }
                }}
                onClick={() => fileInputRef.current?.click()}
                style={{
                  border: isDragOver ? '2px dashed #0284c7' : '1.5px dashed #cbd5e1',
                  background: isDragOver ? '#f0f9ff' : '#f8fafc',
                  borderRadius: 14,
                  padding: isMobile ? '28px 16px' : '40px 20px',
                  textAlign: 'center',
                  cursor: 'pointer',
                  transition: 'all 0.2s',
                }}
              >
                <div
                  style={{
                    width: 52,
                    height: 52,
                    borderRadius: 16,
                    background: '#e0f2fe',
                    display: 'inline-flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    marginBottom: 12,
                  }}
                >
                  <CloudUploadOutlined style={{ fontSize: 28, color: '#0284c7' }} />
                </div>
                <div style={{ fontSize: 15, fontWeight: 600, color: '#0f172a' }}>
                  {isMobile ? '点击选择支付宝截图' : '点击选择截图 或 拖拽截图到此处'}
                </div>
                <div style={{ marginTop: 6, fontSize: 12, color: '#64748b' }}>
                  {isMobile ? '支持直接从手机相册选择持仓页面截图' : '支持截图后按 Ctrl+V 剪贴板快速粘贴'}
                </div>
                <div style={{ marginTop: 10, fontSize: 11, color: '#94a3b8' }}>
                  支持【持仓列表】或【单只详情】截图 · 支持 jpg / png / webp (最多3张)
                </div>
                <input
                  type="file"
                  ref={fileInputRef}
                  style={{ display: 'none' }}
                  accept="image/png,image/jpeg,image/webp,image/jpg"
                  multiple
                  onChange={(e) => {
                    if (e.target.files) {
                      handleProcessFiles(Array.from(e.target.files))
                    }
                  }}
                />
              </div>

              <div style={{ marginTop: 12, fontSize: 11, color: '#94a3b8', textAlign: 'center' }}>
                提示：支付宝持仓截图通常不印 6 位代码，系统会自动为您联想匹配 A/C 类代码
              </div>
            </div>
          )}
        </div>
      ) : (
        <div style={{ marginTop: 10 }}>
          {/* 顶部控制栏 */}
          <div style={{ marginBottom: 12, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <Text strong style={{ fontSize: 14, color: '#0f172a' }}>
                核对识别结果 ({rows.length} 只)
              </Text>
              <span style={{ fontSize: 12, color: '#64748b', marginLeft: 8 }}>
                已选 {selectedCount} 只
              </span>
            </div>
            <Space size={8}>
              <Button
                size="small"
                onClick={() => setRows((prev) => prev.map((r) => ({ ...r, selected: true })))}
              >
                全选
              </Button>
              <Button
                size="small"
                onClick={() => setRows((prev) => prev.map((r) => ({ ...r, selected: false })))}
              >
                取消
              </Button>
            </Space>
          </div>

          {/* 手机端响应式卡片流 (Mobile First) */}
          {isMobile ? (
            <div style={{ maxHeight: '60vh', overflowY: 'auto', paddingRight: 2 }}>
              <Space direction="vertical" size={10} style={{ width: '100%' }}>
                {rows.map((r) => (
                  <Card
                    key={r.key}
                    size="small"
                    style={{
                      borderRadius: 12,
                      border: r.selected ? '1.5px solid #93c5fd' : '1px solid #e2e8f0',
                      background: r.selected ? '#ffffff' : '#f8fafc',
                      boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
                    }}
                    styles={{ body: { padding: '10px 12px' } }}
                  >
                    <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 8 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8, flex: 1 }}>
                        <Checkbox
                          checked={r.selected}
                          onChange={(e) => updateRow(r.key, { selected: e.target.checked })}
                        />
                        <div>
                          <div style={{ fontWeight: 650, color: '#0f172a', fontSize: 14 }}>
                            {r.fundName}
                          </div>
                          {r.warning && (
                            <div style={{ fontSize: 11, color: '#d97706', marginTop: 2 }}>
                              <ExclamationCircleOutlined /> {r.warning}
                            </div>
                          )}
                        </div>
                      </div>

                      <Button
                        size="small"
                        type="text"
                        danger
                        icon={<DeleteOutlined />}
                        onClick={() => setRows((prev) => prev.filter((item) => item.key !== r.key))}
                      />
                    </div>

                    <div style={{ marginTop: 8 }}>
                      {renderCodeSelector(r)}
                    </div>

                    <Row gutter={[8, 8]} style={{ marginTop: 8 }}>
                      <Col span={12}>
                        <div style={{ fontSize: 11, color: '#64748b' }}>持仓金额 (元)</div>
                        <InputNumber
                          value={r.holdingAmount}
                          min={0.01}
                          precision={2}
                          onChange={(v) => updateRow(r.key, { holdingAmount: v ?? 0 })}
                          style={{ width: '100%', marginTop: 2 }}
                        />
                      </Col>
                      <Col span={12}>
                        <div style={{ fontSize: 11, color: '#64748b' }}>昨日收益 (元)</div>
                        <InputNumber
                          value={r.yesterdayIncome}
                          precision={2}
                          onChange={(v) => updateRow(r.key, { yesterdayIncome: v ?? 0 })}
                          style={{
                            width: '100%',
                            marginTop: 2,
                            color: (r.yesterdayIncome ?? 0) > 0 ? '#dc2626' : (r.yesterdayIncome ?? 0) < 0 ? '#16a34a' : undefined,
                          }}
                        />
                      </Col>
                      <Col span={12}>
                        <div style={{ fontSize: 11, color: '#64748b' }}>持有收益 (元)</div>
                        <InputNumber
                          value={r.holdingProfit}
                          precision={2}
                          onChange={(v) => updateRow(r.key, { holdingProfit: v ?? 0 })}
                          style={{
                            width: '100%',
                            marginTop: 2,
                            color: (r.holdingProfit ?? 0) > 0 ? '#dc2626' : (r.holdingProfit ?? 0) < 0 ? '#16a34a' : undefined,
                          }}
                        />
                      </Col>
                      <Col span={12}>
                        <div style={{ fontSize: 11, color: '#64748b' }}>收益率 (%)</div>
                        <InputNumber
                          value={r.holdingProfitRate}
                          precision={2}
                          onChange={(v) => updateRow(r.key, { holdingProfitRate: v ?? 0 })}
                          style={{
                            width: '100%',
                            marginTop: 2,
                            color: (r.holdingProfitRate ?? 0) > 0 ? '#dc2626' : (r.holdingProfitRate ?? 0) < 0 ? '#16a34a' : undefined,
                          }}
                        />
                      </Col>
                    </Row>
                  </Card>
                ))}
              </Space>
            </div>
          ) : (
            /* PC 端紧凑表格视图 */
            <Table
              dataSource={rows}
              pagination={false}
              size="small"
              scroll={{ y: 380 }}
              columns={[
                {
                  title: '',
                  key: 'select',
                  width: 42,
                  align: 'center',
                  render: (_, r) => (
                    <Checkbox
                      checked={r.selected}
                      onChange={(e) => updateRow(r.key, { selected: e.target.checked })}
                    />
                  ),
                },
                {
                  title: '原图识别名称',
                  dataIndex: 'fundName',
                  key: 'fundName',
                  width: 140,
                  render: (val, r) => (
                    <div>
                      <div style={{ fontWeight: 600, color: '#0f172a', fontSize: 13 }}>{val}</div>
                      {r.warning && (
                        <div style={{ fontSize: 11, color: '#d97706', marginTop: 2 }}>
                          <ExclamationCircleOutlined /> {r.warning}
                        </div>
                      )}
                    </div>
                  ),
                },
                {
                  title: '匹配代码与份额类别',
                  dataIndex: 'fundCode',
                  key: 'fundCode',
                  width: 250,
                  render: (_, r) => renderCodeSelector(r),
                },
                {
                  title: '持仓金额',
                  dataIndex: 'holdingAmount',
                  key: 'holdingAmount',
                  width: 110,
                  render: (val, r) => (
                    <InputNumber
                      value={val}
                      min={0.01}
                      precision={2}
                      onChange={(newVal) => updateRow(r.key, { holdingAmount: newVal ?? 0 })}
                      style={{ width: '100%', fontFamily: 'monospace' }}
                    />
                  ),
                },
                {
                  title: '昨日收益',
                  dataIndex: 'yesterdayIncome',
                  key: 'yesterdayIncome',
                  width: 95,
                  render: (val, r) => (
                    <InputNumber
                      value={val}
                      precision={2}
                      onChange={(newVal) => updateRow(r.key, { yesterdayIncome: newVal ?? 0 })}
                      style={{
                        width: '100%',
                        fontFamily: 'monospace',
                        color: (val ?? 0) > 0 ? '#dc2626' : (val ?? 0) < 0 ? '#16a34a' : undefined,
                      }}
                    />
                  ),
                },
                {
                  title: '持有收益',
                  dataIndex: 'holdingProfit',
                  key: 'holdingProfit',
                  width: 95,
                  render: (val, r) => (
                    <InputNumber
                      value={val}
                      precision={2}
                      onChange={(newVal) => updateRow(r.key, { holdingProfit: newVal ?? 0 })}
                      style={{
                        width: '100%',
                        fontFamily: 'monospace',
                        color: (val ?? 0) > 0 ? '#dc2626' : (val ?? 0) < 0 ? '#16a34a' : undefined,
                      }}
                    />
                  ),
                },
                {
                  title: '收益率(%)',
                  dataIndex: 'holdingProfitRate',
                  key: 'holdingProfitRate',
                  width: 85,
                  render: (val, r) => (
                    <InputNumber
                      value={val}
                      precision={2}
                      onChange={(newVal) => updateRow(r.key, { holdingProfitRate: newVal ?? 0 })}
                      style={{
                        width: '100%',
                        fontFamily: 'monospace',
                        color: (val ?? 0) > 0 ? '#dc2626' : (val ?? 0) < 0 ? '#16a34a' : undefined,
                      }}
                    />
                  ),
                },
                {
                  title: '',
                  key: 'action',
                  width: 44,
                  align: 'center',
                  render: (_, r) => (
                    <Button
                      size="small"
                      type="text"
                      danger
                      icon={<DeleteOutlined />}
                      onClick={() => setRows((prev) => prev.filter((item) => item.key !== r.key))}
                    />
                  ),
                },
              ]}
            />
          )}
        </div>
      )}
    </Modal>
  )
}
