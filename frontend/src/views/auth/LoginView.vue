<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const submitting = ref(false)
const form = reactive({ username: '', password: '' })

async function submit() {
  submitting.value = true
  try {
    await auth.login(form)
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
      ? route.query.redirect
      : { name: 'home' }
    await router.push(redirect)
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '登录失败，请检查用户名和密码')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-card class="auth-card" shadow="never">
    <template #header><div class="card-title">登录系统</div></template>
    <el-form label-position="top" @submit.prevent="submit">
      <el-form-item label="用户名">
        <el-input v-model="form.username" autocomplete="username" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" show-password autocomplete="current-password" placeholder="请输入密码" @keyup.enter="submit" />
      </el-form-item>
      <el-button class="full-button" type="primary" :loading="submitting" @click="submit">登录</el-button>
    </el-form>
    <p class="form-help">还没有账号？<router-link to="/register">去注册</router-link></p>
  </el-card>
</template>
