import {createRouter , createWebHistory} from 'vue-router'
import LayoutVue from '../views/Layout.vue'
import LoginVue from '../views/Login.vue'
import { de } from 'element-plus/es/locale/index.mjs'
// import ArticleCategoryVue from '../views/article/ArticleCategory.vue'
// import ArticleManageVue from '../views/article/ArticleManage.vue'
// import UserAvatarVue from '../views/user/UserAvatar.vue'
// import UserInfoVue from '../views/user/UserInfo.vue'
// import UserResetPassword from '../views/user/UserResetPassword.vue'
// 不再直接导入 LayoutVue 和其它组件
// 移除静态导入：import LayoutVue from '../views/Layout.vue'
// 移除其它静态导入（如果需要也改成动态导入）

const routes = [
  { path: '/login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/article/manage',
    children: [
      { path: '/article/category', component: () => import('../views/article/ArticleCategory.vue') },
      { path: '/article/manage', component: () => import('../views/article/ArticleManage.vue') },
      { path: '/user/avatar', component: () => import('../views/user/UserAvatar.vue') },
      { path: '/user/info', component: () => import('../views/user/UserInfo.vue') },
      { path: '/user/changePassword', component: () => import('../views/user/UserChangePassword.vue') },
      { path: '/writer/test', component: () => import('../views/writer/WriterTest.vue') },
      { path: '/bilibili/hot', component: () => import('../views/bilibili/BilibiliHot.vue') }
    ]
  }
]

const router = createRouter({
  routes,
  history: createWebHistory()
})

export default router