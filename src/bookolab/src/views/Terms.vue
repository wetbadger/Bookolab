<template>
  <div class="terms-container">
    <header class="terms-header">
      <div class="header-content">
        <span class="category-tag">Legal & Rules</span>
        <h1>Terms of Service</h1>
        <p class="subtitle">Effective Date: June 19, 2026 • Version 1.0</p>
      </div>
    </header>

    <main class="terms-content">
      <div class="intro-card">
        <p>
          Welcome to our collaborative platform. By accessing or using our service, you agree to comply with and be bound by these Terms of Service. If you do not agree to these rules, please do not use the platform.
        </p>
      </div>

      <section class="policy-section">
        <div class="section-icon">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 1 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
        </div>
        <div class="section-text">
          <h2>1. Description of Service</h2>
          <p>Our platform provides a real-time environment that allows users to type words, create shared text, and contribute to a collaborative engine. To keep this environment functional and lightweight, we do not support rich media uploads, profile pictures, or advanced trackers—just raw, collaborative text entry.</p>
        </div>
      </section>

      <section class="policy-section">
        <div class="section-icon">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="7.86 2 16.14 2 22 7.86 22 16.14 16.14 22 7.86 22 2 16.14 2 7.86 7.86 2"></polygon><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
        </div>
        <div class="section-text">
          <h2>2. Acceptable Use & Prohibited Content</h2>
          <p>Because this is a shared space, your freedom to type words comes with strict community boundaries. You are entirely responsible for the text you submit. <strong>We have a zero-tolerance policy for the following behaviors:</strong></p>

          <ul class="rule-list">
            <li>
              <strong>Illegal Activities:</strong> You may not use this platform to discuss, promote, facilitate, or coordinate illegal acts. This includes, but is not limited to, sharing instructions for crimes, distributing illicit materials, or coordinating malicious software deployment.
            </li>
            <li>
              <strong>Hate Speech & Harassment:</strong> You are strictly prohibited from submitting text that promotes discrimination, hatred, or violence against individuals or groups based on race, ethnicity, religion, gender, sexual orientation, disability, or nationality. Bullying, targeted harassment, and slurs will result in an immediate permanent ban.
            </li>
            <li>
              <strong>Disruption & Exploitation:</strong> You may not use scripts, automated bots, or spam techniques to flood the collaborative canvas with text or degrade service performance for others.
            </li>
          </ul>
        </div>
      </section>

      <section class="policy-section">
        <div class="section-icon">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg>
        </div>
        <div class="section-text">
          <h2>3. Content Ownership & License</h2>
          <p>By typing text on this platform, you grant us a worldwide, royalty-free, perpetual license to host, display, and broadcast your submitted words to other concurrent and future users of the project. Because the text engine integrates inputs in real time, contributions are generally permanent and cannot always be individualistically undone or deleted once woven into the collective record.</p>
        </div>
      </section>

      <section class="policy-section">
        <div class="section-icon">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="16"></line><line x1="8" y1="12" x2="16" y2="12"></line></svg>
        </div>
        <div class="section-text">
          <h2>4. Enforcement & Account Termination</h2>
          <p>We reserve the right, but do not assume the obligation, to monitor submitted text. We look out for violations of these terms to keep the platform safe. We reserve the sole right to remove any text, invalidate session tokens, and permanently ban usernames that violate our content restrictions without prior notice or liability.</p>

          <h2>5. Limitation of Liability</h2>
          <p>This service is provided "as is" and "as available" without any warranties of any kind. We are not responsible for the accuracy, safety, or offensiveness of any text submitted by other users. Use the platform at your own discretion.</p>
        </div>
      </section>
    </main>

    <footer class="terms-footer">
      <p>© 2026 Collaborative Engine. Thank you for keeping our text spaces respectful and creative.</p>
    </footer>
  </div>
</template>

<script setup>
// Terms of Service view component focused on raw text submission guidelines.
</script>

<style scoped>
/* Scoped layout following the matching professional aesthetic */
.terms-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 20px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  color: #2c3e50;
  line-height: 1.6;
}

/* Header Banner */
.terms-header {
  border-bottom: 2px solid #edf2f7;
  padding-bottom: 24px;
  margin-bottom: 32px;
}

.category-tag {
  display: inline-block;
  background-color: #fff5f5;
  color: #c53030;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 4px 10px;
  border-radius: 4px;
  margin-bottom: 12px;
}

.terms-header h1 {
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
  border-left: 4px solid #c53030;
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
  color: #e53e3e;
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

/* Rule list custom treatment */
.rule-list {
  list-style: none;
  padding-left: 0;
  margin: 20px 0;
}

.rule-list li {
  position: relative;
  padding-left: 24px;
  margin-bottom: 18px;
  color: #4a5568;
}

.rule-list li::before {
  content: "•";
  position: absolute;
  left: 4px;
  top: 0;
  color: #e53e3e;
  font-size: 1.4rem;
  line-height: 1;
}

.rule-list strong {
  color: #2d3748;
}

/* Footer Accent */
.terms-footer {
  margin-top: 60px;
  padding-top: 24px;
  border-top: 1px solid #edf2f7;
  text-align: center;
  font-size: 0.85rem;
  color: #a0aec0;
}
</style>
