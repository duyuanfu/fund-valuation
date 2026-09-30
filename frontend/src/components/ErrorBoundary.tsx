import React, { Component, type ReactNode } from 'react'
import { Button, Result } from 'antd'

interface Props {
  children: ReactNode
}

interface State {
  hasError: boolean
  error: Error | null
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null,
  }

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }

  public componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('ErrorBoundary caught:', error, errorInfo)
  }

  public render() {
    if (this.state.hasError) {
      return (
        <div style={{ padding: '60px 20px', maxWidth: 600, margin: '0 auto' }}>
          <Result
            status="error"
            title="页面渲染异常"
            subTitle={this.state.error?.message || '发生未知异常，请点击刷新重试'}
            extra={[
              <Button type="primary" key="reload" onClick={() => window.location.reload()}>
                刷新页面
              </Button>,
              <Button key="home" onClick={() => (window.location.href = '/')}>
                返回自选基金
              </Button>,
            ]}
          />
        </div>
      )
    }

    return this.props.children
  }
}
