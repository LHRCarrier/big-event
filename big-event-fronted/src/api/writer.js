import request from '/src/utils/request'

/** AI撰稿 */
export const writeArticle = (params) => {
    return request.post('/user/writer/write', params)
}

/** 快速撰稿 */
export const quickWrite = (topic) => {
    return request.get('/user/writer/quick-write', { params: { topic } })
}

/** 检查AI服务状态 */
export const checkWriterStatus = () => {
    return request.get('/user/writer/status')
}

/** 基于热点数据撰稿 */
export const writeFromHot = (params) => {
    return request.post('/user/writer/write-from-hot', params)
}

/** 自动发布：评分→筛选→撰稿→入库 */
export const autoPublish = (topN = 5, partition = 'all', minScore = 60) => {
    return request.post('/user/writer/auto-publish', null, {
        params: { topN, partition, minScore }
    })
}

/** 流式AI撰稿（直接调用Python AI服务的SSE流式接口） */
export const writeArticleStream = (params, onChunk, onComplete, onError) => {
    const pythonServiceUrl = 'http://localhost:8001'

    fetch(`${pythonServiceUrl}/api/writer/write/stream`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
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
