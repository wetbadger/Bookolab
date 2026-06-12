<template>
  <Paginator class="mt-4" />

  <div class="mode-toggle">
    <router-link v-if="!isEditMode" :to="`/pages/${id}/edit`" class="btn">✏️ Edit Page</router-link>
    <router-link v-else :to="`/pages/${id}`" class="btn">👁️ View Page</router-link>
  </div>

  <div v-show="isDebugMode">
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
        :ref="(el) => { if (el) plusRefs['start'] = el }"
        :previous="lastWordIdOfPreviousPage ? Number(lastWordIdOfPreviousPage) : null"
        :next="typeof firstWord?.id === 'number' ? firstWord.id : null"
        :previousLocalId="null"
        @submit="(data) => handleWordSubmit(data, 0)"
      />
    </span>

    <template v-for="(word, index) in displayedWords" :key="word.viewKey">
      <span :id="word.id">
        <Word :data="word" />
      </span>

      <span v-if="isEditMode && word.showPlus" class="plus-sign">
        <Plus
          :ref="(el) => { if (el) plusRefs[word.viewKey] = el }"
          :previous="typeof word.id === 'number' ? word.id : null"
          :next="typeof displayedWords[index + 1]?.id === 'number' ? displayedWords[index + 1].id : null"
          :previousLocalId="word.localId"
          @submit="(data) => handleWordSubmit(data, index + 1)"
        />
      </span>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref, computed, watch, nextTick, onBeforeUpdate } from 'vue';
import { usePageStore } from '@/stores/pageStore';
import Word from '@/components/Word.vue';
import Plus from '@/components/Plus.vue';
import Paginator from "@/components/Pagenator.vue";

const props = defineProps({
  id: { type: String, required: true },
  isEditMode: { type: Boolean, default: false },
  isDebugMode: { type: Boolean, default: false }
});

const pageStore = usePageStore();
const dbError = computed(() => pageStore.error);
const displayedWords = ref([]);
const firstWord = ref(null);
const lastWordIdOfPreviousPage = ref(null);

const plusRefs = ref({});

onBeforeUpdate(() => {
  plusRefs.value = {};
});

const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));

const loadWords = async (streamWordsInRealTime, loadPlusSigns) => {
  const result = [];
  firstWord.value = pageStore.records?.firstWord || null;
  lastWordIdOfPreviousPage.value = pageStore.records?.lastWordIdOfPreviousPage;
  let currentWord = firstWord.value;

  while (currentWord) {
    const wordId = currentWord.id ? currentWord.id : lastWordIdOfPreviousPage.value;

    result.push({
      id: Number(wordId),
      content: currentWord.content,
      nextWordId: currentWord?.nextWord?.id ? Number(currentWord.nextWord.id) : null,
      showPlus: false,
      localId: null,                  // FIX: Pure null for verified database items
      viewKey: 'db-' + Number(wordId) // FIX: Dedicated UI lookup string key
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

const handleWordSubmit = async (data, originIndex) => {
  const { id, localId, content, previous, next, previousLocalId } = data;

  const newWordObj = {
    id: id ? Number(id) : null,
    localId: localId,
    content: content,
    nextWordId: next ? Number(next) : null,
    showPlus: true,
    viewKey: localId // For newly typed words, the localId string doubles perfectly as a viewKey
  };

  if (originIndex === 0) {
    displayedWords.value.unshift(newWordObj);
  } else {
    let targetIndex = -1;

    if (previousLocalId) {
      targetIndex = displayedWords.value.findIndex(word => word.localId === previousLocalId);
    }

    if (targetIndex === -1 && previous !== null) {
      targetIndex = displayedWords.value.findIndex(word => Number(word.id) === Number(previous));
    }

    if (targetIndex !== -1) {
      if (displayedWords.value[targetIndex].id) {
        displayedWords.value[targetIndex].nextWordId = newWordObj.id;
      }
      displayedWords.value.splice(targetIndex + 1, 0, newWordObj);
    } else {
      displayedWords.value.push(newWordObj);
    }
  }

  await nextTick();

  // Target focus by our designated viewKey mapping value
  if (plusRefs.value[newWordObj.viewKey]) {
    plusRefs.value[newWordObj.viewKey].focusInnerInput();
  }
};

const initializePage = async () => {
  await pageStore.fetchPage(props.id);
  loadWords(!props.isEditMode, props.isEditMode);
};

onMounted(() => {
  pageStore.initializeTestWebSocket();
  initializePage();
});

watch(() => props.isEditMode, () => {
  loadWords(!props.isEditMode, props.isEditMode);
});

// 2. WATCH THE ROUTE ID: Triggered when user clicks a pagination number
watch(() => props.id, () => {
  initializePage();
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
