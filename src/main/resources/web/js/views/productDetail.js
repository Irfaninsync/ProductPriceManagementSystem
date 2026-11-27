import { api } from "../api.js";
import { renderPriceHistoryChart } from "../components/chart.js";
import { confirmDialog } from "../components/modal.js";
import { confirmDeleteProduct, openAdjustPriceModal, openProductFormModal } from "../components/productModals.js";
import { showToast } from "../components/toast.js";
import { escapeHtml, formatCurrency, formatDateTime, formatMarginPercent, formatPercent, formatSignedCurrency } from "../format.js";

export async function renderProductDetail(container, { productId }) {
  container.innerHTML = `<div class="panel"><div class="panel-body">Loading product details...</div></div>`;

  let data;
  try {
    data = await api.get(`/api/products/${encodeURIComponent(productId)}`);
  } catch (err) {
    container.innerHTML = `
      <div class="panel">
        <div class="panel-body">
          <div class="notice notice-error">${escapeHtml(err.message)}</div>
          <div style="margin-top:12px;"><a href="#/products" class="btn btn-secondary">Back to Products</a></div>
        </div>
      </div>
    `;
    return;
  }

  const p = data.product;
  const history = data.priceHistory || [];
  const reversedHistory = [...history].reverse();

  const historyRows = reversedHistory.length === 0
    ? `<tr><td colspan="6" style="text-align:center;color:var(--text-secondary);padding:20px;">No price adjustments recorded for this product yet.</td></tr>`
    : reversedHistory.map(h => `
        <tr>
          <td class="mono" style="font-size:12px;">${escapeHtml(h.historyId)}</td>
          <td class="right num">${escapeHtml(formatCurrency(h.oldPrice))}</td>
          <td class="right num" style="font-weight:600;">${escapeHtml(formatCurrency(h.newPrice))}</td>
          <td class="right num">${escapeHtml(formatSignedCurrency(h.difference))}</td>
          <td class="right num"><span class="badge ${h.isIncrease ? "badge-success" : "badge-warning"}">${escapeHtml(formatPercent(h.percentageChange))}</span></td>
          <td>${escapeHtml(h.reason)}</td>
          <td style="font-size:12px;color:var(--text-secondary);white-space:nowrap;">${escapeHtml(formatDateTime(h.changedAt))}</td>
        </tr>
      `).join("");

  container.innerHTML = `
    <div class="stack-lg">
      <div style="display:flex;align-items:center;justify-content:space-between;gap:12px;flex-wrap:wrap;">
        <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap;">
          <a href="#/products" class="btn btn-secondary btn-sm">&larr; Back to Products</a>
          <span class="badge badge-neutral mono">${escapeHtml(p.productId)}</span>
          <span class="badge ${p.status === "ACTIVE" ? "badge-success" : "badge-neutral"}">${escapeHtml(p.status)}</span>
          ${p.belowCost ? `<span class="badge badge-error">Priced Below Cost</span>` : ""}
        </div>
        <div style="display:flex;gap:8px;flex-wrap:wrap;">
          ${p.status === "ACTIVE" ? `<button type="button" class="btn btn-primary btn-sm" id="pd-adjust-price">Update Selling Price</button>` : ""}
          <button type="button" class="btn btn-secondary btn-sm" id="pd-edit">Edit Details</button>
          ${p.status === "ACTIVE"
            ? `<button type="button" class="btn btn-secondary btn-sm" id="pd-archive">Archive</button>`
            : `<button type="button" class="btn btn-secondary btn-sm" id="pd-restore">Restore</button>`}
          <button type="button" class="btn btn-danger btn-sm" id="pd-delete">Delete</button>
        </div>
      </div>

      <section class="kpi-strip">
        <div class="kpi-cell">
          <div class="kpi-label">Selling Price</div>
          <div class="kpi-value">${escapeHtml(formatCurrency(p.sellingPrice))}</div>
          <div class="kpi-meta">Cost price: ${escapeHtml(formatCurrency(p.costPrice))}</div>
        </div>
        <div class="kpi-cell">
          <div class="kpi-label">Gross Margin / Unit</div>
          <div class="kpi-value" style="color:${p.belowCost ? "var(--error-text)" : "var(--text-primary)"};">${escapeHtml(formatCurrency(p.grossMarginAmount))}</div>
          <div class="kpi-meta">Margin: ${escapeHtml(formatMarginPercent(p.grossMarginPercentage))} &bull; Markup: ${escapeHtml(formatMarginPercent(p.markupPercentage))}</div>
        </div>
        <div class="kpi-cell">
          <div class="kpi-label">Stock On Hand</div>
          <div class="kpi-value">${p.quantity}</div>
          <div class="kpi-meta">Retail stock value: ${escapeHtml(formatCurrency(p.inventorySellingValue))}</div>
        </div>
        <div class="kpi-cell">
          <div class="kpi-label">Price Revisions</div>
          <div class="kpi-value">${history.length}</div>
          <div class="kpi-meta">Cost stock value: ${escapeHtml(formatCurrency(p.inventoryCostValue))}</div>
        </div>
      </section>

      <div class="grid-2">
        <section class="panel">
          <div class="panel-header"><h2 class="panel-title">Product Specification</h2></div>
          <div class="panel-body">
            <div class="kv-row"><span class="kv-label">Product Name</span><span class="kv-val">${escapeHtml(p.name)}</span></div>
            <div class="kv-row"><span class="kv-label">Category</span><span class="kv-val">${escapeHtml(p.category)}</span></div>
            <div class="kv-row"><span class="kv-label">Description</span><span class="kv-val" style="font-weight:400;text-align:right;max-width:65%;">${escapeHtml(p.description || "—")}</span></div>
            <div class="kv-row"><span class="kv-label">Date Added</span><span class="kv-val" style="font-weight:400;">${escapeHtml(formatDateTime(p.createdAt))}</span></div>
            <div class="kv-row"><span class="kv-label">Last Updated</span><span class="kv-val" style="font-weight:400;">${escapeHtml(formatDateTime(p.updatedAt))}</span></div>
          </div>
        </section>

        <section class="panel">
          <div class="panel-header"><h2 class="panel-title">Price Trend</h2></div>
          <div class="panel-body">
            ${renderPriceHistoryChart(history, p.sellingPrice)}
          </div>
        </section>
      </div>

      <section class="panel">
        <div class="panel-header">
          <div>
            <h2 class="panel-title">Price Change History</h2>
            <p class="panel-subtitle">Immutable audit trail for ${escapeHtml(p.name)}</p>
          </div>
        </div>
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>Audit ID</th>
                <th class="right">Previous Price</th>
                <th class="right">New Price</th>
                <th class="right">Difference</th>
                <th class="right">% Change</th>
                <th>Business Reason</th>
                <th>Date Updated</th>
              </tr>
            </thead>
            <tbody>${historyRows}</tbody>
          </table>
        </div>
      </section>
    </div>
  `;

  const reload = () => renderProductDetail(container, { productId: p.productId });

  const adjBtn = container.querySelector("#pd-adjust-price");
  if (adjBtn) adjBtn.addEventListener("click", () => openAdjustPriceModal({ product: p, onSaved: reload }));
  container.querySelector("#pd-edit").addEventListener("click", () => openProductFormModal({ product: p, onSaved: reload }));

  const archBtn = container.querySelector("#pd-archive");
  if (archBtn) {
    archBtn.addEventListener("click", () => {
      confirmDialog({
        title: `Archive ${p.productId}?`,
        message: "This removes the product from active listings while retaining its price audit history.",
        confirmLabel: "Archive",
        onConfirm: async () => {
          await api.post(`/api/products/${encodeURIComponent(p.productId)}/archive`);
          showToast("Product archived", "success");
          await reload();
        }
      });
    });
  }

  const restBtn = container.querySelector("#pd-restore");
  if (restBtn) {
    restBtn.addEventListener("click", async () => {
      await api.post(`/api/products/${encodeURIComponent(p.productId)}/restore`);
      showToast("Product restored to active catalog", "success");
      await reload();
    });
  }

  container.querySelector("#pd-delete").addEventListener("click", () => {
    confirmDeleteProduct(p, () => {
      window.location.hash = "#/products";
    });
  });
}
