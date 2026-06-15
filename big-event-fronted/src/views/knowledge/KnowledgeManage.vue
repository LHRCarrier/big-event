<script setup>
import { Edit, Delete, View, Search } from '@element-plus/icons-vue'
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
    getKnowledgeListService,
    addKnowledgeService,
    updateKnowledgeService,
    deleteKnowledgeService,
    toggleKnowledgeStatusService,
    getKnowledgeDetailService
} from '../../api/knowledge'

const articles = ref([])

const query = ref({
    page: 1,
    pageSize: 20,
    category: null,
    keyword: null,
    quality: null,
    status: null
})

const total = ref(0)

const getArticles = async () => {
    let result = await getKnowledgeListService(query.value)
    articles.value = result.data.records
    total.value = result.data.total
    ElMessage.success('获取知识库列表成功')
}

const dialogVisible = ref(false)
const detailVisible = ref(false)
const title = ref('')
const currentArticle = ref({})

const categoryModel = ref({
    id: null,
    title: '',
    content: '',
    excerpt: '',
    category: '',
    tags: '',
    author: '',
    sourceUrl: '',
    quality: 3
})

const rules = {
    title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
    content: [{ required: true, message: '请输入文章内容', trigger: 'blur' }],
    category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

const categoryOptions = [
    { label: '科技', value: '科技' },
    { label: '社会', value: '社会' },
    { label: '娱乐', value: '娱乐' },
    { label: '教育', value: '教育' },
    { label: '生活', value: '生活' },
    { label: '体育', value: '体育' },
    { label: '财经', value: '财经' },
    { label: '游戏', value: '游戏' },
    { label: '知识', value: '知识' },
    { label: '影视', value: '影视' }
]

const openAddDialog = () => {
    title.value = '添加知识库文章'
    categoryModel.value = { id: null, title: '', content: '', excerpt: '', category: '', tags: '', author: '', sourceUrl: '', quality: 3 }
    dialogVisible.value = true
}

const openEditDialog = (row) => {
    title.value = '编辑知识库文章'
    categoryModel.value = {
        id: row.id,
        title: row.title,
        content: row.content,
        excerpt: row.excerpt,
        category: row.category,
        tags: row.tags,
        author: row.author,
        sourceUrl: row.sourceUrl,
        quality: row.quality
    }
    dialogVisible.value = true
}

const submitForm = async () => {
    if (categoryModel.value.id) {
        await updateKnowledgeService(categoryModel.value.id, categoryModel.value)
        ElMessage.success('修改成功')
    } else {
        await addKnowledgeService(categoryModel.value)
        ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    getArticles()
}

const handleDelete = (row) => {
    ElMessageBox.confirm('确认删除该知识库文章吗？', '提示', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(async () => {
        await deleteKnowledgeService(row.id)
        ElMessage.success('删除成功')
        getArticles()
    }).catch(() => {
        ElMessage.info('取消删除')
    })
}

const handleToggleStatus = async (row) => {
    await toggleKnowledgeStatusService(row.id)
    ElMessage.success('状态切换成功')
    getArticles()
}

const openDetail = async (row) => {
    let result = await getKnowledgeDetailService(row.id)
    currentArticle.value = result.data
    detailVisible.value = true
}

const handlePageChange = (page) => {
    query.value.page = page
    getArticles()
}

const handleSizeChange = (size) => {
    query.value.pageSize = size
    query.value.page = 1
    getArticles()
}

onMounted(() => {
    getArticles()
})
</script>

<template>
    <el-card class="page-container">
        <template #header>
            <div class="header">
                <span>知识库管理</span>
                <div class="extra">
                    <el-button type="primary" @click="openAddDialog">添加文章</el-button>
                </div>
            </div>
        </template>

        <!-- 搜索栏 -->
        <el-form :model="query" inline class="search-form">
            <el-form-item label="关键词">
                <el-input v-model="query.keyword" placeholder="搜索标题/标签/摘要" clearable @clear="getArticles" />
            </el-form-item>
            <el-form-item label="分类">
                <el-select v-model="query.category" placeholder="全部分类" clearable @change="getArticles">
                    <el-option v-for="opt in categoryOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
            </el-form-item>
            <el-form-item label="质量">
                <el-select v-model="query.quality" placeholder="全部" clearable @change="getArticles" style="width: 80px">
                    <el-option label="5星" :value="5" />
                    <el-option label="4星" :value="4" />
                    <el-option label="3星" :value="3" />
                    <el-option label="2星" :value="2" />
                    <el-option label="1星" :value="1" />
                </el-select>
            </el-form-item>
            <el-form-item>
                <el-button type="primary" :icon="Search" @click="getArticles">搜索</el-button>
            </el-form-item>
        </el-form>

        <!-- 表格 -->
        <el-table :data="articles" style="width: 100%">
            <el-table-column label="序号" width="80" type="index" />
            <el-table-column label="标题" prop="title" min-width="200" show-overflow-tooltip />
            <el-table-column label="分类" prop="category" width="80" />
            <el-table-column label="标签" prop="tags" min-width="150" show-overflow-tooltip />
            <el-table-column label="质量" width="80">
                <template #default="{ row }">
                    <span>{{ '★'.repeat(row.quality) }}{{ '☆'.repeat(5 - row.quality) }}</span>
                </template>
            </el-table-column>
            <el-table-column label="字数" prop="wordCount" width="80" />
            <el-table-column label="状态" width="80">
                <template #default="{ row }">
                    <el-switch :model-value="row.status === 1" @change="handleToggleStatus(row)" />
                </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
                <template #default="{ row }">
                    <el-button :icon="View" circle plain type="info" @click="openDetail(row)" />
                    <el-button :icon="Edit" circle plain type="primary" @click="openEditDialog(row)" />
                    <el-button :icon="Delete" circle plain type="danger" @click="handleDelete(row)" />
                </template>
            </el-table-column>
            <template #empty>
                <el-empty description="知识库为空，请添加参考文章" />
            </template>
        </el-table>

        <!-- 分页 -->
        <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
            class="pagination"
        />

        <!-- 添加/编辑弹窗 -->
        <el-dialog v-model="dialogVisible" :title="title" width="60%" destroy-on-close>
            <el-form :model="categoryModel" :rules="rules" label-width="80px" style="padding-right: 20px">
                <el-row :gutter="20">
                    <el-col :span="16">
                        <el-form-item label="标题" prop="title">
                            <el-input v-model="categoryModel.title" placeholder="文章标题" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="8">
                        <el-form-item label="分类" prop="category">
                            <el-select v-model="categoryModel.category" placeholder="选择分类" style="width: 100%">
                                <el-option v-for="opt in categoryOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                            </el-select>
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row :gutter="20">
                    <el-col :span="12">
                        <el-form-item label="标签">
                            <el-input v-model="categoryModel.tags" placeholder="逗号分隔，如：深度分析,技术,AI" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="6">
                        <el-form-item label="原作者">
                            <el-input v-model="categoryModel.author" placeholder="原作者名" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="6">
                        <el-form-item label="质量">
                            <el-rate v-model="categoryModel.quality" :max="5" />
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-form-item label="摘要">
                    <el-input v-model="categoryModel.excerpt" type="textarea" :rows="2" placeholder="简短摘要（可选）" />
                </el-form-item>
                <el-form-item label="来源链接">
                    <el-input v-model="categoryModel.sourceUrl" placeholder="原文链接（可选）" />
                </el-form-item>
                <el-form-item label="正文" prop="content">
                    <el-input v-model="categoryModel.content" type="textarea" :rows="12" placeholder="粘贴文章全文（Markdown格式）" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" @click="submitForm">确认</el-button>
            </template>
        </el-dialog>

        <!-- 详情弹窗 -->
        <el-dialog v-model="detailVisible" title="文章详情" width="60%" destroy-on-close>
            <div v-if="currentArticle" class="article-detail">
                <div class="detail-meta">
                    <el-tag>{{ currentArticle.category }}</el-tag>
                    <span class="detail-author" v-if="currentArticle.author">作者：{{ currentArticle.author }}</span>
                    <span>字数：{{ currentArticle.wordCount }}</span>
                    <span>质量：{{ '★'.repeat(currentArticle.quality || 0) }}</span>
                </div>
                <div class="detail-content" v-html="currentArticle.content" style="white-space: pre-wrap;"></div>
            </div>
        </el-dialog>
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
}

.search-form {
    margin-bottom: 16px;
}

.pagination {
    margin-top: 16px;
    justify-content: flex-end;
}

.article-detail {
    .detail-meta {
        display: flex;
        gap: 16px;
        align-items: center;
        margin-bottom: 16px;
        color: #909399;
        font-size: 14px;
    }

    .detail-content {
        max-height: 500px;
        overflow-y: auto;
        padding: 16px;
        background: #f5f7fa;
        border-radius: 4px;
        line-height: 1.8;
    }
}
</style>
