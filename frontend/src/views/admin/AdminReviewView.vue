<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveActivity, getAdminActivities, publishActivity, rejectActivity, startActivity, unpublishActivity } from '../../api/activity'
import type { Activity } from '../../types/activity'

const loading = ref(false)
const activities = ref<Activity[]>([])

async function loadReviews() {
  loading.value = true
  try {
    activities.value = (await getAdminActivities()).data.data
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '审核列表加载失败')
  } finally {
    loading.value = false
  }
}

async function approve(id: number) {
  try {
    await approveActivity(id)
    ElMessage.success('审核通过')
    await loadReviews()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '审核失败')
  }
}

async function reject(id: number) {
  try {
    const result = await ElMessageBox.prompt('请输入驳回原因', '驳回活动', { inputPattern: /\S+/, inputErrorMessage: '驳回原因不能为空' })
    await rejectActivity(id, result.value)
    ElMessage.success('活动已驳回')
    await loadReviews()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.response?.data?.message ?? '操作失败')
  }
}

async function publish(id: number) {
  try {
    await publishActivity(id)
    ElMessage.success('活动已发布')
    await loadReviews()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '发布失败')
  }
}

async function unpublish(id: number) {
  try {
    await unpublishActivity(id)
    ElMessage.success('活动已下架')
    await loadReviews()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '下架失败')
  }
}

async function start(id: number) {
  try {
    await ElMessageBox.confirm('立即开启活动并允许参会者签到？', '手动开启活动', { type: 'warning' })
    await startActivity(id)
    ElMessage.success('活动已开启')
    await loadReviews()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.response?.data?.message ?? '开启活动失败')
  }
}

onMounted(loadReviews)
</script>

<template>
  <div class="page-card">
    <div class="page-heading"><div><h2>活动审核</h2><p>审核组织者提交的活动，驳回时必须填写原因。</p></div><el-button @click="loadReviews">刷新</el-button></div>
    <el-empty v-if="!loading && activities.length === 0" description="暂无待审核活动" />
    <el-table v-else v-loading="loading" :data="activities" stripe>
      <el-table-column prop="title" label="活动名称" min-width="180" />
      <el-table-column prop="location" label="地点" width="160" />
      <el-table-column prop="capacity" label="人数上限" width="100" />
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column label="操作" min-width="300"><template #default="{ row }"><el-button v-if="row.status === 'PENDING_REVIEW'" type="success" size="small" @click="approve(row.id)">通过</el-button><el-button v-if="row.status === 'PENDING_REVIEW'" type="danger" size="small" @click="reject(row.id)">驳回</el-button><el-button v-if="row.status === 'APPROVED'" type="primary" size="small" @click="publish(row.id)">发布</el-button><el-button v-if="row.status === 'PUBLISHED'" type="success" size="small" @click="start(row.id)">手动开启</el-button><el-button v-if="row.status === 'PUBLISHED' || row.status === 'ONGOING'" type="warning" size="small" @click="unpublish(row.id)">下架</el-button></template></el-table-column>
    </el-table>
  </div>
</template>
