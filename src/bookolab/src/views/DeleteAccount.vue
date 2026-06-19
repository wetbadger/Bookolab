<template>
  <div class="delete-account-container">
    <div class="delete-card">
      <!-- Sad/Depressed Header -->
      <div class="delete-header">
        <div class="emoji-container">
          <span class="big-emoji">😢</span>
          <span class="big-emoji floating">💔</span>
        </div>
        <h1>We'll Miss You!</h1>
        <p class="subtitle">(Even if you were our favorite troublemaker)</p>
      </div>

      <!-- Funny/Sad Message -->
      <div class="message-box">
        <p class="message">
          Before you go, know that:<br>
          • Your words will feel abandoned 📚<br>
          • Your credits will be lonely 💰<br>
          • Our servers will cry a little 😭<br>
          • We'll tell stories about the one that got away...<br>
        </p>
        <p class="sad-joke">
          <span class="joke-icon">😂</span>
          Just kidding! But seriously, we're sad to see you go.
          <span class="joke-icon">🥲</span>
        </p>
      </div>

      <!-- Delete Button with Confirmation -->
      <div class="button-section">
        <button
          @click="deleteAccount()"
          class="btn-delete"
          :disabled="isDeleting"
        >
          <span v-if="!isDeleting">🗑️ Yes, Delete My Account</span>
          <span v-else>Deleting... 💀</span>
        </button>

        <router-link to="/me" class="btn-cancel">
          🙈 Wait, I Changed My Mind!
        </router-link>

        <p class="warning-text">
          ⚠️ This action is permanent and cannot be undone.
          Your words will become anonymous.
        </p>
      </div>

      <!-- Sad Footer -->
      <div class="delete-footer">
        <p class="footer-text">
          We'll keep a candle lit for you 🕯️<br>
          <span class="small-text">(Actually, we'll just recycle your user data. But it sounds nice, right?)</span>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useAuthStore } from "@/stores/authStore.js";
import { useRouter } from "vue-router";

const authStore = useAuthStore();
const router = useRouter();
const isDeleting = ref(false);

const deleteAccount = () => {
  if (confirm("Are you sure you want to delete your account? (This action cannot be undone)")) {
    isDeleting.value = true;
    authStore.deleteAccount();
    // Add a slight delay to show the loading state
    setTimeout(() => {
      router.push("/");
    }, 1500);
  }
}
</script>

<style scoped>
.delete-account-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 80vh;
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.delete-card {
  max-width: 600px;
  width: 100%;
  background: white;
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  animation: slideUp 0.5s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.delete-header {
  text-align: center;
  margin-bottom: 30px;
}

.emoji-container {
  display: flex;
  justify-content: center;
  gap: 15px;
  margin-bottom: 15px;
}

.big-emoji {
  font-size: 50px;
  animation: float 3s ease-in-out infinite;
}

.floating {
  animation-delay: 1.5s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

.delete-header h1 {
  font-size: 32px;
  color: #2c3e50;
  margin: 10px 0;
  font-weight: 700;
}

.subtitle {
  font-size: 18px;
  color: #7f8c8d;
  font-style: italic;
}

.message-box {
  background: #fff5f5;
  border-left: 4px solid #e74c3c;
  padding: 20px 25px;
  border-radius: 10px;
  margin-bottom: 30px;
}

.message {
  font-size: 16px;
  line-height: 1.8;
  color: #2c3e50;
  margin: 0 0 15px 0;
}

.sad-joke {
  font-size: 15px;
  color: #7f8c8d;
  background: white;
  padding: 12px 16px;
  border-radius: 8px;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.joke-icon {
  font-size: 20px;
}

.button-section {
  display: flex;
  flex-direction: column;
  gap: 15px;
  align-items: center;
}

.btn-delete {
  background: #e74c3c;
  color: white;
  border: none;
  padding: 14px 30px;
  border-radius: 10px;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  width: 100%;
  max-width: 350px;
}

.btn-delete:hover:not(:disabled) {
  background: #c0392b;
  transform: scale(1.02);
  box-shadow: 0 5px 20px rgba(231, 76, 60, 0.4);
}

.btn-delete:disabled {
  background: #bdc3c7;
  cursor: not-allowed;
  transform: none;
}

.btn-cancel {
  background: #ecf0f1;
  color: #2c3e50;
  border: 2px solid #bdc3c7;
  padding: 12px 25px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.3s ease;
  width: 100%;
  max-width: 350px;
  text-align: center;
}

.btn-cancel:hover {
  background: #e8e8e8;
  border-color: #95a5a6;
  transform: scale(1.02);
}

.warning-text {
  font-size: 13px;
  color: #7f8c8d;
  text-align: center;
  margin: 10px 0 0 0;
  padding: 10px;
  background: #fff9e6;
  border-radius: 8px;
  width: 100%;
}

.delete-footer {
  margin-top: 30px;
  text-align: center;
  padding-top: 20px;
  border-top: 2px dashed #ecf0f1;
}

.footer-text {
  font-size: 16px;
  color: #34495e;
  line-height: 1.8;
  margin: 0;
}

.small-text {
  font-size: 13px;
  color: #95a5a6;
  font-style: italic;
}

/* Responsive */
@media (max-width: 480px) {
  .delete-card {
    padding: 25px;
  }

  .delete-header h1 {
    font-size: 26px;
  }

  .big-emoji {
    font-size: 40px;
  }

  .btn-delete, .btn-cancel {
    font-size: 15px;
    padding: 12px 20px;
  }
}
</style>
