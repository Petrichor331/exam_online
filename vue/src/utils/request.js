import {ElMessage} from "element-plus";
import router from "@/router/index.js";
import axios from "axios";

const request = axios.create({
    baseURL: 'http://localhost:9090',
    timeout: 30000  // 后台接口超时时间
})

// request 拦截器
// 可以自请求发送前对请求做一些处理
request.interceptors.request.use(config => {
    config.headers['Content-Type'] = 'application/json;charset=utf-8';

    const userStr = localStorage.getItem("xm-user")
    if (userStr) {
        const user = JSON.parse(userStr)
        if (user && user.token) {
            config.headers['token'] = user.token
        }
    }

    return config
}, error => Promise.reject(error))


// response 拦截器
// 可以在接口响应后统一处理结果
request.interceptors.response.use(
    response => {
        let res = response.data;
        // 如果是返回的文件
        if (response.config.responseType === 'blob') {
            return res
        }
        //当权限验证不通过的时候给出提示
        if(res.code === '401'){
            ElMessage.error('权限验证失败，请重新登录')
            router.push('/login')
        }
        // 兼容服务端返回的字符串数据
        if (typeof res === 'string') {
            res = res ? JSON.parse(res) : res
        }
        return res;
    },
    error => {
        if(!error.response){
            ElMessage.error('请求失败，请检查网络')
            return Promise.reject(error)
        }
        if (error.response.status === 404) {
            ElMessage.error('未找到请求接口')
        } else if (error.response.status === 500) {
            ElMessage.error('系统异常，请查看后端控制台报错')
        } else {
            console.error(error.message)
        }
        return Promise.reject(error)
    }
)

export default request