import { api } from "../api.js";
import { escapeHtml, formatCurrency, formatMarginPercent, formatPercent } from "../format.js";
import { closeModal, confirmDialog, openModal } from "./modal.js";
import { showToast } from "./toast.js";

export async function openProductFormModal({ product = null, onSaved = null } = {}) {
  const isEdit = Boolean(product);
  const catData = await api.get("/api/categories");
  const categories = catData.items || [];

  const categoryOptions = categories.map(c => `<option value="${escapeHtml(c.name)}"></option>`).join("");

  openModal({
    title: isEdit ? `Edit Product — ${product.productId}` : "Add New Product",
    size: "lg",
    bodyHtml: `
      <div id="product-form-error" class="notice notice-error hidden"></div>
      <form id="product-modal-form" class="form-grid">
        ${!isEdit ? `
        <div class="form-field">
          <label class="form-label" for="pf-id">Product ID <span class="form-hint">(Optional, auto-generated if blank)</span></label>
          <input class="input mono" id="pf-id" name="productId" placeholder="e.g. PRD-1001" maxlength="30">
        </div>` : `
        <div class="form-field">
          <label class="form-label">Product ID</label>
          <input class="input mono" value="${escapeHtml(product.productId)}" disabled>
        </div>`}
        <div class="form-field">
          <label class="form-label" for="pf-name">Product Name *</label>
          <input class="input" id="pf-name" name="name" required minlength="2" maxlength="120"
                 value="${escapeHtml(product ? product.name : "")}" placeholder="e.g. Wireless Barcode Scanner">
        </div>
        <div class="form-field">
          <label class="form-label" for="pf-category">Category *</label>
          <input class="input" id="pf-category" name="category" list="pf-category-list" required minlength="2" maxlength="60"
                 value="${escapeHtml(product ? product.category : "")}" placeholder="Select or type new category">
          <datalist id="pf-category-list">${categoryOptions}</datalist>
        </div>
        <div class="form-field">
          <label class="form-label" for="pf-qty">Stock Quantity *</label>
          <input class="input num" id="pf-qty" name="quantity" type="number" min="0" step="1" required
                 value="${product ? product.quantity : 0}">
        </div>
        <div class="form-field">
          <label class="form-label" for="pf-cost">Cost Price *</label>
          <input class="input num" id="pf-cost" name="costPrice" type="number" min="0" step="0.01" required
                 value="${product ? Number(product.costPrice).toFixed(2) : ""}" placeholder="0.00">
        </div>
        ${!isEdit ? `
        <div class="form-field">
          <label class="form-label" for="pf-selling">Selling Price *</label>
          <input class="input num" id="pf-selling" name="sellingPrice" type="number" min="0" step="0.01" required
                 placeholder="0.00">
        </div>` : `
        <div class="form-field">
          <label class="form-label">Current Selling Price</label>
          <input class="input num" value="${escapeHtml(formatCurrency(product.sellingPrice))}" disabled>
          <span class="form-hint">Use Price Management to update selling price with audit trail.</span>
        </div>`}
        <div class="form-field full">
          <label class="form-label" for="pf-desc">Description</label>
          <textarea class="textarea" id="pf-desc" name="description" maxlength="500"
                    placeholder="Optional product specifications, supplier notes, or pack details">${escapeHtml(product ? product.description : "")}</textarea>
        </div>
        <div class="form-field full">
          <div class="preview-box" id="pf-margin-preview">
            <div class="kv-row"><span class="kv-label">Unit Margin Preview</span><span class="kv-val" id="pf-margin-val">-</span></div>
          </div>
        </div>
      </form>
    `,
    footerHtml: `
      <button type="button" class="btn btn-secondary" id="pf-cancel">Cancel</button>
      <button type="submit" form="product-modal-form" class="btn btn-primary" id="pf-submit">
        ${isEdit ? "Save Changes" : "Create Product"}
      </button>
    `,
    onMount: root => {
      root.querySelector("#pf-cancel").addEventListener("click", closeModal);
      const costInput = root.querySelector("#pf-cost");
      const sellInput = root.querySelector("#pf-selling");
      const marginValEl = root.querySelector("#pf-margin-val");
      const errBox = root.querySelector("#product-form-error");

      const updateMarginPreview = () => {
        const cost = Number(costInput.value || 0);
        const sell = isEdit ? Number(product.sellingPrice) : Number(sellInput ? sellInput.value || 0 : 0);
        if (Number.isNaN(cost) || Number.isNaN(sell)) {
          marginValEl.textContent = "-";
          return;
        }
        const diff = sell - cost;
        const pct = sell > 0 ? (diff / sell) * 100 : null;
        const warn = sell < cost ? " (Below Cost)" : "";
        marginValEl.textContent = `${formatCurrency(diff)} | Margin: ${pct === null ? "N/A" : formatMarginPercent(pct)}${warn}`;
        marginValEl.style.color = sell < cost ? "var(--error-text)" : "var(--text-primary)";
      };

      costInput.addEventListener("input", updateMarginPreview);
      if (sellInput) sellInput.addEventListener("input", updateMarginPreview);
      updateMarginPreview();

      root.querySelector("#product-modal-form").addEventListener("submit", async e => {
        e.preventDefault();
        errBox.classList.add("hidden");
        const submitBtn = root.querySelector("#pf-submit");
        submitBtn.disabled = true;

        const formData = new FormData(e.target);
        const payload = {
          name: String(formData.get("name") || "").trim(),
          category: String(formData.get("category") || "").trim(),
          description: String(formData.get("description") || "").trim(),
          costPrice: String(formData.get("costPrice") || "0"),
          quantity: Number(formData.get("quantity") || 0)
        };
        if (!isEdit) {
          const customId = String(formData.get("productId") || "").trim();
          if (customId) payload.productId = customId;
          payload.sellingPrice = String(formData.get("sellingPrice") || "0");
        }

        try {
          const saved = isEdit
            ? await api.put(`/api/products/${encodeURIComponent(product.productId)}`, payload)
            : await api.post("/api/products", payload);
          closeModal();
          showToast(isEdit ? `Updated ${saved.name}` : `Added ${saved.name} (${saved.productId})`, "success");
          if (typeof onSaved === "function") await onSaved(saved);
        } catch (err) {
          errBox.textContent = err.message;
          errBox.classList.remove("hidden");
          submitBtn.disabled = false;
        }
      });
    }
  });
}

