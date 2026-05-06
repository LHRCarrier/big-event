<script setup>
import { ref } from 'vue'
import { ElMessage,ElMessageBox } from 'element-plus';
const changePasswordModel = ref({
    oldPassword: "",
    password: "",
    confirmNewPassword: ""
})
//修改密码
/** TODO
 * 后续需要加更多的身份验证
 */
import { changeUserPasswordService } from '../../api/user';
const updatePassword = async() =>{
    let result = await changeUserPasswordService(changePasswordModel.value)
    console.log("接收到的passwordModel:",changePasswordModel.value)
    if(changePasswordModel.value.password !== changePasswordModel.value.confirmNewPassword){
        ElMessage.warning("两次输入密码不一致!")
    }
    if(result.code === 0){
        ElMessage.success(result.msg ? reset.msg : "修改成功!")
    }else{
        ElMessage.error(result.msg ? result.msg : "服务异常，密码修改失败!")
    }
    //清空数据模型
    resetDataModel()
}
const resetDataModel = () =>{
    changePasswordModel.value={
        oldPassword:'',
        password:'',
        confirmNewPassword:''
    }
}
</script>
<template>
    <el-card class="page-container">
        <template #header>
            <div class="header">
                <span>安全信息</span>
            </div>
        </template>
        <!-- 使用 justify="center" 使整行内容水平居中 -->
        <el-row justify="center">
            <!-- 响应式列宽：不同屏幕下合适的宽度，让表单内容居中且不显得过宽 -->
            <el-col :xs="20" :sm="16" :md="12" :lg="10">
                <el-form 
                    :model="changePasswordModel" 
                    label-width="140px" 
                    size="large" 
                    class="centered-form"
                >
                    <el-form-item label="输入原密码" prop="oldPassword">
                        <el-input v-model="changePasswordModel.oldPassword" type="password" placeholder="请输入原密码"></el-input>
                    </el-form-item>
                    <el-form-item label="输入更新密码" prop="password">
                        <el-input v-model="changePasswordModel.password" type="password" placeholder="请输入新密码"></el-input>
                    </el-form-item>
                    <el-form-item label="请再次确认输入密码" prop="confirmNewPassword">
                        <el-input v-model="changePasswordModel.confirmNewPassword" type="password" placeholder="请再次输入新密码"></el-input>
                    </el-form-item>
                    <!-- 添加自定义类，用于让按钮居中 -->
                    <el-form-item class="submit-button-item">
                        <el-button type="primary" @click="updatePassword">提交修改</el-button>
                    </el-form-item>
                </el-form>
            </el-col>
        </el-row>
    </el-card>
</template>

<style scoped>
/* 卡片整体样式，可选 */
.page-container {
    width: 100%;
}

/* 表单容器：限制最大宽度，并自动水平居中 */
.centered-form {
    width: 100%;
    max-width: 480px;
    margin: 0 auto;
}

/* 按钮所在的表单项：让按钮在容器内水平居中 */
.submit-button-item :deep(.el-form-item__content) {
    justify-content: center;
}

/* 移动端适配：小屏幕下取消最大宽度限制，让表单充分利用宽度 */
@media (max-width: 768px) {
    .centered-form {
        max-width: 100%;
    }
}
</style>