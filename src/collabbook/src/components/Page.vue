<template>
  <div class="mode-toggle">
    <router-link v-if="!isEditMode" :to="`/pages/${id}/edit`" class="btn">✏️ Edit Page</router-link>
    <router-link v-else :to="`/pages/${id}`" class="btn">👁️ View Page</router-link>
  </div>

  <div v-if="dbError" class="error-alert">
    ⚠️ Error: {{ dbError }}
  </div>

  <div v-else-if="pageStore.loading && displayedWords.length === 0">
    Loading database values...
  </div>

  <div v-else class="sentence-container">
    <span v-if="isEditMode" class="plus-sign">
      <Plus :previous="pageStore.previousPageLastWordId" />
    </span>
    
    <template v-for="word in displayedWords" :key="word.id">
      <span>
        <Word :data="word" />
      </span>
      
      <span v-if="isEditMode && word.showPlus" class="plus-sign">
        <Plus :previous="word.id"/>
      </span>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref, computed, watch } from 'vue';
import { usePageStore } from '@/stores/pageStore';
import Word from '@/components/Word.vue';
import Plus from '@/components/Plus.vue';

// Define the incoming props from Vue Router
const props = defineProps({
  id: { type: String, required: true },
  isEditMode: { type: Boolean, default: false }
});

const pageStore = usePageStore();
const dbError = computed(() => pageStore.error);
const displayedWords = ref([]);

const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));

// Stream words one-by-one for standard viewing
const streamWordsRealTime = async () => {
  displayedWords.value = [];
  let currentWord = pageStore.records?.firstWord;
  
  while (currentWord) {
    displayedWords.value.push({ id: currentWord.id, content: currentWord.content });
    await delay(30); // Fast stream speed
    currentWord = currentWord.nextWord;
  }
};

// Instantly load all words into the array for immediate editing
const loadWordsInstantly = async () => {
  const result = [];
  let currentWord = pageStore.records?.firstWord;
  while (currentWord) {
    result.push({ id: currentWord.id, content: currentWord.content, showPlus: false });
    currentWord = currentWord.nextWord;
  }
  // Words appear instantly
  displayedWords.value = result; 

  // Plus signs cascade in one-by-one
  for (const word of displayedWords.value) {
    await delay(40); // Adjust this delay to make the plus signs spawn faster/slower
    word.showPlus = true;
  }
};

// Central logic initializer
const initializePage = async () => {
  // Use props.id dynamically instead of hardcoding page one
  await pageStore.fetchPage(props.id); 
  
  if (props.isEditMode) {
    loadWordsInstantly();
  } else {
    streamWordsRealTime();
  }
};

onMounted(() => {
  initializePage();
});

// Watch for route switches (e.g., clicking 'Edit' while viewing)
watch(() => props.isEditMode, () => {
  if (props.isEditMode) {
    loadWordsInstantly();
  } else {
    streamWordsRealTime();
  }
});
</script>

<style scoped>
.mode-toggle {
  margin-bottom: 20px;
}
.btn {
  display: inline-block;
  padding: 8px 16px;
  background-color: #007bff;
  color: white;
  text-decoration: none;
  border-radius: 4px;
}
.sentence-container {
  font-size: 1.2rem;
  line-height: 1.5;
  padding: 10px;
  display: flex;
  flex-wrap: wrap; 
  gap: 8px;
}
.word-input {
  width: 70px;
  padding: 4px;
  font-size: 1.1rem;
  text-align: center;
}
.error-alert {
  color: #721c24;
  background-color: #f8d7da;
  border: 1px solid #f5c6cb;
  padding: 10px;
  border-radius: 4px;
}
</style>