export async function openAdjustPriceModal({ product, onSaved = null }) {
  if (product.status === "ARCHIVED") {
    showToast("Restore this archived product before updating its selling price.", "warning");
    return;
  }
  const reasonsData = await api.get("/api/pricing/reasons");
  const reasons = reasonsData.items || [];
  let latestProposal = null;

  openModal({
    title: `Update Selling Price — ${product.name}`,
    size: "lg",
    bodyHtml: `
      <div id="price-modal-error" class="notice notice-error hidden"></div>
      <div class="preview-box">
        <div class="kv-row"><span class="kv-label">Product ID</span><span class="kv-val mono">${escapeHtml(product.productId)}</span></div>
        <div class="kv-row"><span class="kv-label">Cost Price</span><span class="kv-val num">${escapeHtml(formatCurrency(product.costPrice))}</span></div>
        <div class="kv-row"><span class="kv-label">Current Selling Price</span><span class="kv-val num">${escapeHtml(formatCurrency(product.sellingPrice))}</span></div>
      </div>

      <form id="price-adjust-form" class="form-grid">
        <div class="form-field">
          <label class="form-label" for="pm-new-price">New Selling Price *</label>
          <input class="input num" id="pm-new-price" type="number" min="0" step="0.01" required
                 value="${Number(product.sellingPrice).toFixed(2)}">
        </div>
        <div class="form-field">
          <label class="form-label" for="pm-reason-select">Business Reason *</label>
          <select class="select" id="pm-reason-select">
            ${reasons.map(r => `<option value="${escapeHtml(r.label)}">${escapeHtml(r.label)}</option>`).join("")}
            <option value="__CUSTOM__">Custom Reason...</option>
          </select>
        </div>
        <div class="form-field full hidden" id="pm-custom-reason-wrap">
          <label class="form-label" for="pm-custom-reason">Specify Reason *</label>
          <input class="input" id="pm-custom-reason" placeholder="Enter business justification for audit trail" maxlength="160">
        </div>
      </form>

      <div id="pm-proposal-preview" class="preview-box hidden"></div>
    `,
    footerHtml: `
      <button type="button" class="btn btn-secondary" id="pm-cancel">Cancel</button>
      <button type="button" class="btn btn-secondary" id="pm-preview-btn">Preview Impact</button>
      <button type="button" class="btn btn-primary hidden" id="pm-apply-btn">Confirm &amp; Update Price</button>
    `,
    onMount: root => {
      root.querySelector("#pm-cancel").addEventListener("click", closeModal);
      const priceInput = root.querySelector("#pm-new-price");
      const reasonSelect = root.querySelector("#pm-reason-select");
      const customWrap = root.querySelector("#pm-custom-reason-wrap");
      const customInput = root.querySelector("#pm-custom-reason");
      const errBox = root.querySelector("#price-modal-error");
      const previewBox = root.querySelector("#pm-proposal-preview");
      const previewBtn = root.querySelector("#pm-preview-btn");
      const applyBtn = root.querySelector("#pm-apply-btn");

      reasonSelect.addEventListener("change", () => {
        const isCustom = reasonSelect.value === "__CUSTOM__";
        customWrap.classList.toggle("hidden", !isCustom);
        applyBtn.classList.add("hidden");
      });

      const resetConfirmation = () => {
        latestProposal = null;
        applyBtn.classList.add("hidden");
      };
      priceInput.addEventListener("input", resetConfirmation);
      customInput.addEventListener("input", resetConfirmation);

      const getReasonValue = () => {
        if (reasonSelect.value === "__CUSTOM__") {
          return customInput.value.trim();
        }
        return reasonSelect.value;
      };

      previewBtn.addEventListener("click", async () => {
        errBox.classList.add("hidden");
        previewBox.classList.add("hidden");
        applyBtn.classList.add("hidden");

        try {
          const proposal = await api.post("/api/pricing/preview", {
            productId: product.productId,
            proposedPrice: priceInput.value,
            reason: getReasonValue()
          });
          latestProposal = proposal;

          const belowCostBanner = proposal.belowCost
            ? `<div class="notice notice-warning" style="margin-bottom:8px;">Warning: Proposed price (${escapeHtml(formatCurrency(proposal.proposedPrice))}) is below cost (${escapeHtml(formatCurrency(proposal.costPrice))}).</div>`
            : "";

          const invalidBanner = !proposal.valid
            ? `<div class="notice notice-error" style="margin-bottom:8px;">${escapeHtml((proposal.errors || []).join(" "))}</div>`
            : "";

          previewBox.innerHTML = `
            ${invalidBanner}
            ${belowCostBanner}
            <div class="kv-row"><span class="kv-label">Old Selling Price</span><span class="kv-val num">${escapeHtml(formatCurrency(proposal.currentPrice))}</span></div>
            <div class="kv-row"><span class="kv-label">Proposed Selling Price</span><span class="kv-val num">${escapeHtml(formatCurrency(proposal.proposedPrice))}</span></div>
            <div class="kv-row"><span class="kv-label">Price Difference</span><span class="kv-val num">${escapeHtml(formatCurrency(proposal.difference))} (${escapeHtml(formatPercent(proposal.percentageChange))})</span></div>
            <div class="kv-row"><span class="kv-label">New Gross Margin</span><span class="kv-val num">${escapeHtml(formatCurrency(proposal.newMarginAmount))} (${escapeHtml(formatMarginPercent(proposal.newMarginPercentage))})</span></div>
          `;
          previewBox.classList.remove("hidden");

          if (proposal.valid) {
            applyBtn.classList.remove("hidden");
          }
        } catch (err) {
          errBox.textContent = err.message;
          errBox.classList.remove("hidden");
        }
      });

      applyBtn.addEventListener("click", async () => {
        if (!latestProposal || !latestProposal.valid) return;
        applyBtn.disabled = true;
        errBox.classList.add("hidden");
        try {
          const res = await api.post("/api/pricing/apply", {
            productId: product.productId,
            proposedPrice: String(latestProposal.proposedPrice),
            reason: latestProposal.reason
          });
          closeModal();
          showToast(`Updated price for ${product.name} (${res.history.historyId})`, "success");
          if (typeof onSaved === "function") await onSaved(res);
        } catch (err) {
          errBox.textContent = err.message;
          errBox.classList.remove("hidden");
          applyBtn.disabled = false;
        }
      });
    }
  });
}

export function confirmDeleteProduct(product, onDeleted) {
  confirmDialog({
    title: `Delete Product ${product.productId}?`,
    message: `Permanently delete "${product.name}" and its price audit history? If you want to preserve historical audit records, use Archive instead.`,
    confirmLabel: "Permanently Delete",
    danger: true,
    onConfirm: async () => {
      await api.del(`/api/products/${encodeURIComponent(product.productId)}`);
      showToast(`Deleted ${product.name}`, "success");
      if (typeof onDeleted === "function") await onDeleted();
    }
  });
}
