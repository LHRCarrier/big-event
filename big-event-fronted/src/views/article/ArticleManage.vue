<script setup>
import {
    Edit,
    Delete
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { ref, onMounted, computed } from 'vue'
import { getArticleCategoryListService } from '../../api/article-category'
import { getArticleListService, addArticlesService, deleteArticleService, updateArticlesService } from '../../api/article'
//分页条数据模型
const pageNum = ref(1)//当前页
const total = ref(20)//总条数
const pageSize = ref(5)//每页条数

//当每页条数发生了变化，调用此函数
const onSizeChange = (size) => {
    pageSize.value = size
    getArticles()
}
//当前页码发生变化，调用此函数
const onCurrentChange = (num) => {
    pageNum.value = num
    getArticles()
}

const queryCategory = computed(() => (
    {
        page: pageNum.value,
        pageSize: pageSize.value,
        categoryName: null,
        categoryAlias: null,
        updateTime: null,
        createTime: null
    }
))
const queryArticle = computed(() => (
    {
        page: pageNum.value,
        pageSize: pageSize.value,
        categoryId: categoryId.value || null,
        state: state.value || null,
        title: null,
        updateTime: null,
        createTime: null
    }
))
//回显文章分类
const getCategorys = async () => {
    let result = await getArticleCategoryListService(queryCategory.value)
    categorys.value = result.data.records
}
//文章分类数据模型
const categorys = ref([
    {
        "id": '',
        "categoryName": '',
        "categoryAlias": '',
        "createTime": '',
        "updateTime": ''
    }

])

//用户搜索时选中的分类id
const categoryId = ref('')

//用户搜索时选中的发布状态
const state = ref('')

//文章列表数据模型
const articles = ref([
    {
        "id": '',
        "title": "",
        "content": "",
        "coverImg": "",
        "state": "",
        "categoryId": "",
        "categoryName": "",
        "createTime": "",
        "updateTime": ""
    }

])
// 添加一个计算属性来获取文章的分类名称
const getCategoryName = (categoryId) => {
    const category = categorys.value.find(cat => String(cat.id) === String(categoryId))
    //.find() 是数组的高阶方法，用于查找数组中符合条件的第一个元素
    // cat 是临时变量名，表示当前遍历的数组元素（代表单个分类对象）
    return category ? category.categoryName : '未分类'
}
//显示文章列表
const getArticles = async () => {
    let result = await getArticleListService(queryArticle.value)
    console.log('后端返回的完整文章数据:', result)
    total.value = result.data.total
    articles.value = result.data.records
    console.log('后端返回的完整文章数据1:', articles.value)
}
//删除文章
import { ElMessageBox } from 'element-plus';
const deleteArticles = (row) => {
    ElMessageBox.confirm(
        '你确认删除该文章信息吗？',
        '温馨提示',
        {
            confirmButtonText: '确认',
            cancelButtonText: '取消',
            type: 'warning',
        }
    )
        .then(async () => {
            //用户点击了确认
            let result = await deleteArticleService(row.id)
            ElMessage.success(result.message ? result.message : '删除成功')
            //再次调用getAllCategory，获取所有文章分类
            getArticles()
        })
        .catch((error) => {
            console.log('catch 捕获到的错误对象:', error)
            console.log('错误类型:', typeof error)
            console.log('错误值:', error)
            // 只有用户点击取消或关闭对话框才会进入这里
            //用户点击了取消
            ElMessage({
                type: 'info',
                message: '取消删除',
            })
        })
}
const openAddDrawer = () => {
    title.value = '添加文章'
    articleModel.value.id = ''  // 清空 id，确保是新增模式
    resetForm()  // 重置所有表单数据
    visibleDrawer.value = true
}

// 打开编辑文章抽屉
const openEditDrawer = (row) => {
    title.value = '修改文章'
    visibleDrawer.value = true

    // 回显数据
    articleModel.value = {
        id: row.id,
        title: row.title || '',
        coverImg: row.coverImg || '',
        content: row.content || '',
        state: row.state || '',
        categoryId: row.categoryId || ''
    }

    // 设置预览图（显示原有封面）
    preview.value = row.coverImg || ''
    pendingFile.value = null  // 编辑时没有新文件
}
//更新文章回显
const title = ref('')
const pendingFile = ref(null)

//添加文章
import { Plus } from '@element-plus/icons-vue'

//添加表单数据模型
const articleModel = ref({
    id: '',
    title: '',
    categoryId: '',
    coverImg: '',
    content: '',
    state: ''
})
//重置函数
const resetSearch = async () => {
    categoryId.value = ''
    state.value = ''
    getArticles()
}

// 添加搜索加载状态
const searchLoading = ref(false)

// 自动搜索方法
const handleAutoSearch = () => {
    // 重置到第一页
    pageNum.value = 1
    // 执行搜索
    getArticles()
}
//富文本编辑器
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
//文章添加模块及文件上传模块
//添加文章函数
import { uploadFileService } from '../../api/common'
import { ar } from 'element-plus/es/locale/index.mjs'
// 控制抽屉显示
const visibleDrawer = ref(false)
const submitLoading = ref(false)
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
// 添加一个变量保存本地预览的文件
//const pendingFile = ref(null)  // 暂存待上传的文件
const preview = ref('')
// 文件改变时的处理
const handleFileChange = (file) => {
    // 显示本地预览

    if (file.raw) {
        pendingFile.value = file.raw
        console.log("pending预备", pendingFile.value)
        const reader = new FileReader()
        reader.onload = (e) => {
            preview.value = e.target.result
        }

        reader.readAsDataURL(file.raw)
    }
}

// 提交文章
const submitArticle = async (state) => {
    // 表单验证
    if (!articleModel.value.title.trim()) {
        ElMessage.warning('请输入文章标题')
        return
    }

    if (!articleModel.value.categoryId) {
        ElMessage.warning('请选择文章分类')
        return
    }

    if (!pendingFile.value && !articleModel.value.id) {
        ElMessage.warning('请上传文章封面')
        return
    }

    if (!articleModel.value.content || articleModel.value.content === '<p><br></p>') {
        ElMessage.warning('请输入文章内容')
        return
    }

    submitLoading.value = true

    try {
        // 1. 处理封面图片
        let coverImgUrl = articleModel.value.coverImg || '' // 默认使用原有封面
        console.log("上传前检查pendingFile.value是否为空", pendingFile.value)
        //只有选择了新文件时才上传
        if (pendingFile.value && pendingFile.value instanceof File) {
            const formData = new FormData()
            formData.append('file', pendingFile.value)
            console.log("上传的数据formData", formData.get('file'))
            //这里也正常显示图片url
            const uploadResult = await uploadFileService(formData)//在这里调用后端上传文件接口时报错，说imageUrl传递为null
            coverImgUrl = uploadResult.data  // 保存上传返回的URL
            console.log("封面上传成功，URL:", coverImgUrl)
            pendingFile.value = null
            preview.value = ''  // 清空预览
        }
        else if (!articleModel.value.id && !pendingFile.value) {
            // 新增模式且没有文件
            ElMessage.warning('请上传文章封面')
            submitLoading.value = false
            return
        }
        // 2. 准备提交的数据（使用上传得到的URL）
        const submitData = {
            title: articleModel.value.title,
            content: articleModel.value.content,
            categoryId: Number(articleModel.value.categoryId),
            state: state === 'published' ? '已发布' : '草稿',
            coverImg: coverImgUrl  // 使用上传后的URL
        }
        console.log("提交信息submitData:", submitData)
        // 3. 调用添加或编辑文章接口
        let result
        if (articleModel.value.id) {
            // 有 id 表示编辑
            submitData.id = articleModel.value.id
            result = await updateArticlesService(submitData)

        } else {
            // 无 id 表示新增
            result = await addArticlesService(submitData)
        }
        if (result.code === 0) {
            ElMessage.success(state === 'published' ? '发布成功！' : '保存草稿成功！')
            visibleDrawer.value = false
            resetForm()
            await getArticles()
        } else {
            ElMessage.error(result.message || '操作失败')
        }
    } catch (error) {
        console.error('提交失败:', error)
        ElMessage.error('操作失败，请重试！')
    } finally {
        submitLoading.value = false
    }
}
const editorKey = ref(0)
const resetForm = () => {
    // 重置文章数据
    articleModel.value = {
        id: '',
        title: '',
        content: '',
        categoryId: '',
        coverImg: '',
        state: ''
    }


    // 清空预览图
    preview.value = ''

    // 清空待上传的文件
    pendingFile.value = null
    editorKey.value++
}
const handleDrawerClose = () => {
    // 抽屉关闭时重置表单
    resetForm()
    // 如果有必要，可以重置其他状态
    pendingFile.value = null
    preview.value = ''
}
onMounted(async () => {
    await getCategorys()
    await getArticles()
})
</script>
<template>
    <el-card class="page-container">
        <template #header>
            <div class="header">
                <span>文章管理</span>
                <div class="extra">
                    <el-button type="primary" @click="openAddDrawer">添加文章</el-button>
                </div>
            </div>
        </template>
        <!-- 搜索表单 -->
        <el-form inline>
            <el-form-item label="文章分类：">
                <el-select placeholder="请选择" v-model="categoryId" @change="handleAutoSearch" clearable
                    :loading="searchLoading">
                    <el-option v-for="c in categorys" :key="c.id" :label="c.categoryName" :value="c.id">
                    </el-option>
                </el-select>
            </el-form-item>

            <el-form-item label="发布状态：">
                <el-select placeholder="请选择" v-model="state" @change="handleAutoSearch" clearable
                    :loading="searchLoading">
                    <el-option label="已发布" value="已发布"></el-option>
                    <el-option label="草稿" value="草稿"></el-option>
                </el-select>
            </el-form-item>
            <el-form-item>
                <el-button type="primary" @click="getArticles">搜索</el-button>
                <el-button @click="resetSearch">重置</el-button>
            </el-form-item>
        </el-form>
        <!-- 文章列表 -->
        <el-table :data="articles" style="width: 100%">
            <el-table-column label="文章标题" width="400" prop="title"></el-table-column>
            <el-table-column label="分类"><template #default="{ row }">{{ getCategoryName(row.categoryId)
                    }}</template></el-table-column>
            <el-table-column label="发表时间" prop="createTime"> </el-table-column>
            <el-table-column label="更新时间" prop="updateTime"></el-table-column>
            <el-table-column label="状态" prop="state"></el-table-column>
            <el-table-column label="操作" width="100">
                <template #default="{ row }">

                    <el-button :icon="Edit" @click="openEditDrawer(row)" circle plain type="primary"></el-button>
                    <el-button :icon="Delete" @click="deleteArticles(row)" circle plain type="danger"></el-button>
                </template>
            </el-table-column>
            <template #empty>
                <el-empty description="没有数据" />
            </template>
        </el-table>
        <!-- 分页条 -->
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :page-sizes="[3, 5, 10, 15]"
            layout="jumper, total, sizes, prev, pager, next" background :total="total" @size-change="onSizeChange"
            @current-change="onCurrentChange" style="margin-top: 20px; justify-content: flex-end" />
        <!-- 抽屉 -->
        <!-- <el-drawer v-model="visibleDrawer" title="添加文章" direction="rtl" size="50%"> -->
        <!-- 添加文章表单 -->
        <!-- <el-form :model="articleModel" label-width="100px">
                <el-form-item label="文章标题">
                    <el-input v-model="articleModel.title" placeholder="请输入标题"></el-input>
                </el-form-item>
                <el-form-item label="文章分类">
                    <el-select placeholder="请选择" v-model="articleModel.categoryId">
                        <el-option v-for="c in categorys" :key="c.id" :label="c.categoryName" :value="c.id">
                        </el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="文章封面">

                    <el-upload class="avatar-uploader" :auto-upload="false" :show-file-list="false">
                        <img v-if="articleModel.coverImg" :src="articleModel.coverImg" class="avatar" />
                        <el-icon v-else class="avatar-uploader-icon">
                            <Plus />
                        </el-icon>
                    </el-upload>
                </el-form-item>
                <el-form-item label="文章内容">
                    <div class="editor"><quill-editor theme="snow" v-model:content="articleModel.content"
                            contentType="html">
                        </quill-editor></div>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="addArticles">发布</el-button>
                    <el-button type="info">草稿</el-button>
                </el-form-item> -->
        <!-- </el-form> -->
        <!-- </el-drawer> -->
        <!-- 抽屉 -->
        <el-drawer v-model="visibleDrawer" :title="title" direction="rtl" size="50%" @close="handleDrawerClose">
            <!-- 添加文章表单 -->
            <el-form :model="articleModel" label-width="100px">
                <el-form-item label="文章标题">
                    <el-input v-model="articleModel.title" placeholder="请输入标题"></el-input>
                </el-form-item>

                <el-form-item label="文章分类">
                    <el-select placeholder="请选择" v-model="articleModel.categoryId">
                        <el-option v-for="c in categorys" :key="c.id" :label="c.categoryName" :value="c.id">
                        </el-option>
                    </el-select>
                </el-form-item>

                <el-form-item label="文章封面">
                    <el-upload class="avatar-uploader" :auto-upload="false" :show-file-list="false"
                        :before-upload="handleBeforeUpload" :on-change="handleFileChange"
                        accept="image/jpeg,image/png,image/gif,image/jpg">
                        <img v-if="preview" :src="preview" class="avatar" />
                        <img v-else-if="articleModel.coverImg" :src="articleModel.coverImg" class="avatar" />
                        <el-icon v-else class="avatar-uploader-icon">
                            <Plus />
                        </el-icon>
                    </el-upload>
                    <div class="upload-tip">支持 jpg、png、gif 格式，大小不超过 5MB</div>
                </el-form-item>

                <el-form-item label="文章内容">
                    <div class="editor">
                        <quill-editor theme="snow" :key="editorKey" v-model:content="articleModel.content"
                            contentType="html">
                        </quill-editor>
                    </div>
                </el-form-item>

                <el-form-item>
                    <el-button type="primary" @click="submitArticle('published')"
                        :loading="submitLoading">发布</el-button>
                    <el-button type="info" @click=" submitArticle('draft')" :loading="submitLoading">草稿</el-button>
                </el-form-item>
            </el-form>
        </el-drawer>
    </el-card>

</template>
<style lang="scss" scoped>
.page-container {
    min-height: 100%;
    box-sizing: border-box;

    .header {
        display: flex;
        align-items: center;
        justify-content: space-between;
    }

    .el-select {
        --el-select-width: 220px;
    }

    /* 抽屉样式 */
    .avatar-uploader {
        :deep() {
            .avatar {
                width: 178px;
                height: 178px;
                display: block;
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
                width: 178px;
                height: 178px;
                text-align: center;
            }
        }
    }

    .editor {
        width: 100%;

        :deep(.ql-editor) {
            min-height: 200px;
        }
    }

}
</style>