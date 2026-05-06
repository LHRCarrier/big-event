import request from '/src/utils/request'
import { da } from 'element-plus/es/locale/index.mjs'
import { useUserTokenStore } from '/src/stores/token.js'

/**
 * 文件上传
 * @param {FormData} formData - 包含文件的表单数据
 * @returns {Promise}
 */
export const uploadFileService = (formData) => {
    return request({
        url: '/user/common/upload',
        method: 'post',
        data: formData,
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })
}
