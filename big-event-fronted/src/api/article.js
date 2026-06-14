import request from '/src/utils/request'

export const getArticleListService = (listQuery) =>{
    return request.get('/user/article/page',{params:listQuery})
}
export const getArticleDetailService = (id) =>{
    return request.get('/user/article/detail',{params:{id}})
}
/**
 * 添加文章 */
export const addArticlesService = (articleModel) =>{
    return request.post('/user/article/add',articleModel)
}
/**
 * 删除文章
 */
export const deleteArticleService = (id) =>{
    return request.delete(`/user/article/delete?id=${id}`)
}
/**
 * 编辑文章
 */
export const updateArticlesService = (articleModel) =>{
    console.log("updateArticlesService 接收到的数据:", articleModel)
    return request.put('/user/article/update',articleModel)
}