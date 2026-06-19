<template>
  <div class="privacy-container">
    <header class="privacy-header">
      <div class="header-content">
        <span class="category-tag">Legal & Trust</span>
        <h1>Privacy Policy</h1>
        <p class="subtitle">Effective Date: June 19, 2026 • Version 1.0</p>
      </div>
    </header>

    <main class="privacy-content">
      <div class="intro-card">
        <p>
          We believe in ultimate data minimization and absolute transparency. This Privacy Policy outlines exactly what information we collect, how it is processed, and your rights regarding that data. We explicitly do not track you, sell your information, or build behavioral profiles.
        </p>
      </div>

      <section class="policy-section">
        <div class="section-icon">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
        </div>
        <div class="section-text">
          <h2>1. Data We Collect & Store</h2>
          <p>To provide our collaborative writing service while respecting your anonymity, we strictly limit data collection to the absolute technical minimum. We only store the following four data points:</p>

          <ul class="data-list">
            <li>
              <strong>Username:</strong> A unique identifier chosen by you to attribute your contributions and display your authorship within the platform.
            </li>
            <li>
              <strong>Hashed Password:</strong> We never see or store your raw password. Passwords are cryptographically salted and hashed using robust, industry-standard algorithms before hitting our database, ensuring your credentials remain completely private.
            </li>
            <li>
              <strong>Submitted Text & Contributions:</strong> Any text content, words, or character sequences you explicitly write and commit to the collaborative platform. This text becomes a permanent or structural part of the shared engine.
            </li>
            <li>
              <strong>Session Tokens:</strong> Short-lived, secure JSON Web Tokens (JWT) or session identifiers stored locally in your browser (e.g., via cookies or local storage). These are used solely to verify your authenticated session and secure real-time WebSocket pipelines.
            </li>
          </ul>
        </div>
      </section>

      <section class="policy-section">
        <div class="section-icon">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
        </div>
        <div class="section-text">
          <h2>2. How Your Data Is Processed</h2>
          <p>Your data is processed strictly to maintain state synchronization and security across the platform:</p>
          <ul>
            <li><strong>Authentication:</strong> Usernames, hashed passwords, and session tokens are processed to authenticate REST endpoints and authorize bidirectional messaging connections.</li>
            <li><strong>Real-Time Delivery:</strong> Submitted text is processed via our application backend and broadcasted dynamically to other active concurrent users.</li>
          </ul>
        </div>
      </section>

      <section class="policy-section">
        <div class="section-icon">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/></svg>
        </div>
        <div class="section-text">
          <h2>3. What We Do NOT Collect</h2>
          <p>For absolute clarity, our infrastructure is intentionally engineered to avoid capturing secondary tracking metrics. We do <strong>not</strong> collect, log, or store:</p>
          <ul>
            <li>Your real name, email address, or physical contact information.</li>
            <li>IP addresses or geographical location data.</li>
            <li>Browser history, device fingerprinting details, or behavioral telemetry.</li>
            <li>Third-party analytics tracking tags or advertising tracking cookies.</li>
          </ul>
        </div>
      </section>

      <section class="policy-section">
        <div class="section-icon">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
        </div>
        <div class="section-text">
          <h2>4. Data Retention & Governance</h2>
          <p>Submitted text contributions are deeply integrated into the structural ledger of the platform and will remain accessible as part of the public project record. Account records (usernames and password hashes) are maintained until you request deletion or as required to enforce platform security boundaries (such as managing real-time moderation parameters).</p>

          <h2>5. Contact & Inquiries</h2>
          <p>If you have any questions regarding this policy or want to request a manual look at your account identifiers, please open an issue or reach out directly via our official repository channels.</p>
        </div>
      </section>
    </main>

    <footer class="privacy-footer">
      <p>© 2026 Collaborative Engine. Built with transparency and respect for user privacy.</p>
    </footer>
  </div>
</template>

<script setup>
// Privacy view component containing explicit data-minimization privacy terms.
</script>

<style scoped>
/* Main Container styling following a clean, editorial, professional aesthetic */
.privacy-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 20px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  color: #2c3e50;
  line-height: 1.6;
}

/* Header Banner Styling */
.privacy-header {
  border-bottom: 2px solid #edf2f7;
  padding-bottom: 24px;
  margin-bottom: 32px;
}

.category-tag {
  display: inline-block;
  background-color: #ebf8ff;
  color: #2b6cb0;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 4px 10px;
  border-radius: 4px;
  margin-bottom: 12px;
}

.privacy-header h1 {
  font-size: 2.25rem;
  color: #1a202c;
  margin: 0 0 8px 0;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.subtitle {
  color: #718096;
  font-size: 0.95rem;
  margin: 0;
}

/* Intro block */
.intro-card {
  background-color: #f7fafc;
  border-left: 4px solid #4a5568;
  padding: 18px 24px;
  border-radius: 0 8px 8px 0;
  margin-bottom: 40px;
  font-size: 1.05rem;
  color: #4a5568;
}

/* Structural Layout for Sections */
.policy-section {
  display: table;
  width: 100%;
  margin-bottom: 36px;
}

.section-icon {
  display: table-cell;
  width: 48px;
  vertical-align: top;
  padding-top: 4px;
  color: #4a5568;
}

.section-text {
  display: table-cell;
  vertical-align: top;
  padding-left: 16px;
}

.section-text h2 {
  font-size: 1.35rem;
  color: #1a202c;
  margin-top: 0;
  margin-bottom: 12px;
  font-weight: 700;
}

.section-text p {
  margin-bottom: 16px;
  color: #4a5568;
}

/* Bullet list custom treatment */
.data-list {
  list-style: none;
  padding-left: 0;
  margin: 20px 0;
}

.data-list li {
  position: relative;
  padding-left: 24px;
  margin-bottom: 18px;
  color: #4a5568;
}

.data-list li::before {
  content: "•";
  position: absolute;
  left: 4px;
  top: 0;
  color: #3182ce;
  font-size: 1.4rem;
  line-height: 1;
}

.data-list strong {
  color: #2d3748;
}

ul:not(.data-list) {
  padding-left: 20px;
  margin-bottom: 16px;
}

ul:not(.data-list) li {
  margin-bottom: 8px;
  color: #4a5568;
}

/* Footer Accent */
.privacy-footer {
  margin-top: 60px;
  padding-top: 24px;
  border-top: 1px solid #edf2f7;
  text-align: center;
  font-size: 0.85rem;
  color: #a0aec0;
}
</style>
