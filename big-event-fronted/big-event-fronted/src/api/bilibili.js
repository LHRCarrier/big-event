const pythonServiceUrl = 'http://localhost:8001'

/**
 * 获取B站热榜数据
 * @param {Object} params - 请求参数
 * @param {number} [params.limit=20] - 返回数量限制
 */
export const getBilibiliHot = (params = {}) => {
    const limit = params.limit || 20
    return fetch(`${pythonServiceUrl}/api/bilibili/hot?type=bilibili&limit=${limit}`, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        }
    }).then(response => {
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`)
        }
        return response.json()
    })
}

/**
 * 格式化热度数值
 * @param {number} value - 热度值
 */
export const formatHotValue = (value) => {
    if (value >= 1000000) {
        return (value / 1000000).toFixed(1) + '万'
    } else if (value >= 10000) {
        return (value / 10000).toFixed(1) + '万'
    }
    return value.toString()
}
