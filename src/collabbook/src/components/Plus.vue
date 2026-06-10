<template>
  <div class="plus-wrapper">
    <input
      v-if="isEditing"
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
import { ref, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import { usePageStore } from '@/stores/pageStore';

const emit = defineEmits(['submit']);
// Grab the 'previous' word prop passed down from the parent template
const props = defineProps({
  previous: {
    type: Number,
    default: null // Will be null for the very first plus sign on the page
  },
  next: {
    type: Number,
    default: null
  }
});

const route = useRoute();
const pageStore = usePageStore();

const isEditing = ref(false);
const isSubmitting = ref(false);
const newWord = ref('');
const inputRef = ref(null);

// Switch to input field and auto-focus it
const startEditing = async () => {
  isEditing.value = true;
  // NextTick ensures the DOM has updated and the input exists before we try to focus it
  await nextTick();
  inputRef.value?.focus();
};

// Reset state if the user clicks away without typing anything
const cancelEditing = () => {
  if (!isSubmitting.value) {
    isEditing.value = false;
    newWord.value = '';
  }
};

// Handle submitting to the backend
const submitWord = async () => {
  const trimmedWord = newWord.value.trim();
  if (!trimmedWord) { cancelEditing(); return; }

  try {
    isSubmitting.value = true;
    const currentPageId = Number(route.params.id);

    // 1. Await the server's database response
    const savedWord = await pageStore.addWord(trimmedWord, currentPageId, props.previous, props.next);

    // 2. NOW emit the real, database-allocated ID to the parent
    emit('submit', {
      id: savedWord.id, // 100% valid database ID
      content: trimmedWord,
      previous: props.previous,
      next: props.next
    });

    newWord.value = '';
    isEditing.value = false;
  } catch (error) {
    console.error("Failed to add word:", error);
    inputRef.value?.focus();
  } finally {
    isSubmitting.value = false;
  }
};
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
