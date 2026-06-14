<script setup>
import { Plus, Upload } from '@element-plus/icons-vue'
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import avatar from '/src/assets/default.png'
import { uploadFileService } from '../../api/common'

const uploadRef = ref()

//读取用户信息
import { updateUserAvatarService } from '../../api/user'
import { userInfoUpdateService } from '../../api/user'
import { useUserInfoStore } from '../../stores/user'
import { useUserTokenStore } from '../../stores/token'
import { updateArticlesService } from '../../api/article'
const userInfoStore = useUserInfoStore()
const userInfo = ref({ ...userInfoStore.info })
const imgUrl = ref(userInfoStore.info.userPic)

// 暂存待上传的文件
const pendingFile = ref(null)
// 预览图
const preview = ref('')
// 上传加载状态
const uploadLoading = ref(false)

// 文件上传前的校验
const handleBeforeUpload = (file) => {
    // 检查文件类型
    const isImage = file.type.startsWith('image/')
    if (!isImage) {
        ElMessage.error('只能上传图片文件！')
        return false
    }

    // 检查文件大小（限制5MB）
    const isLt5M = file.size / 1024 / 1024 < 5
    if (!isLt5M) {
        ElMessage.error('图片大小不能超过 5MB！')
        return false
    }

    return true
}

// 文件改变时的处理
const handleFileChange = (file) => {
    // 显示本地预览
    if (file.raw) {
        pendingFile.value = file.raw
        const reader = new FileReader()
        reader.onload = (e) => {
            preview.value = e.target.result
        }
        reader.readAsDataURL(file.raw)
    }
}

// 上传头像
const uploadAvatar = async () => {
    // 检查是否有选择文件
    if (!pendingFile.value && !userInfoStore.info.userPic) {
        ElMessage.warning('请先选择图片')
        return
    }

    uploadLoading.value = true

    try {
        // 创建 FormData 对象
        const formData = new FormData()
        formData.append('file', pendingFile.value)

        // 调用上传接口
        const uploadResult = await uploadFileService(formData)

        if (uploadResult.code === 0) {
            const newAvatarUrl = uploadResult.data
            console.log("上传后的url:",newAvatarUrl)//这里测试过正常有值
            // 更新本地显示的图片
            imgUrl.value = newAvatarUrl

            // 更新用户信息中的头像
            await updateUserAvatarService(newAvatarUrl)
            userInfo.value.userPic = newAvatarUrl
            
            userInfoStore.info.userPic = newAvatarUrl
            preview.value = '' // 清空预览



            

            ElMessage.success('头像上传成功！')
            // 清空待上传的文件
            pendingFile.value = null
            await updateUserAvatarService(newAvatarUrl)
        } else {
            ElMessage.error(uploadResult.message || '上传失败')
        }
    } catch (error) {
        console.error('上传失败:', error)
        ElMessage.error('上传失败，请重试！')
    } finally {
        uploadLoading.value = false
    }
}

// 触发文件选择
const triggerFileUpload = () => {
    uploadRef.value.$el.querySelector('input').click()
}
</script>

<template>
    <el-card class="page-container">
        <template #header>
            <div class="header">
                <span>更换头像</span>
            </div>
        </template>
        <el-row>
            <el-col :span="12">
                <el-upload ref="uploadRef" class="avatar-uploader" :show-file-list="false" :auto-upload="false"
                    :before-upload="handleBeforeUpload" :on-change="handleFileChange"
                    accept="image/jpeg,image/png,image/gif,image/jpg" name="file">
                    <img v-if="preview" :src="preview" class="avatar" />
                    <img v-else-if="imgUrl" :src="imgUrl" class="avatar" />
                    <img v-else src="/src/assets/avatar.jpg" width="278" />
                </el-upload>
                <br />
                <div class="upload-tip">支持 jpg、png、gif 格式，大小不超过 5MB</div>
                <br />
                <el-button type="primary" :icon="Plus" size="large" @click="triggerFileUpload">
                    选择图片
                </el-button>
                <el-button type="success" :icon="Upload" size="large" @click="uploadAvatar" :loading="uploadLoading">
                    上传头像
                </el-button>
            </el-col>
        </el-row>
    </el-card>
</template>

<style lang="scss" scoped>
.page-container {
    .header {
        display: flex;
        align-items: center;
        justify-content: space-between;
    }
}

.avatar-uploader {
    :deep() {
        .avatar {
            width: 278px;
            height: 278px;
            display: block;
            object-fit: cover;
        }

        .el-upload {
            border: 1px dashed var(--el-border-color);
            border-radius: 6px;
            cursor: pointer;
            position: relative;
            overflow: hidden;
            transition: var(--el-transition-duration-fast);
        }

        .el-upload:hover {
            border-color: var(--el-color-primary);
        }

        .el-icon.avatar-uploader-icon {
            font-size: 28px;
            color: #8c939d;
            width: 278px;
            height: 278px;
            text-align: center;
        }
    }
}

.upload-tip {
    font-size: 12px;
    color: #8c939d;
    margin-top: 8px;
}
</style>