import { escapeHtml } from "../format.js";

export function showToast(message, type = "info", durationMs = 3800) {
  const root = document.getElementById("toast-root");
  if (!root) return;

  const toast = document.createElement("div");
  toast.className = `toast ${type}`;
  toast.innerHTML = `
    <div>${escapeHtml(message)}</div>
    <button type="button" class="link-btn" aria-label="Dismiss">&times;</button>
  `;

  const dismiss = () => {
    if (toast.parentNode) {
      toast.remove();
    }
  };

  toast.querySelector("button").addEventListener("click", dismiss);
  root.appendChild(toast);
  setTimeout(dismiss, durationMs);
}
