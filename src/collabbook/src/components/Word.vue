<template>
  <span class="word-container">
    <span class="word-text" @click="registerClick">{{ data.content }}</span>

    <span class="reaction-badges">
      <div class="tooltip-content">
        <div class="buttons-row">

          <BButton
            class="badge like-btn"
            :class="{ active: isLikeActive }"
            @click="toggleLike"
            :disabled="!isAuthenticated"
            v-b-tooltip="!isAuthenticated ? 'Log in to like' : ''"
          >
            👍 <span class="count">{{ data.likeCount || 0 }}</span>
          </BButton>

          <BButton
            class="badge dislike-btn"
            :class="{ active: isDislikeActive }"
            @click="toggleDislike"
            :disabled="!isAuthenticated"
            v-b-tooltip="!isAuthenticated ? 'Log in to dislike' : ''"
          >
            👎 <span class="count">{{ data.dislikeCount || 0 }}</span>
          </BButton>

        </div>

        <div class="author-info">
          By: <a :href="`/authors/${data.authorName || 'Anonymous'}`">{{ data.authorName || 'Anonymous' }}</a>
        </div>
        <!-- TODO: add a v-if user is admin or has enough delete credits -->
        <!-- Show delete cost -->
          <BButton
            class="delete-btn"
            @click="deleteWord"
          >
            <span class="delete">DELETE</span><br><span> {{ credits }} credits</span>
          </BButton>
      </div>
    </span>
  </span>
</template>

<script setup>
import {computed, ref} from 'vue';
import { BButton, vBTooltip } from 'bootstrap-vue-next';
import { usePageStore } from '@/stores/pageStore';

const pageStore = usePageStore();

const props = defineProps({
  data: { type: Object, required: true },
  isAuthenticated: { type: Boolean, required: true },
  currentPageId: { type: Number },
  wordId: { type: Number }
});

const credits = computed(() => props.data.likeCount - props.data.dislikeCount ); // TODO: make this based on std deviations

const emit = defineEmits(['react']);

// Simple local booleans initialized to whatever the server said originally
const isLikeActive = ref(props.data.userLiked);
const isDislikeActive = ref(props.data.userDisliked);

let clickCount = 0;

const deleteWord = () => {
  pageStore.deleteWordViaWebSocket({
    currentPageId: props.currentPageId,
    wordId: props.wordId
  });
};

const toggleLike = () => {
  isLikeActive.value = !isLikeActive.value; // Simple true/false toggle
  emit('react', props.data.id, 'LIKE');
  if (isLikeActive.value && isDislikeActive.value) {
    isDislikeActive.value = false;
  }
};

const toggleDislike = () => {
  isDislikeActive.value = !isDislikeActive.value; // Simple true/false toggle
  emit('react', props.data.id, 'DISLIKE');
  if (isLikeActive.value && isDislikeActive.value) {
    isLikeActive.value = false;
  }
};

let isTimingClicks = false;

const registerClick = () => {
  clickCount += 1;
  if (!isTimingClicks) {
    isTimingClicks = true;
    setTimeout(
      function () {
        countClicks();
      }, 500
    );
  }
}

const countClicks = () => {
  isTimingClicks = false;
  if (clickCount === 2) {
    toggleLike();
  } else if (clickCount === 3) {
    toggleDislike();
  }
  clickCount = 0;
}
</script>

<style scoped>
.word-container {
  display: inline-block;
  position: relative; /* Crucial for absolute positioning of the tooltip */
  margin-right: 0.25rem;
  padding: 2px 4px;
  cursor: pointer;
}

.word-text {
  font-size: 1.1rem;
  font-weight: 500;
}

/* Tooltip wrapper */
.reaction-badges {
  position: absolute;
  top: 100%; /* Places it right below the word */
  left: 50%;
  transform: translateX(-50%) translateY(8px); /* Centers it and pushes it down for the arrow */
  display: flex;
  flex-direction: column;
  z-index: 10;

  /* Hide by default */
  visibility: hidden;
  opacity: 0;
  transition: opacity 0.2s ease, transform 0.2s ease;
}

/* Show tooltip on hover */
.word-container:hover .reaction-badges {
  visibility: visible;
  opacity: 1;
  transform: translateX(-50%) translateY(4px); /* Subtle slide-up effect */
}

/* The Box */
.tooltip-content {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 8px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  white-space: nowrap; /* Prevents text wrapping inside the box */
  display: flex;
  flex-direction: column;
  gap: 6px;
}

/* Invisible bridge touching the bottom of the word container */
.tooltip-content::before {
  content: '';
  position: absolute;
  top: -12px; /* Stretches up across the gap */
  left: 0;
  width: 100%;
  height: 12px; /* Matches the gap height */
  background: transparent; /* Invisible to the user */
}

/* The Arrow (Divet Triangle) */
.reaction-badges::before {
  content: "";
  position: absolute;
  top: -6px; /* Positions it on top of the box border */
  left: 50%;
  transform: translateX(-50%);
  border-width: 0 6px 6px 6px;
  border-style: solid;
  border-color: transparent transparent #e5e7eb transparent; /* Matches border color */
  z-index: 11;
}

.reaction-badges::after {
  content: "";
  position: absolute;
  top: -5px; /* Sits 1px lower to mask the border and match white background */
  left: 50%;
  transform: translateX(-50%);
  border-width: 0 6px 6px 6px;
  border-style: solid;
  border-color: transparent transparent white transparent;
  z-index: 12;
}

.buttons-row {
  display: flex;
  gap: 6px;
}

.author-info {
  font-size: 0.75rem;
  color: #4b5563;
  text-align: left;
}

.author-info a {
  color: #2563eb;
  text-decoration: none;
}
.author-info a:hover {
  text-decoration: underline;
}

.badge {
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 2px 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.8rem;
}

.badge:not(:disabled):hover {
  background: #e5e7eb;
}

.like-btn:not(:disabled):active { background-color: #d1fae5; }
.dislike-btn:not(:disabled):active { background-color: #fee2e2; }

.badge:disabled {
  background-color: white;
  border-color: white;
  pointer-events: auto !important;
}

.count {
  font-weight: bold;
  color: #4b5563;
}

/* Highlight styles when the user has reacted */
.like-btn.active {
  background-color: #d1fae5; /* Light green background */
  border-color: #10b981;     /* Emerald border accent */
}

.dislike-btn.active {
  background-color: #fee2e2; /* Light red background */
  border-color: #ef4444;     /* Red border accent */
}

/* Make the count extra bold when active */
.badge.active .count {
  font-weight: 800; /* Extra bold */
  color: #111827;   /* Darker text color for emphasis */
}

.delete-btn {
  background-color: #ef4444; /* Standard red */
  border: none;
  color: white;
  border-radius: 12px;
  padding: 4px 12px;
  cursor: pointer;
  display: block;
  font-size: 0.8rem;
  width: 100%;
}

.delete-btn:hover {
  background-color: #f87171; /* A lighter shade for hover */
}
</style>
