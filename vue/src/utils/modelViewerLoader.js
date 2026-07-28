let modelViewerPromise = null

export const hasModelViewer = () => {
  if (typeof window === 'undefined') return false
  return Boolean(window.customElements?.get('model-viewer'))
}

export const ensureModelViewer = async () => {
  if (hasModelViewer()) return true
  if (typeof window === 'undefined') return false

  if (!modelViewerPromise) {
    modelViewerPromise = import('@google/model-viewer')
      .then(async () => {
        if (window.customElements?.whenDefined) {
          await window.customElements.whenDefined('model-viewer')
        }
        return true
      })
      .catch((error) => {
        modelViewerPromise = null
        throw error
      })
  }

  return modelViewerPromise
}
