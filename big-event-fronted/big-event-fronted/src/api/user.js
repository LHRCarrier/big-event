import request from '/src/utils/request'
import { da } from 'element-plus/es/locale/index.mjs'
//调用用户注册接口函数
export const userRegisterService = (registerData)=>{
    //借助于urlSearchParams完成传递
    const params = new URLSearchParams()
    for(let key in registerData){
        params.append(key,registerData[key])
    }
    return request.post("/user/user/register",params)
}
//调用用户登录接口函数
export function userLoginService(loginData){
    const params = new URLSearchParams()
    for(let key in loginData){
        params.append(key,loginData[key])
    }
    return request.post("/user/user/login",params)
}
//获取用户个人信息
export const getUserInfoService =()=>{
    return request.get("/user/user/userInfo")
}
//修改个人信息
export const userInfoUpdateService = (userInfo)=>{
    return request.put('/user/user/update',userInfo)
}
//修改个人头像
export const updateUserAvatarService = (avatarUrl) => {
    return request.put('/user/user/updateAvatar', null, {
        params: { avatarUrl }
    })
}
//用户修改密码
export const changeUserPasswordService= (changePasswordModel)=>{
    return request.put('/user/user/updatePassword',changePasswordModel)
}