<template>
  <div class="mode-toggle">
    <router-link v-if="!isEditMode" :to="`/pages/${id}/edit`" class="btn">✏️ Edit Page</router-link>
    <router-link v-else :to="`/pages/${id}`" class="btn">👁️ View Page</router-link>
  </div>

  <div>
    {{ pageStore.records }}
  </div>

  <div v-if="dbError" class="error-alert">
    ⚠️ Error: {{ dbError }}
  </div>

  <div v-else-if="pageStore.loading && displayedWords.length === 0">
    Loading database values...
  </div>

  <div v-else class="sentence-container">
    <span v-if="isEditMode" class="plus-sign">
      <Plus
        :previous="lastWordIdOfPreviousPage"
        :next="firstWord?.id"
        @submit="handleWordSubmit"
      />
    </span>

    <template v-for="word in displayedWords" :key="word.id">
      <span :id="word.id">
        <Word :data="word" />
      </span>

      <span v-if="isEditMode && word.showPlus" class="plus-sign">
        <Plus
          :previous="word.id"
          :next="word.nextWordId"
          @submit="handleWordSubmit"
        />
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
const firstWord = ref(null);
const lastWordIdOfPreviousPage = ref(null);

const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));

// Instantly load all words into the array for immediate editing
const loadWords = async (streamWordsInRealTime, loadPlusSigns) => {
  const result = [];
  
  firstWord.value = pageStore.records?.firstWord || null; 
  lastWordIdOfPreviousPage.value = pageStore.records?.lastWordIdOfPreviousPage;
  let currentWord = firstWord.value;

  while (currentWord) {
    const wordId = currentWord.id ? currentWord.id : lastWordIdOfPreviousPage.value;

    result.push({ 
      id: Number(wordId), // Force it to be a pure number
      content: currentWord.content, 
      nextWordId: currentWord?.nextWord?.id ? Number(currentWord.nextWord.id) : null, 
      showPlus: false 
    });

    if (streamWordsInRealTime) {
      displayedWords.value = [...result];
      await delay(30);
    }
    
    currentWord = currentWord.nextWord;
  }

  if (!streamWordsInRealTime) {
    displayedWords.value = result;
  }

  if (loadPlusSigns) {
    for (const word of displayedWords.value) {
      await delay(40);
      word.showPlus = true;
    }
  }
};

const handleWordSubmit = (data) => {
  const { id, content, previous, next } = data;

  const newWordObj = {
    id: id,
    content: content,
    nextWordId: next,
    showPlus: true
  };

  // Grab the ID of the current first word displayed on the screen
  const currentFirstWordId = displayedWords.value[0]?.id;

  // CRITICAL FIX: It belongs at the front if there's no previous ID,
  // OR if the 'next' ID matches the current first word on the screen.
  const isInsertingAtFront = !previous || (next && Number(next) === Number(currentFirstWordId));

  if (isInsertingAtFront) {
    // Insert at the absolute front of the displayed list
    displayedWords.value.unshift(newWordObj);
  } else {
    // Find the word we are inserting AFTER in the middle or end
    const previousIndex = displayedWords.value.findIndex(word => Number(word.id) === Number(previous));

    if (previousIndex !== -1) {
      // Update the pointer of the word preceding our new word
      displayedWords.value[previousIndex].nextWordId = newWordObj.id;

      // Splice the new word into the flat array layout exactly where it belongs
      displayedWords.value.splice(previousIndex + 1, 0, newWordObj);
    }
  }
};

// Central logic initializer
const initializePage = async () => {
  // Use props.id dynamically instead of hardcoding page one
  await pageStore.fetchPage(props.id);

  if (props.isEditMode) {
    loadWords(false, true);
  } else {
    loadWords(true, false);
  }
};

onMounted(() => {
  initializePage();
});

// Watch for route switches (e.g., clicking 'Edit' while viewing)
watch(() => props.isEditMode, () => {
  if (props.isEditMode) {
    loadWords(false, true);
  } else {
    loadWords(true, false);
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
