<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createActivity, deleteActivity, getMyActivities, submitActivity, updateActivity } from '../../api/activity'
import type { Activity, ActivityPayload } from '../../types/activity'

const loading = ref(false)
const saving = ref(false)
const activities = ref<Activity[]>([])
const form = reactive<ActivityPayload>({
  title: '',
  description: '',
  location: '',
  startTime: '',
  endTime: '',
  registrationStartTime: '',
  registrationEndTime: '',
  capacity: 100,
  requireFeedback: false,
})
const editingId = ref<number | null>(null)

const statusLabel: Record<string, string> = {
  DRAFT: '草稿',
  PENDING_REVIEW: '审核中',
  REJECTED: '已驳回',
  APPROVED: '待发布',
  PUBLISHED: '已发布',
  ONGOING: '进行中',
  ENDED: '已结束',
}

async function loadActivities() {
  loading.value = true
  try {
    activities.value = (await getMyActivities()).data.data
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '活动加载失败')
  } finally {
    loading.value = false
  }
}

async function saveDraft() {
  saving.value = true
  try {
    if (editingId.value) {
      await updateActivity(editingId.value, form)
      ElMessage.success('活动已更新')
    } else {
      await createActivity(form)
      ElMessage.success('活动草稿已保存')
    }
    resetForm()
    await loadActivities()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '保存失败')
  } finally {
    saving.value = false
  }
}

function resetForm() {
  editingId.value = null
  Object.assign(form, { title: '', description: '', location: '', startTime: '', endTime: '', registrationStartTime: '', registrationEndTime: '', capacity: 100, requireFeedback: false, coverImage: undefined, feedbackDeadline: undefined })
}

function editActivity(activity: Activity) {
  editingId.value = activity.id
  Object.assign(form, {
    title: activity.title,
    description: activity.description,
    location: activity.location,
    startTime: activity.startTime,
    endTime: activity.endTime,
    registrationStartTime: activity.registrationStartTime,
    registrationEndTime: activity.registrationEndTime,
    capacity: activity.capacity,
    requireFeedback: activity.requireFeedback,
    coverImage: activity.coverImage,
    feedbackDeadline: activity.feedbackDeadline,
  })
}

async function removeActivity(id: number) {
  try {
    await ElMessageBox.confirm('删除后无法恢复，确定继续吗？', '删除活动', { type: 'warning' })
    await deleteActivity(id)
    ElMessage.success('活动已删除')
    await loadActivities()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.response?.data?.message ?? '删除失败')
  }
}

async function submitForReview(id: number) {
  try {
    await submitActivity(id)
    ElMessage.success('已提交审核')
    await loadActivities()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '提交失败')
  }
}

onMounted(loadActivities)
</script>

<template>
  <div class="page-card">
    <div class="page-heading"><div><h2>我的活动</h2><p>创建活动草稿并提交管理员审核。</p></div><el-button @click="loadActivities">刷新</el-button></div>
    <el-card shadow="never" class="form-card">
      <template #header>{{ editingId ? '编辑活动草稿' : '创建活动草稿' }}</template>
      <el-form label-position="top">
        <div class="form-columns">
          <el-form-item label="活动名称"><el-input v-model="form.title" placeholder="例如：校园春季运动会" /></el-form-item>
          <el-form-item label="活动地点"><el-input v-model="form.location" placeholder="例如：大学生活动中心" /></el-form-item>
        </div>
        <el-form-item label="活动介绍"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
        <div class="form-columns">
          <el-form-item label="活动开始"><el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
          <el-form-item label="活动结束"><el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
          <el-form-item label="报名开始"><el-date-picker v-model="form.registrationStartTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
          <el-form-item label="报名结束"><el-date-picker v-model="form.registrationEndTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
        </div>
        <div class="form-columns">
          <el-form-item label="人数上限"><el-input-number v-model="form.capacity" :min="1" /></el-form-item>
          <el-form-item label="活动结束后收集反馈"><el-switch v-model="form.requireFeedback" /></el-form-item>
        </div>
        <el-button type="primary" :loading="saving" @click="saveDraft">{{ editingId ? '保存修改' : '保存草稿' }}</el-button>
        <el-button v-if="editingId" @click="resetForm">取消编辑</el-button>
      </el-form>
    </el-card>
    <el-table v-loading="loading" :data="activities" stripe>
      <el-table-column prop="title" label="活动名称" min-width="180" />
      <el-table-column prop="status" label="状态" width="110"><template #default="{ row }"><el-tag>{{ statusLabel[row.status] ?? row.status }}</el-tag></template></el-table-column>
      <el-table-column prop="capacity" label="容量" width="90" />
      <el-table-column label="操作" min-width="210"><template #default="{ row }"><el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" link type="primary" @click="editActivity(row)">编辑</el-button><el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" link type="primary" @click="submitForReview(row.id)">提交审核</el-button><el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" link type="danger" @click="removeActivity(row.id)">删除</el-button></template></el-table-column>
    </el-table>
  </div>
</template>
