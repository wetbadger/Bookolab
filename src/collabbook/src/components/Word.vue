<template>
  {{ data.content }}
</template>

<script setup>
// Define the prop to accept the word object
defineProps({
  data: {
    type: Object,
    required: true
  }
});
</script>

<style scoped>
.word-wrapper {
  display: inline-flex;
  align-items: center;
}

/* Add a space between words naturally */
.word-wrapper::after {
  content: " ";
  white-space: pre;
}
</style>