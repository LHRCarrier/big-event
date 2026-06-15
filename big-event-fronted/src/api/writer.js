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

/**
 * 流式AI撰稿（通过Spring Boot代理 → Python AI服务SSE）
 * Spring Boot SSE 格式为: data:chunk\n\n （纯文本内容，非JSON包裹）
 */
export const writeArticleStream = (params, onChunk, onComplete, onError) => {
    const token = JSON.parse(localStorage.getItem('token') || '{}').token || ''

    fetch('/api/user/writer/write/stream', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': token ? `Bearer ${token}` : ''
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
                const lines = buffer.split('\n')
                // Keep the last potentially incomplete line in buffer
                buffer = lines.pop() || ''
                for (const line of lines) {
                    if (line.startsWith('data:')) {
                        // Spring SSE: data:content (no space after colon)
                        const chunk = line.slice(5)
                        if (chunk) {
                            onChunk && onChunk(chunk)
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

/**
 * 知识库匹配预览（直接调用Python AI服务）
 * 返回与话题匹配的高质量参考文章列表
 */
export const matchKnowledge = async (topic, limit = 5) => {
    const pythonServiceUrl = 'http://localhost:8001'
    try {
        const response = await fetch(
            `${pythonServiceUrl}/api/knowledge/match?topic=${encodeURIComponent(topic)}&limit=${limit}`
        )
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`)
        }
        return await response.json()
    } catch (error) {
        console.error('知识库匹配请求失败:', error)
        return { matched: 0, articles: [], error: error.message }
    }
}
