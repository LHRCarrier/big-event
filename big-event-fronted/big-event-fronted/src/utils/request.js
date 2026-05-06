//定制请求的实例

//导入axios  npm install axios
import axios from 'axios';
import { ElMessage } from 'element-plus'
//定义一个变量,记录公共的前缀  ,  baseURL
//const baseURL = 'http://localhost:8080';
const baseURL = '/api';
const instance = axios.create({ baseURL })

import router from '/src/router/index.js'
//添加响应拦截器
instance.interceptors.response.use(
    result => {
        //判断业务状态码
        if (result.data.code === 0) {
            return result.data;
        }
        ElMessage.error(result.data.msg ? result.data.msg : "服务异常")
        //异步操作的状态转换为失败
        return Promise.reject(result.data);
        
    },
    err => {
        if (err.response.status === 401) {
            ElMessage.error('请先登录!');
            router.push('/login')
        } else if (err.response.status === 403) {
            ElMessage.error('没有权限访问');
        } else if (err.response.status === 404) {
            ElMessage.error('资源不存在');
        } else if (err.response.status === 500) {
            ElMessage.error('服务器异常');
        } else {
            ElMessage.error('服务异常');
        }
        return Promise.reject(err);
    }
)
export default instance;

//导入token状态
import { useUserTokenStore } from '../stores/token.js'
//添加请求拦截器
instance.interceptors.request.use(

    (config) => {
        if (config.url.includes('/login') || config.url.includes('/user/login')) {
            return config  // 直接返回，不添加 Authorization
        }
        //在发送请求之前做什么
        let tokenStore = useUserTokenStore()
        //如果token中有值，在携带
        if (tokenStore.token) {
            config.headers.Authorization = `Bearer ${tokenStore.token}`
        } else {
            console.warn('请求未携带 token, 当前路径:', config.url)
        }
        return config
    },
    (err) => {
        //如果请求错误做什么
        return  Promise.reject(err)
    }
)