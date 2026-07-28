const STORAGE_KEY = 'yc_ai3d_pending_task'

export const savePendingCeramicTask = (task) => {
  if (!task?.taskId) return
  localStorage.setItem(STORAGE_KEY, JSON.stringify({ ...task, saved: false, updatedAt: Date.now() }))
}

export const readPendingCeramicTask = () => {
  try {
    const task = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')
    return task?.taskId && !task.saved ? task : null
  } catch (error) {
    localStorage.removeItem(STORAGE_KEY)
    return null
  }
}

export const clearPendingCeramicTask = (taskId) => {
  const task = readPendingCeramicTask()
  if (!task || !taskId || task.taskId === taskId) {
    localStorage.removeItem(STORAGE_KEY)
  }
}
