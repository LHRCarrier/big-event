import request from '/src/utils/request'

export const getKnowledgeListService = (query) => {
    return request.get('/user/knowledge', { params: query })
}

export const getKnowledgeDetailService = (id) => {
    return request.get(`/user/knowledge/${id}`)
}

export const addKnowledgeService = (model) => {
    return request.post('/user/knowledge', model)
}

export const updateKnowledgeService = (id, model) => {
    return request.put(`/user/knowledge/${id}`, model)
}

export const deleteKnowledgeService = (id) => {
    return request.delete(`/user/knowledge/${id}`)
}

export const toggleKnowledgeStatusService = (id) => {
    return request.patch(`/user/knowledge/${id}/status`)
}
