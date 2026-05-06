import request from '/src/utils/request'
import { da } from 'element-plus/es/locale/index.mjs'
import { useUserTokenStore } from '/src/stores/token.js'
export function getArticleCategoryListService(listQuery){
    return request.get("/user/category/page", { params: listQuery })
}
//添加文章分类
export const articleCategoryAddService = (categoryModel) => {
    return request.post('/user/category/add', categoryModel)
}

export const editArticleCategoryInfoService = (categoryModel) =>{
    return request.put('/user/category/update',categoryModel)
}
export const deleteArticleCategoryService = (id) =>{
    // return request.delete('/user/category/delete?id'+id)
    return request.delete(`/user/category/delete?id=${id}`)
}
