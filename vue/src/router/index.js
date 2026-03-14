import { createRouter, createWebHistory } from 'vue-router'
import {getCurrentUser} from "@/utils/userStorage.js";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/login' },
    {
      path: '/manager',
      component: () => import('@/views/Manager.vue'),
      children: [
        { path: 'home', meta: { name: '系统首页' }, component: () => import('@/views/manager/Home.vue')  },
        { path: 'admin', meta: { name: '管理员信息' }, component: () => import('@/views/manager/Admin.vue')},
        { path: 'teacher', meta: { name: '教师信息' }, component: () => import('@/views/manager/Teacher.vue')},
        { path: 'student', meta: { name: '学生信息' }, component: () => import('@/views/manager/Student.vue')},
        { path: 'person', meta: { name: '个人资料' }, component: () => import('@/views/manager/Person.vue'),  },
        { path: 'password', meta: { name: '修改密码' }, component: () => import('@/views/manager/Password.vue'),  },
        { path: 'notice', meta: { name: '系统公告' }, component: () => import('@/views/manager/Notice.vue'),  },
        { path: 'examPlan', meta: { name: '考试安排' }, component: () => import('@/views/manager/ExamPlan.vue'),  },
        { path: 'questionType', meta: { name: '题型信息' }, component: () => import('@/views/manager/QuestionType.vue'),  },
        { path: 'course', meta: { name: '课程信息' }, component: () => import('@/views/manager/Course.vue'),  },
        { path: 'question', meta: { name: '题库信息' }, component: () => import('@/views/manager/Question.vue'),  },
        { path: 'testPaper', meta: { name: '试卷信息' }, component: () => import('@/views/manager/TestPaper.vue'),  },
      ]
    },
    {
      path: '/teacher',
      component: () => import('@/views/TeacherLayout.vue'),
      children: [
        { path: 'home', meta: { name: '教师首页' }, component: () => import('@/views/teacher/Home.vue')  },
        { path: 'grading', meta: { name: '试卷批阅' }, component: () => import('@/views/teacher/Grading.vue'),  },
        { path: 'course', meta: { name: '课程管理' }, component: () => import('@/views/teacher/Course.vue'),  },
        { path: 'testPaper', meta: { name: '试卷管理' }, component: () => import('@/views/teacher/TestPaper.vue'),  },
        { path: 'question', meta: { name: '题库管理' }, component: () => import('@/views/teacher/Question.vue'),  },
        { path: 'examPlan', meta: { name: '考试安排' }, component: () => import('@/views/teacher/ExamPlan.vue'),  },
        { path: 'person', meta: { name: '个人资料' }, component: () => import('@/views/manager/Person.vue'),  },
        { path: 'password', meta: { name: '修改密码' }, component: () => import('@/views/teacher/Password.vue'),  },
      ]
    },
    {
      path: '/front',
      component: () => import('@/views/Front.vue'),
      children: [
        { path: 'home', meta: { name: '首页' }, component: () => import('@/views/front/Home.vue'),  },
        { path: 'person', meta: { name: '个人中心' }, component: () => import('@/views/front/person.vue'),  },
        { path: 'course', meta: { name: '课程中心' }, component: () => import('@/views/front/Course.vue'),  },
        { path: 'myCourse', meta: { name: '我的课程' }, component: () => import('@/views/front/MyCourse.vue'),  },
        { path: 'exam/:paperId', meta: { name: '正在考试' }, component: () => import('@/views/front/Exam.vue'),  },
        { path: 'myScore', meta: { name: '我的成绩' }, component: () => import('@/views/front/MyScore.vue'),  },
        { path: 'examList', meta: { name: '考试列表' }, component: () => import('@/views/front/ExamList.vue')  },
        { path: 'practice', meta: { name: '刷题中心' }, component: () => import('@/views/front/Practice.vue')  },
        { path: 'practice-exam/:courseId', meta: { name: '刷题练习' }, component: () => import('@/views/front/PracticeExam.vue')  },
        { path: 'exam-practice', meta: { name: '模拟考试' }, component: () => import('@/views/front/ExamPractice.vue')  },
        { path: 'answer/:scoreId', meta: { name: '查看答卷' }, component: () => import('@/views/front/Answer.vue')  },
        { path: 'password', meta: { name: '修改密码' }, component: () => import('@/views/front/Password.vue')  },
      ]
    },
    { path: '/404', component: () => import('@/views/404.vue') },
    { path: '/login', component: () => import('@/views/Login.vue') },
    { path: '/register', component: () => import('@/views/Register.vue') },
    { path: '/:pathMatch(.*)', redirect: '/404' }
  ]
})

//路由守卫
router.beforeEach((to, from, next) => {
  const user = getCurrentUser()
  //不需要登录的页面
  const whiteList = ['/login', '/register', '/404']
  if(whiteList.includes(to.path)){
    next()
    return
  }
  //需要登录的页面跳转登录页
  if(! user || !user.id){
    next('/login')
    return
  }
  next()
})

export default router
