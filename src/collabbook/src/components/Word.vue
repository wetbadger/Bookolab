<template>
  <span class="word-container">
    <span class="word-text">{{ data.content }}</span>

    <span class="reaction-badges">
      <div class="tooltip-content">
        <div class="buttons-row">
          <button class="badge like-btn" @click="$emit('react', data.id, 'LIKE')">
            👍 <span class="count">{{ data.likeCount || 0 }}</span>
          </button>

          <button class="badge dislike-btn" @click="$emit('react', data.id, 'DISLIKE')">
            👎 <span class="count">{{ data.dislikeCount || 0 }}</span>
          </button>
        </div>

        <div class="author-info">
          Author: <a :href="`/authors/${data.authorName || 'Anonymous'}`">{{ data.authorName || 'Anonymous' }}</a>
        </div>
      </div>
    </span>
  </span>
</template>

<script setup>
defineProps({
  data: {
    type: Object,
    required: true
  }
});

defineEmits(['react']);
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

.badge:hover {
  background: #e5e7eb;
}

.like-btn:active { background-color: #d1fae5; }
.dislike-btn:active { background-color: #fee2e2; }

.count {
  font-weight: bold;
  color: #4b5563;
}
</style>
