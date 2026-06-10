<template>
  <span 
    class="word-text" 
    :class="{ 'word-saving': disabled }"
  >
    {{ data.content }}
  </span>
</template>

<script setup>
// Accept both the original word object data and our new disabled flag
defineProps({
  data: {
    type: Object,
    required: true
  },
  disabled: {
    type: Boolean,
    default: false
  }
});
</script>

<style scoped>
.word-text {
  display: inline;
  transition: color 0.2s ease, opacity 0.2s ease;
}

/* Add a space between words naturally */
.word-text::after {
  content: " ";
  white-space: pre;
}

/* The "isSubmitting" visual feedback style */
.word-saving {
  color: #a0aec0; /* Soft gray text */
  opacity: 0.65;
  font-style: italic;
  cursor: not-allowed;
}
</style>