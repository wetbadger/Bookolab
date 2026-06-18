<template>
  <button @click="deleteAccount()">Delete Account</button>
</template>

<script setup>
import { useAuthStore } from "@/stores/authStore.js";
import { useRouter } from "vue-router";

const authStore = useAuthStore();
const router = useRouter();

const deleteAccount = () => {
  if (confirm("Are you sure you want to delete your account? (This action cannot be undone)")) {
    authStore.deleteAccount();
    router.push("/");
  } else {
    router.push("me");
  }
}
</script>
