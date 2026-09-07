<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const submitting = ref(false)
const form = reactive({ username: '', password: '', name: '', studentId: '', email: '', phone: '' })

async function submit() {
  submitting.value = true
  try {
    await auth.register(form)
    ElMessage.success('注册成功，请登录')
    await router.push({ name: 'login' })
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '注册失败，请检查填写内容')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-card class="auth-card" shadow="never">
    <template #header><div class="card-title">注册普通用户</div></template>
    <el-form label-position="top" @submit.prevent="submit">
      <el-form-item label="用户名"><el-input v-model="form.username" autocomplete="username" placeholder="3-64 个字符" /></el-form-item>
      <el-form-item label="密码"><el-input v-model="form.password" type="password" show-password autocomplete="new-password" placeholder="至少 8 个字符" /></el-form-item>
      <el-form-item label="姓名"><el-input v-model="form.name" placeholder="请输入姓名" /></el-form-item>
      <el-form-item label="学号"><el-input v-model="form.studentId" placeholder="可选" /></el-form-item>
      <el-form-item label="邮箱"><el-input v-model="form.email" type="email" placeholder="可选" /></el-form-item>
      <el-form-item label="手机号"><el-input v-model="form.phone" placeholder="可选" /></el-form-item>
      <el-button class="full-button" type="primary" :loading="submitting" @click="submit">注册</el-button>
    </el-form>
    <p class="form-help">已有账号？<router-link to="/login">返回登录</router-link></p>
  </el-card>
</template>
