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
import { ref, nextTick, watch } from 'vue';

const emit = defineEmits(['submitWordStream', 'close']);

const props = defineProps({
  // Accept either a database Number ID or a temporary String token safely
  previous: { type: [String, Number], default: null },
  next: { type: [String, Number], default: null },
  autoFocus: { type: Boolean, default: false }
});

const isEditing = ref(false);
const newWord = ref('');
const inputRef = ref(null);

const startEditing = async () => {
  isEditing.value = true;
  await nextTick();
  inputRef.value?.focus();
};

const cancelEditing = () => {
  isEditing.value = false;
  newWord.value = '';
  emit('close');
};

const submitWord = () => {
  const trimmedWord = newWord.value.trim();
  if (!trimmedWord) { cancelEditing(); return; }

  // Ship the word up to the parent immediately
  emit('submitWordStream', {
    content: trimmedWord,
    previous: props.previous,
    next: props.next
  });

  // Reset this local input instance. 
  // A completely separate Plus component will catch the focus next!
  newWord.value = '';
  isEditing.value = false;
};

// Watch for the parent telling this specific component instance to open up
watch(() => props.autoFocus, (newValue) => {
  if (newValue === true) {
    startEditing();
  }
}, { immediate: true });
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
