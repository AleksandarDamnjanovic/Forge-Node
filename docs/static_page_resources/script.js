/**
 * Forge Node Static Website Interactive Effects
 * 1. Scroll-triggered cascading animations for images 1..4 and paired text paragraphs
 * 2. Automated & interactive side-by-side interchanging animation for Modular & Monolithic architectures
 * 3. Diagram lightbox viewer
 */

document.addEventListener('DOMContentLoaded', () => {
  /* ========================================================================
     1. Cascading Scroll Reveal Observer
     ======================================================================== */
  const revealElements = document.querySelectorAll('.reveal-item, .reveal-left, .reveal-right');

  if ('IntersectionObserver' in window) {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-visible');
          }
        });
      },
      {
        threshold: 0.14,
        rootMargin: '0px 0px -40px 0px',
      }
    );

    revealElements.forEach((el) => observer.observe(el));
  } else {
    revealElements.forEach((el) => el.classList.add('is-visible'));
  }

  /* ========================================================================
     2. Modular & Monolithic Architecture Interchanging Animation
     ======================================================================== */
  const archStage = document.getElementById('archStage');
  const swapArchBtn = document.getElementById('swapArchBtn');
  const toggleAutoSwapBtn = document.getElementById('toggleAutoSwapBtn');
  const autoSwapDot = document.getElementById('autoSwapDot');
  const autoSwapLabel = document.getElementById('autoSwapLabel');
  const archCycleBar = document.getElementById('archCycleBar');
  const badgeModular = document.getElementById('badgeModular');
  const badgeMonolithic = document.getElementById('badgeMonolithic');

  const SWAP_INTERVAL_MS = 4800;
  const TICK_MS = 60;
  let elapsedMs = 0;
  let autoSwapEnabled = true;
  let isHoverPaused = false;

  function performInterchange() {
    if (!archStage) return;
    const currentState = archStage.getAttribute('data-state');
    const nextState = currentState === 'modular-left' ? 'monolithic-left' : 'modular-left';
    archStage.setAttribute('data-state', nextState);

    if (badgeModular && badgeMonolithic) {
      if (nextState === 'modular-left') {
        badgeModular.textContent = 'Spotlight View';
        badgeMonolithic.textContent = 'Companion View';
      } else {
        badgeModular.textContent = 'Companion View';
        badgeMonolithic.textContent = 'Spotlight View';
      }
    }
    elapsedMs = 0;
    if (archCycleBar) {
      archCycleBar.style.width = '0%';
    }
  }

  setInterval(() => {
    if (!autoSwapEnabled || isHoverPaused) return;
    elapsedMs += TICK_MS;
    const pct = Math.min(100, (elapsedMs / SWAP_INTERVAL_MS) * 100);
    if (archCycleBar) {
      archCycleBar.style.width = `${pct}%`;
    }
    if (elapsedMs >= SWAP_INTERVAL_MS) {
      performInterchange();
    }
  }, TICK_MS);

  if (swapArchBtn) {
    swapArchBtn.addEventListener('click', () => {
      performInterchange();
    });
  }

  if (toggleAutoSwapBtn) {
    toggleAutoSwapBtn.addEventListener('click', () => {
      autoSwapEnabled = !autoSwapEnabled;
      toggleAutoSwapBtn.setAttribute('aria-pressed', String(autoSwapEnabled));
      if (autoSwapDot) {
        autoSwapDot.classList.toggle('paused', !autoSwapEnabled);
      }
      if (autoSwapLabel) {
        autoSwapLabel.textContent = autoSwapEnabled
          ? 'Auto-Interchange: ON'
          : 'Auto-Interchange: PAUSED';
      }
    });
  }

  if (archStage) {
    archStage.addEventListener('mouseenter', () => {
      isHoverPaused = true;
    });
    archStage.addEventListener('mouseleave', () => {
      isHoverPaused = false;
    });
  }

  /* ========================================================================
     3. Lightbox Modal for Expanding Architecture Diagrams
     ======================================================================== */
  const lightbox = document.getElementById('archLightbox');
  const lightboxImg = document.getElementById('lightboxImg');
  const lightboxTitle = document.getElementById('lightboxTitle');
  const lightboxClose = document.getElementById('lightboxClose');
  const lightboxBackdrop = document.getElementById('lightboxBackdrop');

  function openLightbox(src, title) {
    if (!lightbox || !lightboxImg) return;
    lightboxImg.src = src;
    if (lightboxTitle) {
      lightboxTitle.textContent = title || 'Architecture Diagram';
    }
    lightbox.classList.add('open');
    lightbox.setAttribute('aria-hidden', 'false');
  }

  function closeLightbox() {
    if (!lightbox) return;
    lightbox.classList.remove('open');
    lightbox.setAttribute('aria-hidden', 'true');
  }

  document.querySelectorAll('.zoom-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      const src = btn.getAttribute('data-zoom-src');
      const title = btn.getAttribute('data-zoom-title');
      if (src) openLightbox(src, title);
    });
  });

  document.querySelectorAll('.arch-img-container').forEach((container) => {
    container.addEventListener('click', () => {
      const img = container.querySelector('img');
      if (img) {
        openLightbox(img.getAttribute('src'), img.getAttribute('alt'));
      }
    });
  });

  if (lightboxClose) lightboxClose.addEventListener('click', closeLightbox);
  if (lightboxBackdrop) lightboxBackdrop.addEventListener('click', closeLightbox);
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && lightbox && lightbox.classList.contains('open')) {
      closeLightbox();
    }
  });

  /* ========================================================================
     4. Load Settings from ./meta.conf & Setup Install / Tutorials Actions
     ======================================================================== */
  const githubLinkBtn = document.getElementById('githubLinkBtn');
  const githubUrlDisplay = document.getElementById('githubUrlDisplay');
  const dockerPullCommandText = document.getElementById('dockerPullCommandText');
  const copyDockerCmdBtn = document.getElementById('copyDockerCmdBtn');
  const copyBtnLabel = document.getElementById('copyBtnLabel');
  const copyStatus = document.getElementById('copyStatus');
  const downloadComposeBtn = document.getElementById('downloadComposeBtn');
  const tutorialsLinkBtn = document.getElementById('tutorialsLinkBtn');

  // Dynamically sync with ./static_page_resources/meta.conf if served over HTTP/HTTPS
  fetch('./static_page_resources/meta.conf')
    .then((res) => (res.ok ? res.text() : Promise.reject(new Error('Failed to load meta.conf'))))
    .then((text) => {
      const config = {};
      text.split(/\r?\n/).forEach((line) => {
        const trimmed = line.trim();
        if (!trimmed || trimmed.startsWith('#')) return;
        const eqIdx = trimmed.indexOf('=');
        if (eqIdx > 0) {
          const key = trimmed.slice(0, eqIdx).trim();
          const val = trimmed.slice(eqIdx + 1).trim();
          config[key] = val;
        }
      });

      if (config.github) {
        if (githubLinkBtn) githubLinkBtn.href = config.github;
        if (githubUrlDisplay) githubUrlDisplay.textContent = config.github;
      }
      if (config.docker_pull_command && dockerPullCommandText) {
        dockerPullCommandText.textContent = config.docker_pull_command;
      }
      if (config.docker_compose && downloadComposeBtn) {
        downloadComposeBtn.href = config.docker_compose;
      }
      if (config.tutorials && tutorialsLinkBtn) {
        tutorialsLinkBtn.href = config.tutorials;
      }
    })
    .catch(() => {
      // Defaults from meta.conf are already pre-rendered in index.html
    });

  // Copy Docker Pull Command to Clipboard
  if (copyDockerCmdBtn && dockerPullCommandText) {
    copyDockerCmdBtn.addEventListener('click', async () => {
      const cmd = dockerPullCommandText.textContent.trim();
      try {
        if (navigator.clipboard && navigator.clipboard.writeText) {
          await navigator.clipboard.writeText(cmd);
        } else {
          const textarea = document.createElement('textarea');
          textarea.value = cmd;
          textarea.style.position = 'fixed';
          textarea.style.opacity = '0';
          document.body.appendChild(textarea);
          textarea.select();
          document.execCommand('copy');
          document.body.removeChild(textarea);
        }

        copyDockerCmdBtn.classList.add('copied');
        if (copyBtnLabel) copyBtnLabel.textContent = 'Copied!';
        if (copyStatus) {
          copyStatus.textContent = 'Copied to clipboard';
          copyStatus.classList.add('copied');
        }

        setTimeout(() => {
          copyDockerCmdBtn.classList.remove('copied');
          if (copyBtnLabel) copyBtnLabel.textContent = 'Copy Command';
          if (copyStatus) {
            copyStatus.textContent = 'Ready to copy';
            copyStatus.classList.remove('copied');
          }
        }, 2400);
      } catch (err) {
        if (copyStatus) copyStatus.textContent = 'Press Ctrl+C to copy';
      }
    });
  }

  // Ensure clicking Download docker-compose.yml forces a file download
  if (downloadComposeBtn) {
    downloadComposeBtn.addEventListener('click', async (e) => {
      const targetUrl = downloadComposeBtn.getAttribute('href') || './static_page_resources/docker-compose.yml';
      try {
        const response = await fetch(targetUrl);
        if (!response.ok) return; // Fall back to native <a download>
        e.preventDefault();
        const blob = await response.blob();
        const blobUrl = URL.createObjectURL(blob);
        const tempLink = document.createElement('a');
        tempLink.href = blobUrl;
        tempLink.download = 'docker-compose.yml';
        document.body.appendChild(tempLink);
        tempLink.click();
        document.body.removeChild(tempLink);
        setTimeout(() => URL.revokeObjectURL(blobUrl), 1500);
      } catch (_) {
        // When opened via file:// protocol, native <a href="./docker-compose.yml" download="docker-compose.yml"> handles it
      }
    });
  }
});
