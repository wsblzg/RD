import assert from 'node:assert/strict'
import {
  clearPendingCeramicTask,
  readPendingCeramicTask,
  savePendingCeramicTask
} from './ceramicModelTaskStore.js'

const data = new Map()
globalThis.localStorage = {
  getItem: (key) => data.get(key) || null,
  setItem: (key, value) => data.set(key, String(value)),
  removeItem: (key) => data.delete(key)
}

savePendingCeramicTask({ taskId: 'task-1', title: '青花梅瓶', prompt: '生成一个青花梅瓶' })
assert.equal(readPendingCeramicTask().taskId, 'task-1')
assert.equal(readPendingCeramicTask().saved, false)

clearPendingCeramicTask('task-2')
assert.equal(readPendingCeramicTask().taskId, 'task-1')

clearPendingCeramicTask('task-1')
assert.equal(readPendingCeramicTask(), null)

savePendingCeramicTask({})
assert.equal(readPendingCeramicTask(), null)
