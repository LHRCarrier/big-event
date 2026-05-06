<script lang="ts" setup>
import { reactive, ref } from 'vue'
import { User, Lock } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
const isRegister = ref(false)

const registerData = ref({
    username: '',
    password: '',
    confirmPassword: '',
})
//注册校验密码函数
const checkRePassword = (rule, value, callback) => {
    if (value === '') {
        callback(new Error("请再次输入密码"))
    } else if (value !== registerData.value.password) {
        callback(new Error("两次输入密码不一致!"))
    } else {
        callback()
    }
}
//定义表单校验规则
const rules = {
    password: [
        { required: true, message: "请输入密码", trigger: "blur" },
        { min: 3, max: 16, message: "密码长度必须在3-16个字符之间", trigger: "blur" },
    ],
    confirmPassword: [
        { required: true, message: "请再次输入密码", trigger: "blur" },
        { validator: checkRePassword, trigger: "blur" }
    ],
    username: [
        { required: true, message: "请输入用户名", trigger: "blur" },
        { min: 3, max: 16, message: "用户名长度必须在3-16个字符之间", trigger: "blur" },
    ],
}
//token管理
import { useUserTokenStore } from '../stores/token'
const tokenStore = useUserTokenStore()
//注册
import { userRegisterService, userLoginService } from '/src/api/user.js'
import { useRouter } from 'vue-router'
const router = useRouter()
const handleRegister = async () => {
    let result = await userRegisterService(registerData.value)
    // if(result.code === 0){
    //     alert(result.msg ? result.msg : "注册成功！")
    //     isRegister.value = false
    // }else{
    //     alert(result.msg ? result.msg : "注册失败！")
    // }
    ElMessage.success(result.msg ? result.msg : "注册成功！")
}
//登录
const handleLogin = async () => {
    let result = await userLoginService(registerData.value)
    console.log('完整登录响应:', result)
    console.log('将要存储的 token 值:', result.data.data)  //看这一行打印的是什么
    //保存token
    tokenStore.setToken(result.data.token)
    console.log('存储后的 tokenStore.token:', tokenStore.token) // 立即读取看看
    if(result.code === 0){
        ElMessage.success(result.msg ? result.msg : "登录成功！")
    }else{
        ElMessage.error(result.msg ? result.msg : "登录失败! ")
    }
    
    //跳转到首页,借助路由完成跳转
    router.push('/')
}
//定义函数，清空数据模型的数据
const clearRegisterData = () => {
    registerData.value.username = ''
    registerData.value.password = ''
    registerData.value.confirmPassword = ''
}

</script>
<template>
    <el-row class="enter-page">
        <el-col :span="12" class="bg"></el-col>
        <el-col :span="8" :offset="2" class="form">
            <!-- 注册表单 -->
            <el-form ref="form" size="large" aria-autocomplete="on" v-if="isRegister" :model="registerData"
                :rules="rules">
                <el-form-item>
                    <h1>注册</h1>
                </el-form-item>

                <el-form-item prop="username">
                    <el-input :prefix-icon="User" placeholder="请输入用户名" v-model="registerData.username" />
                </el-form-item>
                <el-form-item prop="password">
                    <el-input :prefix-icon="Lock" placeholder="请输入密码" type="password" v-model="registerData.password" />
                </el-form-item>
                <el-form-item prop="confirmPassword">
                    <el-input :prefix-icon="Lock" placeholder="请再次输入密码" type="password"
                        v-model="registerData.confirmPassword" />
                </el-form-item>
                <el-form-item>
                    <!-- 注册按钮 -->
                    <el-button class="button" type="primary" auto-inset-space @click="handleRegister">注册</el-button>
                </el-form-item>
                <el-form-item class="flex">
                    <el-link type="info" :underline="false"
                        @click="isRegister = false; clearRegisterData()">已有账号？登录</el-link>
                </el-form-item>
            </el-form>
            <!-- 登录表单 -->
            <el-form ref="form" size="large" autocomplete="off" v-else :model="registerData" :rules="rules">
                <el-form-item>
                    <h1>Login</h1>
                </el-form-item>
                <el-form-item label="Username" prop="username">
                    <el-input :prefix-icon="User" placeholder="请输入用户名" v-model="registerData.username" />
                </el-form-item>
                <el-form-item label="Password" prop="password">
                    <el-input :prefix-icon="Lock" placeholder="请输入密码" type="password" v-model="registerData.password" />
                </el-form-item>
                <el-form-item>
                    <!-- 登录按钮 -->
                    <el-button class="button" type="primary" auto-inset-space @click="handleLogin">登录</el-button>
                </el-form-item>
                <el-form-item class="flex">
                    <el-link type="info" :underline="false"
                        @click="isRegister = true; clearRegisterData()">没有账号？注册</el-link>
                </el-form-item>

            </el-form>
        </el-col>
    </el-row>
</template>
<style lang="scss" scoped>
/* 样式 */
.enter-page {
    height: 100vh;
    background-color: #ffffff;

    .bg {
        border-radius: 0 20px 20px 0;
        /* 半透明遮罩层*/
        background-image:
            // linear-gradient(rgba(255, 255, 255, 1)),

            url('src/assets/logo2.png'),
            url('src/assets/login_bg.jpg');
        background-repeat: no-repeat, no-repeat;
        background-position: 60% center, center;
        background-size: 240px auto, cover;
    }

    .form {
        display: flex;
        flex-direction: column;
        justify-content: center;
        user-select: none;

        .title {
            margin: 0 auto;
        }

        .button {
            width: 100%;
        }

        .flex {
            width: 100%;
            display: flex;
            justify-content: space-between;
        }
    }
}
</style>