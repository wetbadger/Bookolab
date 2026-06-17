<template>
  <div class="plus-wrapper">
    <input
      v-if="isComponentEditing"
      ref="inputRef"
      v-model="newWord"
      type="text"
      maxlength="30"
      placeholder="Type word..."
      class="word-input"
      :disabled="isSubmitting"
      @keydown.enter.prevent="submitWord"
      @keydown.space.prevent="submitWord"
      @blur="cancelEditing"
    />

    <button
      v-else
      type="button"
      class="plus-btn"
      @click="startEditing"
    >
      <span>+</span>
    </button>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { usePageStore } from '@/stores/pageStore';

const emit = defineEmits(['submit']);
const props = defineProps({
  // Enforces data mapping discipline matching Spring Boot's Long vs String params
  previous: {
    type: Number,
    default: null
  },
  next: {
    type: Number,
    default: null
  },
  isEditing: {
    type: Boolean,
    default: false
  },
  previousLocalId: {
    type: String,
    default: null
  },
  currentPageId: {
    type: Number
  }
});

const route = useRoute();
const pageStore = usePageStore();

const isComponentEditing = ref(false);
const isSubmitting = ref(false);
const newWord = ref('');
const inputRef = ref(null);

let localId = null;

const startEditing = async () => {
  if (!localId) localId = pageStore.generateSimpleId();
  isComponentEditing.value = true;
  await nextTick();
  inputRef.value?.focus();
};

const cancelEditing = () => {
  if (!isSubmitting.value) {
    isComponentEditing.value = false;
    newWord.value = '';
  }
};

const focusInnerInput = () => {
  if (!localId) localId = pageStore.generateSimpleId();
  isComponentEditing.value = true;
  nextTick(() => {
    inputRef.value?.focus();
  });
};

const submitWord = () => {
  const trimmedWord = newWord.value.trim();
  if (!trimmedWord) { cancelEditing(); return; }

  if (!localId) localId = pageStore.generateSimpleId();
  const currentLocalId = localId;
  const currentPageId = props.currentPageId;
  // const currentPageId = Number(route.params.id);

  // 1. OPTIMISTIC PASS: Broadcast immediately to layout to construct the next inline button
  emit('submit', {
    id: null,
    localId: currentLocalId,
    content: trimmedWord,
    previous: props.previous,         // Pure Long (or null)
    next: props.next,                 // Pure Long (or null)
    previousLocalId: props.previousLocalId // Pure String UUID (or null)
  });

  // Re-initialize this UI input slot completely so it is ready for reuse
  newWord.value = '';
  isComponentEditing.value = false;
  localId = null;

// Look for your pageStore.sendWordViaWebSocket line and change it to pass an object:
  pageStore.sendWordViaWebSocket({
    content: trimmedWord,
    currentPageId: currentPageId,
    localId: currentLocalId,
    previousWordId: props.previous,
    nextWordId: props.next,
    previousLocalId: props.previousLocalId
  });
};

onMounted(() => {
  isComponentEditing.value = props.isEditing;
  if (props.isEditing) {
    localId = pageStore.generateSimpleId();
  }
});

defineExpose({
  focusInnerInput,
  isComponentEditing,
  previous: props.previous
});
</script>

<style scoped>
.plus-wrapper {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.plus-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #e0e0e0;
  border: none;
  border-radius: 4px;
  width: 24px;
  height: 24px;
  font-size: 1rem;
  font-weight: bold;
  color: #555;
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 0;
}

.plus-btn:hover:not(:disabled) {
  background-color: #007bff;
  color: white;
  transform: scale(1.1);
}

.word-input {
  width: 100px;
  padding: 2px 6px;
  font-size: 1rem;
  border: 2px solid #007bff;
  border-radius: 4px;
  outline: none;
  text-align: center;
}

.word-input:disabled {
  background-color: #f5f5f5;
  border-color: #ccc;
  color: #999;
}

.spinner {
  font-size: 0.8rem;
  animation: pulse 1s infinite alternate;
}

@keyframes pulse {
  from { opacity: 0.5; }
  to { opacity: 1; }
}
</style>
