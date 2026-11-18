import { get, del } from '@/utils/request'

/** AI 智能体接口 */

export interface ChatSession {
  id: number
  title: string
  createTime: string
}

export interface ChatMessage {
  id?: number
  role: 'user' | 'assistant'
  content: string
}

export function getSessions() {
  return get<ChatSession[]>('/ai/sessions')
}

export function getMessages(sessionId: number) {
  return get<ChatMessage[]>(`/ai/sessions/${sessionId}/messages`)
}

export function deleteSession(id: number) {
  return del<void>(`/ai/sessions/${id}`)
}

/** 个性化推荐（非流式，返回 Markdown 文本） */
export function getRecommend() {
  return get<string>('/ai/recommend')
}

/**
 * AI 流式对话：POST /api/ai/chat 为 SSE，
 * EventSource 不支持 POST，用 fetch + ReadableStream 手动解析
 *
 * @param onSession 新建会话时回调 sessionId
 * @param onDelta 每收到一个增量片段回调（打字机效果）
 */
export async function chatStream(
  sessionId: number | null,
  content: string,
  onSession: (id: number) => void,
  onDelta: (delta: string) => void,
): Promise<void> {
  const token = localStorage.getItem('token') || ''
  const res = await fetch('/api/ai/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ sessionId, content }),
  })

  if (!res.ok || !res.body) {
    throw new Error('AI 服务连接失败')
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  // 解析 SSE：事件由空行分隔，每段可能包含 event:/data: 两行
  for (;;) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })

    const blocks = buffer.split('\n\n')
    buffer = blocks.pop() ?? ''
    for (const block of blocks) {
      let eventName = ''
      let data = ''
      for (const line of block.split('\n')) {
        if (line.startsWith('event:')) eventName = line.slice(6).trim()
        else if (line.startsWith('data:')) data += line.slice(5).trim()
      }
      if (!data) continue

      if (eventName === 'session') {
        try {
          onSession(JSON.parse(data).sessionId)
        } catch {
          /* 忽略解析失败 */
        }
      } else if (data === '[DONE]') {
        return
      } else {
        try {
          onDelta(JSON.parse(data).delta)
        } catch {
          /* 忽略解析失败 */
        }
      }
    }
  }
}
