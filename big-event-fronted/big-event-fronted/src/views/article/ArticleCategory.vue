<script setup>
import {
    Edit,
    Delete
} from '@element-plus/icons-vue'
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
    getArticleCategoryListService,
    articleCategoryAddService,
    editArticleCategoryInfoService,
    deleteArticleCategoryService
} from '../../api/article-category';
const categorys = ref([{
    "id": '',
    "categoryName": '',
    "categoryAlias": '',
    "createTime": '',
    "updateTime": ''
}])
const query = ref({
    "page": 1,
    "pageSize": 20,
    "categoryName": null,
    "categoryAlias": null,
    "updateTime": null,
    "createTime": null
})
const getCategorys = async () => {
    let result = await getArticleCategoryListService(query.value)
    console.log('后端返回的完整数据:', result)  // 查看数据结构
    console.log('第一条数据:', result.data.records[0])  // 查看字段名
    ElMessage.success(result.msg ? result.msg : "获取分类列表成功！")
    categorys.value = result.data.records

}
//访问后台，添加文章分类
const addCategory = async () => {
    let result = await articleCategoryAddService(categoryModel.value);
    ElMessage.success(result.message ? result.message : '添加成功!')
    //隐藏弹窗
    dialogVisible.value = false
    //再次访问后台接口，查询所有分类
    getCategorys()
}
const updateCategory = async () => {
    let result = await editArticleCategoryInfoService(categoryModel.value);
    ElMessage.success(result.message ? result.message : '修改成功!')
    //隐藏弹窗
    dialogVisible.value = false
    //重新加载数据
    getCategorys()
}
//删除分类
import { ElMessageBox } from 'element-plus';
const deleteCategory = (row) => {
    ElMessageBox.confirm(
        '你确认删除该分类信息吗？',
        '温馨提示',
        {
            confirmButtonText: '确认',
            cancelButtonText: '取消',
            type: 'warning',
        }
    )
        .then(async () => {
            //用户点击了确认
            let result = await deleteArticleCategoryService(row.id)
            ElMessage.success(result.message ? result.message : '删除成功')
            //再次调用getAllCategory，获取所有文章分类
            getCategorys()
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
//控制添加分类弹窗
const dialogVisible = ref(false)

//添加分类数据模型
const categoryModel = ref({
    categoryName: '',
    categoryAlias: ''
})
//添加分类表单校验
const rules = {
    categoryName: [
        { required: true, message: '请输入分类名称', trigger: 'blur' },
    ],
    categoryAlias: [
        { required: true, message: '请输入分类别名', trigger: 'blur' },
    ]
}
//修改分类回显
const updateCategoryEcho = (row) => {
    title.value = '修改分类'
    dialogVisible.value = true
    //将row中的数据赋值给categoryModel
    categoryModel.value.categoryName = row.categoryName
    categoryModel.value.categoryAlias = row.categoryAlias
    //修改的时候必须传递分类的id，所以扩展一个id属性
    categoryModel.value.id = row.id
}
onMounted(() => {
    getCategorys()
})
const title = ref('')
</script>
<template>
    <el-card class="page-container">
        <template #header>
            <div class="header">
                <span>文章分类</span>
                <div class="extra">
                    <el-button type="primary" @click="title = '添加分类'; dialogVisible = true">添加分类</el-button>
                </div>
            </div>
        </template>
        <el-table :data="categorys" style="width: 100%">
            <el-table-column label="序号" width="100" type="index"> </el-table-column>
            <el-table-column label="分类名称" prop="categoryName"></el-table-column>
            <el-table-column label="分类别名" prop="categoryAlias"></el-table-column>
            <el-table-column label="操作" width="100">
                <template #default="{ row }">
                    <el-button :icon="Edit" circle plain type="primary" @click="updateCategoryEcho(row)"></el-button>
                    <el-button :icon="Delete" circle plain type="danger" @click="deleteCategory(row)"></el-button>
                </template>
            </el-table-column>
            <template #empty>
                <el-empty description="没有数据" />
            </template>
        </el-table>
        <!-- 添加分类弹窗 -->
        <el-dialog v-model="dialogVisible" :title="title" width="30%">
            <el-form :model="categoryModel" :rules="rules" label-width="100px" style="padding-right: 30px">
                <el-form-item label="分类名称" prop="categoryName">
                    <el-input v-model="categoryModel.categoryName" minlength="1" maxlength="10"></el-input>
                </el-form-item>
                <el-form-item label="分类别名" prop="categoryAlias">
                    <el-input v-model="categoryModel.categoryAlias" minlength="1" maxlength="15"></el-input>
                </el-form-item>
            </el-form>
            <span class="dialog-footer">
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" @click="title === '添加分类' ? addCategory() : updateCategory()"> 确认 </el-button>
            </span>
        </el-dialog>
        <!-- 添加修改文章分类的弹窗 -->
        <!-- <el-dialog v-model="dialogVisible" :title="title" width="30%">
            <el-form :model="categoryModel" :rules="rules" label-width="100px" style="padding-right: 30px">
                <el-form-item label="分类名称" prop="categoryName">
                    <el-input v-model="categoryModel.categoryName" minlength="1" maxlength="10"></el-input>
                </el-form-item>
                <el-form-item label="分类别名" prop="categoryAlias">
                    <el-input v-model="categoryModel.categoryAlias" minlength="1" maxlength="15"></el-input>
                </el-form-item>
            </el-form>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="dialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="editCategory"> 确认 </el-button>
                </span>
            </template>
        </el-dialog> -->
    </el-card>
>
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
</style>