import request from '/src/utils/request'

/**
 * 获取 Top N 热点话题（带综合评分，走 Spring Boot → HotScoreCalculator）
 * @param {Object} params - { topN, partition }
 */
export const getHotTopics = (params = {}) => {
    return request.get('/user/hot/topics', {
        params: {
            topN: params.topN || 10,
            partition: params.partition || 'all'
        }
    })
}

/**
 * 手动触发热榜同步（立刻从B站拉取最新热榜数据落库）
 */
export const syncHotTopics = () => {
    return request.post('/user/hot/sync')
}

/**
 * 获取B站热榜原始数据（直接调用Python AI服务）
 * @param {Object} params - { limit }
 */
export const getBilibiliHot = (params = {}) => {
    const limit = params.limit || 20
    return fetch(`http://localhost:8001/api/bilibili/hot?hot_type=hot&limit=${limit}`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' }
    }).then(response => {
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`)
        }
        return response.json()
    })
}

/**
 * 格式化热度数值
 */
export const formatHotValue = (value) => {
    if (value == null) return '0'
    if (value >= 1000000) {
        return (value / 1000000).toFixed(1) + '万'
    } else if (value >= 10000) {
        return (value / 10000).toFixed(1) + '万'
    }
    return value.toString()
}
