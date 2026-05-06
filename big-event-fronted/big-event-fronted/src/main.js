import { createApp } from 'vue'
import './style.css'
import './assets/main.scss'
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import router from '/src/router/index.js'
import { createPinia } from 'pinia'
import locale from 'element-plus/dist/locale/zh-cn.js'
//导入持久化插件
import { createPersistedState } from 'pinia-persistedstate-plugin'
const pinia = createPinia()
const persist= createPersistedState()
const app = createApp(App)
app.use(ElementPlus,{locale})
app.use(router)
//pinia使用持久化插件
pinia.use(persist)
app.use(pinia)
app.mount('#app')
