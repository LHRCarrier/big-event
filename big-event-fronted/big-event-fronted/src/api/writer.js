import request from '/src/utils/request'

/**
 * AI撰稿请求
 * @param {Object} params - 撰稿参数
 * @param {string} params.topic - 话题
 * @param {number} [params.length=500] - 文章长度
 * @param {string} [params.style='neutral'] - 风格
 * @param {string} [params.audience='general'] - 受众
 * @param {boolean} [params.generateSummary=true] - 是否生成摘要
 */
export const writeArticle = (params) => {
    return request.post('/user/writer/write', params)
}

/**
 * 快速撰稿（简化版）
 * @param {string} topic - 话题
 */
export const quickWrite = (topic) => {
    return request.get('/user/writer/quick-write', { params: { topic } })
}

/**
 * 检查AI服务状态
 */
export const checkWriterStatus = () => {
    return request.get('/user/writer/status')
}

/**
 * 流式AI撰稿（实时显示）
 * 直接调用Python AI服务的流式接口
 * @param {Object} params - 撰稿参数
 * @param {Function} onChunk - 收到内容块时的回调函数
 * @param {Function} onComplete - 完成时的回调函数
 * @param {Function} onError - 出错时的回调函数
 */
export const writeArticleStream = (params, onChunk, onComplete, onError) => {
    const pythonServiceUrl = 'http://localhost:8001'

    fetch(`${pythonServiceUrl}/api/writer/write/stream`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(params)
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`)
        }

        const reader = response.body.getReader()
        const decoder = new TextDecoder('utf-8')
        let buffer = ''

        const readChunk = () => {
            reader.read().then(({ done, value }) => {
                if (done) {
                    onComplete && onComplete()
                    return
                }

                buffer += decoder.decode(value, { stream: true })

                while (buffer.includes('\n\n')) {
                    const [event, remaining] = buffer.split('\n\n', 2)
                    buffer = remaining

                    if (event.startsWith('data: ')) {
                        try {
                            const data = JSON.parse(event.slice(6))
                            if (data.content) {
                                onChunk && onChunk(data.content)
                            }
                        } catch (e) {
                            console.error('Failed to parse SSE data:', e)
                        }
                    }
                }

                readChunk()
            }).catch(error => {
                onError && onError(error)
            })
        }

        readChunk()
    })
    .catch(error => {
        onError && onError(error)
    })
}