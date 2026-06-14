import {createRouter , createWebHistory} from 'vue-router'
const routes = [
  { path: '/login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/article/browse',
    children: [
      { path: '/article/browse', component: () => import('../views/article/ArticleBrowse.vue') },
      { path: '/article/read', component: () => import('../views/article/ArticleDetail.vue') },
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