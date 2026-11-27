import { escapeHtml } from "../format.js";
import { icon } from "../icons.js";

export function closeModal() {
  const root = document.getElementById("modal-root");
  if (!root) return;
  root.classList.add("hidden");
  root.innerHTML = "";
}

export function openModal({ title, bodyHtml, footerHtml = "", size = "md", onMount = null }) {
  const root = document.getElementById("modal-root");
  if (!root) return;

  root.innerHTML = `
    <div class="modal-dialog ${size === "lg" ? "modal-lg" : ""}">
      <div class="modal-header">
        <h3 class="modal-title">${escapeHtml(title)}</h3>
        <button type="button" class="icon-btn" id="modal-close-btn" aria-label="Close">${icon("close")}</button>
      </div>
      <div class="modal-body" id="modal-body-slot">${bodyHtml}</div>
      ${footerHtml ? `<div class="modal-footer" id="modal-footer-slot">${footerHtml}</div>` : ""}
    </div>
  `;

  root.classList.remove("hidden");
  root.querySelector("#modal-close-btn").addEventListener("click", closeModal);

  const onBackdropClick = e => {
    if (e.target === root) {
      closeModal();
    }
  };
  root.onclick = onBackdropClick;

  if (typeof onMount === "function") {
    onMount(root);
  }
}

export function confirmDialog({
  title,
  message,
  confirmLabel = "Confirm",
  danger = false,
  onConfirm
}) {
  openModal({
    title,
    bodyHtml: `<p style="margin:0;color:var(--text-primary);line-height:1.55;">${escapeHtml(message)}</p>`,
    footerHtml: `
      <button type="button" class="btn btn-secondary" id="confirm-cancel-btn">Cancel</button>
      <button type="button" class="btn ${danger ? "btn-danger-solid" : "btn-primary"}" id="confirm-ok-btn">${escapeHtml(confirmLabel)}</button>
    `,
    onMount: root => {
      root.querySelector("#confirm-cancel-btn").addEventListener("click", closeModal);
      const okBtn = root.querySelector("#confirm-ok-btn");
      okBtn.addEventListener("click", async () => {
        okBtn.disabled = true;
        try {
          await onConfirm();
          closeModal();
        } catch (err) {
          okBtn.disabled = false;
          const body = root.querySelector("#modal-body-slot");
          const existingErr = body.querySelector(".notice-error");
          if (existingErr) existingErr.remove();
          body.insertAdjacentHTML("afterbegin", `<div class="notice notice-error">${escapeHtml(err.message)}</div>`);
        }
      });
    }
  });
}

document.addEventListener("keydown", e => {
  if (e.key === "Escape") {
    closeModal();
  }
});
