<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import jsQR from 'jsqr'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()

const router = useRouter()
const video = ref<HTMLVideoElement | null>(null)
const cameraStream = ref<MediaStream | null>(null)
const cameraError = ref('')
const cameraReady = ref(false)
const scanning = ref(false)
const pastedValue = ref('')
const imageInput = ref<HTMLInputElement | null>(null)
const isLanHttp = window.location.protocol === 'http:'
  && !['localhost', '127.0.0.1', '::1'].includes(window.location.hostname.toLowerCase())
let scanTimer: ReturnType<typeof setInterval> | undefined
let detector: BarcodeDetectorInstance | undefined

type DetectedBarcode = { rawValue?: string }
type BarcodeDetectorInstance = {
  detect(source: HTMLVideoElement | ImageBitmap): Promise<DetectedBarcode[]>
}
type BarcodeDetectorConstructor = new (options?: { formats?: string[] }) => BarcodeDetectorInstance

function barcodeDetectorConstructor() {
  return (window as unknown as { BarcodeDetector?: BarcodeDetectorConstructor }).BarcodeDetector
}

function close() {
  stopCamera()
  emit('update:modelValue', false)
}

function parseCheckinUrl(value: string) {
  try {
    const url = new URL(value.trim(), window.location.origin)
    const token = url.searchParams.get('token')?.trim()
    if (url.pathname !== '/checkin' || !token) return null
    return {
      token,
      activityId: url.searchParams.get('activityId')?.trim() ?? '',
    }
  } catch {
    return null
  }
}

async function useScannedValue(value: string) {
  const checkin = parseCheckinUrl(value)
  if (!checkin) {
    ElMessage.error('二维码不是本系统的签到二维码')
    return
  }
  close()
  await router.push({ name: 'checkin', query: checkin })
}

async function scanFrame() {
  if (!detector || !video.value || video.value.readyState < HTMLMediaElement.HAVE_CURRENT_DATA) return
  try {
    const detections = await detector.detect(video.value)
    const value = detections[0]?.rawValue
    if (value) await useScannedValue(value)
  } catch {
    cameraError.value = '当前浏览器无法识别二维码，请使用手机系统相机扫码。'
    stopCamera()
  }
}

async function startCamera() {
  cameraError.value = ''
  cameraReady.value = false
  const Detector = barcodeDetectorConstructor()
  if (!Detector) {
    cameraError.value = '当前浏览器暂不支持网页扫码，请使用手机系统相机或微信扫码。'
    return
  }
  if (!window.isSecureContext && window.location.hostname !== 'localhost') {
    cameraError.value = '局域网 HTTP 页面不能直接调用摄像头，请使用手机系统相机扫码；正式部署 HTTPS 后可使用网页扫码。'
    return
  }
  if (!navigator.mediaDevices?.getUserMedia) {
    cameraError.value = '当前浏览器不支持摄像头，请使用手机系统相机扫码。'
    return
  }
  try {
    cameraStream.value = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: { ideal: 'environment' } },
      audio: false,
    })
    await nextTick()
    if (!video.value) return
    video.value.srcObject = cameraStream.value
    await video.value.play()
    cameraReady.value = true
    scanning.value = true
    detector = new Detector({ formats: ['qr_code'] })
    scanTimer = setInterval(() => { void scanFrame() }, 250)
  } catch {
    cameraError.value = '摄像头权限未开启，请允许浏览器访问摄像头后重试。'
    stopCamera()
  }
}

function stopCamera() {
  scanning.value = false
  cameraReady.value = false
  if (scanTimer) {
    clearInterval(scanTimer)
    scanTimer = undefined
  }
  cameraStream.value?.getTracks().forEach((track) => track.stop())
  cameraStream.value = null
  detector = undefined
  if (video.value) video.value.srcObject = null
}

async function submitPastedValue() {
  if (!pastedValue.value.trim()) {
    ElMessage.warning('请粘贴二维码链接')
    return
  }
  await useScannedValue(pastedValue.value)
}

function chooseImage() {
  imageInput.value?.click()
}

function decodeCanvasRegion(image: HTMLImageElement, sourceX: number, sourceY: number, sourceWidth: number, sourceHeight: number) {
  const canvas = document.createElement('canvas')
  const scale = Math.min(3, 2200 / Math.max(sourceWidth, sourceHeight))
  canvas.width = Math.max(1, Math.round(sourceWidth * scale))
  canvas.height = Math.max(1, Math.round(sourceHeight * scale))
  const context = canvas.getContext('2d', { willReadFrequently: true })
  if (!context) return null
  context.drawImage(image, sourceX, sourceY, sourceWidth, sourceHeight, 0, 0, canvas.width, canvas.height)
  const imageData = context.getImageData(0, 0, canvas.width, canvas.height)
  return jsQR(imageData.data, imageData.width, imageData.height, { inversionAttempts: 'attemptBoth' })
}

function decodeImage(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  const image = new Image()
  const imageUrl = URL.createObjectURL(file)
  image.onload = () => {
    try {
      const width = image.naturalWidth
      const height = image.naturalHeight
      const fullResult = decodeCanvasRegion(image, 0, 0, width, height)
      const centerSize = Math.min(width, height) * 0.82
      const centerResult = fullResult || decodeCanvasRegion(
        image,
        Math.max(0, (width - centerSize) / 2),
        Math.max(0, (height - centerSize) / 2),
        centerSize,
        centerSize,
      )
      const result = centerResult
      if (!result) {
        ElMessage.error('未识别到二维码，请让二维码占画面一半以上并拍摄清晰、完整的图片')
        return
      }
      void useScannedValue(result.data)
    } finally {
      URL.revokeObjectURL(imageUrl)
    }
  }
  image.onerror = () => {
    URL.revokeObjectURL(imageUrl)
    ElMessage.error('二维码图片读取失败，请重试')
  }
  image.src = imageUrl
}

watch(() => props.modelValue, (visible) => {
  if (visible) {
    pastedValue.value = ''
    void nextTick(startCamera)
  } else {
    stopCamera()
  }
})

onBeforeUnmount(stopCamera)
</script>

<template>
  <el-dialog :model-value="modelValue" title="扫码签到" width="min(440px, 94vw)" @close="close">
    <div class="scanner-content">
      <video v-if="!cameraError" ref="video" class="scanner-video" playsinline muted />
      <div v-if="cameraReady" class="scanner-hint">请将动态二维码放入取景框</div>
      <el-alert v-if="isLanHttp" title="当前是局域网 HTTP 页面" type="warning" :closable="false" show-icon>
        <p class="scanner-alert-text">请优先关闭此窗口，直接用手机系统相机或微信扫描电脑上显示的二维码；识别后选择在当前浏览器打开。网页摄像头扫码需要 HTTPS。</p>
      </el-alert>
      <el-alert v-if="cameraError" :title="cameraError" type="info" :closable="false" show-icon />
      <p class="scanner-help">已登录后扫码会直接提交签到；同一设备会保留登录状态。</p>
      <input ref="imageInput" class="scanner-file-input" type="file" accept="image/*" capture="environment" @change="decodeImage" />
      <el-button plain @click="chooseImage">拍照/选择二维码图片</el-button>
      <el-input v-model="pastedValue" type="textarea" :rows="2" placeholder="也可以粘贴二维码打开的 /checkin 链接" />
      <el-button class="scanner-submit" type="primary" @click="submitPastedValue">打开签到链接</el-button>
    </div>
    <template #footer><el-button @click="close">关闭</el-button></template>
  </el-dialog>
</template>
