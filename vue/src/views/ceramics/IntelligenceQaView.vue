<template>
  <div class="qa-standalone-page">
    <NewHeaderNavigation />

    <main class="qa-page-main">
      <section class="qa-page-inner">
        <header class="qa-page-head">
          <p class="qa-kicker">INTELLIGENCE</p>
          <h1>柴烧知识问答</h1>
          <p>循着窑火与陶土的脉络，在一问一答之间，读懂柴烧的技艺、器物与传承故事。</p>
        </header>

        <div class="digital-qa-stage">
          <div class="conversation-column">
            <IntelligenceHub
              ref="hubRef"
              view-mode="qa"
              @answer-complete="handleAnswerComplete"
            />
          </div>
          <aside class="digital-human-column" aria-label="柴智云数字讲解员">
            <XilingRealtimeAvatar
              ref="avatarRef"
              @quick-question="handleQuickQuestion"
            />
          </aside>
        </div>
      </section>
    </main>

    <SiteFooter />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import NewHeaderNavigation from '@/components/NewHeaderNavigation.vue'
import SiteFooter from '@/components/SiteFooter.vue'
import IntelligenceHub from '@/components/intelligence/IntelligenceHub.vue'
import XilingRealtimeAvatar from '@/components/xiling-realtime/XilingRealtimeAvatar.vue'

const hubRef = ref(null)
const avatarRef = ref(null)

const handleAnswerComplete = (answer) => {
  avatarRef.value?.speak(answer)
}

const handleQuickQuestion = (question) => {
  hubRef.value?.askQuestion(question)
}
</script>

<style scoped>
.qa-standalone-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 14% 14%, rgba(var(--ym-gold-rgb), 0.08), transparent 24%),
    radial-gradient(circle at 88% 34%, rgba(var(--ym-accent-rgb), 0.06), transparent 28%),
    var(--ym-bg);
}

.qa-page-main {
  width: min(1600px, calc(100vw - 48px));
  margin: 22px auto 0;
  padding-bottom: 56px;
}

.qa-page-head {
  margin-bottom: 14px;
}

.qa-kicker {
  margin: 0;
  font-size: 0.72rem;
  letter-spacing: 0.24em;
  color: var(--ym-gold);
  text-transform: uppercase;
}

.qa-page-head h1 {
  margin: 8px 0 0;
  font-family: var(--ym-font-display);
  font-size: clamp(1.9rem, 2.6vw, 2.5rem);
  font-weight: 600;
  line-height: 1.15;
}

.qa-page-head p {
  max-width: 680px;
  margin: 8px 0 0;
  color: var(--ym-text-secondary);
  font-size: 0.95rem;
  line-height: 1.65;
}

.digital-qa-stage {
  display: grid;
  grid-template-columns: minmax(0, 0.84fr) minmax(500px, 0.76fr);
  align-items: stretch;
  gap: 24px;
  height: clamp(760px, calc(100vh - 190px), 940px);
  min-width: 0;
}

.conversation-column,
.digital-human-column {
  min-width: 0;
  min-height: 0;
}

.conversation-column {
  grid-column: 1;
  grid-row: 1;
  height: 100%;
  overflow: hidden;
}

.conversation-column :deep(.intelligence-hub) {
  height: 100%;
  min-height: 0;
}

.digital-human-column {
  grid-column: 2;
  grid-row: 1;
  height: 100%;
  width: 100%;
  overflow: hidden;
}

.digital-human-column :deep(.xiling-panel) {
  height: 100%;
  max-width: none;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
  border-color: rgba(var(--ym-accent-rgb), 0.32);
  box-shadow:
    0 30px 72px rgba(58, 47, 40, 0.18),
    0 0 0 1px rgba(var(--ym-gold-rgb), 0.08);
}

.digital-human-column :deep(.xiling-viewport) {
  height: 100%;
  min-height: 0;
  max-height: none;
  aspect-ratio: auto;
}

@media (max-width: 1240px) {
  .qa-page-main {
    width: min(1040px, calc(100vw - 40px));
  }

  .digital-qa-stage {
    grid-template-columns: minmax(0, 1fr);
    height: auto;
  }

  .conversation-column {
    grid-column: 1;
    grid-row: 1;
    order: 1;
    height: auto;
    overflow: visible;
  }

  .digital-human-column {
    grid-column: 1;
    grid-row: 2;
    order: 2;
    height: auto;
    width: min(520px, 100%);
    max-width: 520px;
    overflow: visible;
    margin: 20px auto 0;
  }

  .conversation-column :deep(.intelligence-hub) {
    height: auto;
  }

  .digital-human-column :deep(.xiling-panel) {
    height: auto;
    max-width: 520px;
    grid-template-rows: auto auto auto auto;
  }

  .digital-human-column :deep(.xiling-viewport) {
    height: auto;
    max-height: 560px;
    aspect-ratio: 9 / 16;
  }
}

@media (max-width: 720px) {
  .qa-standalone-page {
    overflow-x: hidden;
  }

  .qa-page-main {
    width: min(100% - 24px, 680px);
    margin-top: 12px;
    padding-bottom: 36px;
  }

  .qa-page-head {
    margin-bottom: 12px;
  }

  .qa-page-head p {
    line-height: 1.65;
  }

  .digital-qa-stage {
    width: 100%;
    height: auto;
    overflow: visible;
  }

  .conversation-column,
  .digital-human-column {
    width: 100%;
    height: auto;
    overflow: visible;
  }
}
</style>
