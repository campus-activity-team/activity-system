<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveOrganizerApplication, changeUserRole, getAdminUsers, getOrganizerApplications, rejectOrganizerApplication } from '../../api/userManagement'
import { useAuthStore } from '../../stores/auth'
import type { UserProfile, UserRole } from '../../types/auth'
import type { OrganizerApplication } from '../../types/organizer'

const auth = useAuthStore()
const loading = ref(false)
const users = ref<UserProfile[]>([])
const applications = ref<OrganizerApplication[]>([])
const roleDialogVisible = ref(false)
const roleTarget = ref<UserProfile | null>(null)
const targetRole = ref<UserRole>('USER')
const currentPassword = ref('')
const changingRole = ref(false)

const roleLabel: Record<UserRole, string> = {
  USER: '普通用户',
  ORGANIZER: '活动发起者',
  ADMIN: '管理员',
}

const applicationStatusLabel = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回',
}

async function loadData() {
  loading.value = true
  try {
    const [userResponse, applicationResponse] = await Promise.all([
      getAdminUsers(),
      getOrganizerApplications(),
    ])
    users.value = userResponse.data.data
    applications.value = applicationResponse.data.data
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '用户权限数据加载失败')
  } finally {
    loading.value = false
  }
}

async function approve(application: OrganizerApplication) {
  try {
    await ElMessageBox.confirm(`确认授予“${application.name}”活动发起者权限？`, '通过发起者申请', { type: 'warning' })
    await approveOrganizerApplication(application.id)
    ElMessage.success('申请已通过')
    await loadData()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.response?.data?.message ?? '审批失败')
  }
}

async function reject(application: OrganizerApplication) {
  try {
    const result = await ElMessageBox.prompt('请输入驳回原因', '驳回发起者申请', { inputPattern: /\S+/, inputErrorMessage: '驳回原因不能为空' })
    await rejectOrganizerApplication(application.id, result.value)
    ElMessage.success('申请已驳回')
    await loadData()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.response?.data?.message ?? '审批失败')
  }
}

function openRoleDialog(user: UserProfile) {
  roleTarget.value = user
  targetRole.value = user.role
  currentPassword.value = ''
  roleDialogVisible.value = true
}

async function saveRole() {
  if (!roleTarget.value || !currentPassword.value) {
    ElMessage.warning('请输入当前管理员密码确认操作')
    return
  }
  changingRole.value = true
  try {
    await changeUserRole(roleTarget.value.id, { role: targetRole.value, currentPassword: currentPassword.value })
    ElMessage.success('用户角色已更新')
    roleDialogVisible.value = false
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message ?? '角色更新失败')
  } finally {
    changingRole.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-card">
    <div class="page-heading"><div><h2>用户与权限</h2><p>审批活动发起者申请，并由现有管理员安全地授予管理员身份。</p></div><el-button @click="loadData">刷新</el-button></div>
    <el-tabs v-loading="loading">
      <el-tab-pane :label="`发起者申请（${applications.filter((item) => item.status === 'PENDING').length} 待审）`">
        <el-empty v-if="applications.length === 0" description="暂无发起者申请" />
        <el-table v-else :data="applications" stripe>
          <el-table-column prop="name" label="姓名" width="110" />
          <el-table-column prop="studentId" label="学号" width="140" />
          <el-table-column prop="reason" label="申请理由" min-width="240" show-overflow-tooltip />
          <el-table-column label="状态" width="100"><template #default="{ row }">{{ applicationStatusLabel[row.status as keyof typeof applicationStatusLabel] }}</template></el-table-column>
          <el-table-column prop="reviewComment" label="审核意见" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="150"><template #default="{ row }"><template v-if="row.status === 'PENDING'"><el-button link type="success" @click="approve(row)">通过</el-button><el-button link type="danger" @click="reject(row)">驳回</el-button></template><span v-else>已处理</span></template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane :label="`全部用户（${users.length}）`">
        <el-alert title="管理员不能公开注册；请先让对方注册普通账号，再由现有管理员调整为管理员。角色变更需要验证当前管理员密码。" type="warning" show-icon :closable="false" class="management-alert" />
        <el-table :data="users" stripe>
          <el-table-column prop="name" label="姓名" width="110" />
          <el-table-column prop="username" label="用户名" min-width="140" />
          <el-table-column prop="studentId" label="学号" min-width="130" />
          <el-table-column label="角色" width="120"><template #default="{ row }"><el-tag>{{ roleLabel[row.role as UserRole] }}</el-tag></template></el-table-column>
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column label="操作" width="120"><template #default="{ row }"><el-button link type="primary" :disabled="row.id === auth.user?.id" @click="openRoleDialog(row)">调整角色</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="roleDialogVisible" title="调整用户角色" width="min(460px, 94vw)">
      <el-alert v-if="targetRole === 'ADMIN'" title="管理员拥有审核、发布和用户权限管理能力，请确认身份后授予。" type="warning" show-icon :closable="false" />
      <el-form v-if="roleTarget" label-position="top" class="role-change-form">
        <el-form-item label="用户"><el-input :model-value="`${roleTarget.name}（${roleTarget.username}）`" disabled /></el-form-item>
        <el-form-item label="目标角色"><el-select v-model="targetRole"><el-option v-for="(label, role) in roleLabel" :key="role" :label="label" :value="role" /></el-select></el-form-item>
        <el-form-item label="当前管理员密码"><el-input v-model="currentPassword" type="password" show-password autocomplete="current-password" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="roleDialogVisible = false">取消</el-button><el-button type="primary" :loading="changingRole" @click="saveRole">确认变更</el-button></template>
    </el-dialog>
  </div>
</template>
