<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getOperationLogs } from '../../api/operationLog'
import type { OperationLogItem } from '../../types/operationLog'

const loading = ref(false)
const logs = ref<OperationLogItem[]>([])
const filters = reactive({ operation: '', targetType: '', userId: '' })

const operationOptions = [
  'ACTIVITY_APPROVED',
  'ACTIVITY_REJECTED',
  'ACTIVITY_PUBLISHED',
  'ACTIVITY_UNPUBLISHED',
  'ACTIVITY_MANUALLY_STARTED',
  'ACTIVITY_MANUALLY_ENDED',
  'ATTENDANCE_MANUAL_CHECKIN',
  'ATTENDANCE_CANCELLED',
  'ORGANIZER_APPLICATION_SUBMITTED',
  'ORGANIZER_APPLICATION_APPROVED',
  'ORGANIZER_APPLICATION_REJECTED',
]

const operationLabel: Record<string, string> = {
  ACTIVITY_APPROVED: '活动审核通过',
  ACTIVITY_REJECTED: '活动审核驳回',
  ACTIVITY_PUBLISHED: '活动发布',
  ACTIVITY_UNPUBLISHED: '活动下架',
  ACTIVITY_MANUALLY_STARTED: '手动开启活动',
  ACTIVITY_MANUALLY_ENDED: '手动结束活动',
  ATTENDANCE_MANUAL_CHECKIN: '手动补签',
  ATTENDANCE_CANCELLED: '撤销签到',
  ORGANIZER_APPLICATION_SUBMITTED: '提交发起者申请',
  ORGANIZER_APPLICATION_APPROVED: '通过发起者申请',
  ORGANIZER_APPLICATION_REJECTED: '驳回发起者申请',
  BOOTSTRAP_ADMIN_CREATED: '创建初始管理员',
}

const targetTypeLabel: Record<string, string> = {
  ACTIVITY: '活动',
  ATTENDANCE: '签到',
  USER: '用户',
  ORGANIZER_APPLICATION: '发起者申请',
}

async function loadLogs() {
  loading.value = true
  try {
    const userId = filters.userId.trim() ? Number(filters.userId) : undefined
    logs.value = (await getOperationLogs({
      operation: filters.operation || undefined,
      targetType: filters.targetType || undefined,
      userId: Number.isFinite(userId) ? userId : undefined,
      limit: 200,
    })).data.data
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '操作日志加载失败')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  Object.assign(filters, { operation: '', targetType: '', userId: '' })
  loadLogs()
}

function describeOperation(operation: string) {
  if (operation.startsWith('USER_ROLE_CHANGED_')) return '调整用户角色'
  return operationLabel[operation] ?? operation
}

function formatTime(value: string) {
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

onMounted(loadLogs)
</script>

<template>
  <div class="page-card">
    <div class="page-heading"><div><h2>操作日志</h2><p>查询管理员和发起者执行的关键权限、活动及签到操作。</p></div><el-button @click="loadLogs">刷新</el-button></div>
    <el-form inline class="log-filters" @submit.prevent="loadLogs">
      <el-form-item label="操作"><el-select v-model="filters.operation" clearable filterable placeholder="全部操作"><el-option v-for="operation in operationOptions" :key="operation" :label="describeOperation(operation)" :value="operation" /></el-select></el-form-item>
      <el-form-item label="对象"><el-select v-model="filters.targetType" clearable placeholder="全部对象"><el-option v-for="(label, value) in targetTypeLabel" :key="value" :label="label" :value="value" /></el-select></el-form-item>
      <el-form-item label="操作人 ID"><el-input v-model="filters.userId" clearable placeholder="例如 1" /></el-form-item>
      <el-form-item><el-button type="primary" native-type="submit">查询</el-button><el-button @click="resetFilters">重置</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="logs" stripe>
      <el-table-column label="时间" min-width="170"><template #default="{ row }">{{ formatTime(row.createdAt) }}</template></el-table-column>
      <el-table-column label="操作人" min-width="150"><template #default="{ row }"><span>{{ row.name }}</span><small v-if="row.username" class="muted-copy">（{{ row.username }}，ID {{ row.userId }}）</small></template></el-table-column>
      <el-table-column label="操作" min-width="170"><template #default="{ row }">{{ describeOperation(row.operation) }}</template></el-table-column>
      <el-table-column label="对象" min-width="130"><template #default="{ row }"><el-tag effect="plain">{{ targetTypeLabel[row.targetType] ?? row.targetType ?? '—' }}</el-tag><span v-if="row.targetId"> #{{ row.targetId }}</span></template></el-table-column>
      <el-table-column prop="operation" label="操作代码" min-width="250" show-overflow-tooltip />
    </el-table>
  </div>
</template>
