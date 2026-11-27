import { api } from "../api.js";
import { showToast } from "../components/toast.js";
import { escapeHtml, formatCurrency, formatDateTime, formatMarginPercent, formatPercent, formatSignedCurrency } from "../format.js";

export async function renderPricing(container) {
  container.innerHTML = `<div class="panel"><div class="panel-body">Loading pricing engine...</div></div>`;

  const [prodResp, catResp, reasonResp] = await Promise.all([
    api.get("/api/products?status=ACTIVE&sortBy=name&asc=true&pageSize=200"),
    api.get("/api/categories"),
    api.get("/api/pricing/reasons")
  ]);

  const activeProducts = prodResp.items || [];
  const categories = catResp.items || [];
  const reasons = reasonResp.items || [];
  let activeTab = "single";

  container.innerHTML = `
    <div class="stack-lg">
      <div class="panel">
        <div class="tab-bar" role="tablist">
          <button type="button" class="tab-btn active" data-tab="single">Single Price Update</button>
          <button type="button" class="tab-btn" data-tab="bulk">Bulk Percentage Revaluation</button>
          <button type="button" class="tab-btn" data-tab="history">Price Change History</button>
        </div>
        <div id="pricing-tab-slot" class="panel-body"></div>
      </div>
    </div>
  `;

  const slot = container.querySelector("#pricing-tab-slot");
  const tabs = container.querySelectorAll(".tab-btn");

  const switchTab = tab => {
    activeTab = tab;
    tabs.forEach(t => t.classList.toggle("active", t.dataset.tab === tab));
    if (tab === "single") renderSingleTab();
    else if (tab === "bulk") renderBulkTab();
    else renderHistoryTab();
  };

  tabs.forEach(btn => btn.addEventListener("click", () => switchTab(btn.dataset.tab)));

  function renderSingleTab() {
    if (activeProducts.length === 0) {
      slot.innerHTML = `<div class="empty-state"><div class="empty-state-title">No active products</div><div class="empty-state-copy">Add active products before modifying prices.</div></div>`;
      return;
    }

    let selectedProduct = activeProducts[0];
    let currentProposal = null;

    slot.innerHTML = `
      <div class="grid-2">
        <div class="stack-md">
          <div id="sp-error" class="notice notice-error hidden"></div>
          <div class="form-field">
            <label class="form-label" for="sp-product">Select Active Product</label>
            <select class="select" id="sp-product">
              ${activeProducts.map(p => `<option value="${escapeHtml(p.productId)}">${escapeHtml(p.name)} (${escapeHtml(p.productId)}) — ${escapeHtml(formatCurrency(p.sellingPrice))}</option>`).join("")}
            </select>
          </div>
          <div class="preview-box" id="sp-current-card"></div>
          <div class="form-grid">
            <div class="form-field">
              <label class="form-label" for="sp-new-price">Proposed Selling Price *</label>
              <input type="number" class="input num" id="sp-new-price" min="0" step="0.01" value="${Number(selectedProduct.sellingPrice).toFixed(2)}">
            </div>
            <div class="form-field">
              <label class="form-label" for="sp-reason">Reason *</label>
              <select class="select" id="sp-reason">
                ${reasons.map(r => `<option value="${escapeHtml(r.label)}">${escapeHtml(r.label)}</option>`).join("")}
                <option value="__CUSTOM__">Custom Reason...</option>
              </select>
            </div>
            <div class="form-field full hidden" id="sp-custom-wrap">
              <label class="form-label" for="sp-custom-reason">Custom Reason *</label>
              <input type="text" class="input" id="sp-custom-reason" placeholder="Enter reason for audit record">
            </div>
          </div>
          <div style="display:flex;gap:10px;">
            <button type="button" class="btn btn-secondary" id="sp-preview-btn">Preview Price Comparison</button>
            <button type="button" class="btn btn-primary hidden" id="sp-apply-btn">Confirm &amp; Apply Change</button>
          </div>
        </div>

        <div class="stack-md">
          <div class="panel" style="background:var(--surface-secondary);">
            <div class="panel-header"><h3 class="panel-title">Pre-Commit Price Comparison</h3></div>
            <div class="panel-body" id="sp-preview-panel">
              <p style="margin:0;color:var(--text-secondary);">Enter a new price and click <strong>Preview Price Comparison</strong> to inspect the exact price delta, percentage change, and profit margin impact before committing.</p>
            </div>
          </div>
        </div>
      </div>
    `;

    const prodSel = slot.querySelector("#sp-product");
    const newPriceIn = slot.querySelector("#sp-new-price");
    const reasonSel = slot.querySelector("#sp-reason");
    const customWrap = slot.querySelector("#sp-custom-wrap");
    const customIn = slot.querySelector("#sp-custom-reason");
    const currentCard = slot.querySelector("#sp-current-card");
    const previewPanel = slot.querySelector("#sp-preview-panel");
    const errBox = slot.querySelector("#sp-error");
    const previewBtn = slot.querySelector("#sp-preview-btn");
    const applyBtn = slot.querySelector("#sp-apply-btn");

    const paintCurrent = () => {
      currentCard.innerHTML = `
        <div class="kv-row"><span class="kv-label">Category</span><span class="kv-val">${escapeHtml(selectedProduct.category)}</span></div>
        <div class="kv-row"><span class="kv-label">Cost Price</span><span class="kv-val num">${escapeHtml(formatCurrency(selectedProduct.costPrice))}</span></div>
        <div class="kv-row"><span class="kv-label">Current Selling Price</span><span class="kv-val num">${escapeHtml(formatCurrency(selectedProduct.sellingPrice))}</span></div>
        <div class="kv-row"><span class="kv-label">Current Gross Margin</span><span class="kv-val num">${escapeHtml(formatCurrency(selectedProduct.grossMarginAmount))} (${escapeHtml(formatMarginPercent(selectedProduct.grossMarginPercentage))})</span></div>
      `;
    };
    paintCurrent();

    prodSel.addEventListener("change", () => {
      selectedProduct = activeProducts.find(p => p.productId === prodSel.value) || activeProducts[0];
      newPriceIn.value = Number(selectedProduct.sellingPrice).toFixed(2);
      currentProposal = null;
      applyBtn.classList.add("hidden");
      paintCurrent();
    });

    reasonSel.addEventListener("change", () => {
      customWrap.classList.toggle("hidden", reasonSel.value !== "__CUSTOM__");
      applyBtn.classList.add("hidden");
    });

    newPriceIn.addEventListener("input", () => { applyBtn.classList.add("hidden"); currentProposal = null; });
    customIn.addEventListener("input", () => { applyBtn.classList.add("hidden"); currentProposal = null; });

    const getReason = () => reasonSel.value === "__CUSTOM__" ? customIn.value.trim() : reasonSel.value;

    previewBtn.addEventListener("click", async () => {
      errBox.classList.add("hidden");
      applyBtn.classList.add("hidden");
      try {
        const prop = await api.post("/api/pricing/preview", {
          productId: selectedProduct.productId,
          proposedPrice: newPriceIn.value,
          reason: getReason()
        });
        currentProposal = prop;
        previewPanel.innerHTML = `
          ${!prop.valid ? `<div class="notice notice-error" style="margin-bottom:10px;">${escapeHtml((prop.errors || []).join(" "))}</div>` : ""}
          ${prop.belowCost ? `<div class="notice notice-warning" style="margin-bottom:10px;">Below-cost warning: Selling below procurement cost (${escapeHtml(formatCurrency(prop.costPrice))}).</div>` : ""}
          <div class="kv-row"><span class="kv-label">Current Price</span><span class="kv-val num">${escapeHtml(formatCurrency(prop.currentPrice))}</span></div>
          <div class="kv-row"><span class="kv-label">Proposed Price</span><span class="kv-val num">${escapeHtml(formatCurrency(prop.proposedPrice))}</span></div>
          <div class="kv-row"><span class="kv-label">Price Change</span><span class="kv-val num">${escapeHtml(formatSignedCurrency(prop.difference))} (${escapeHtml(formatPercent(prop.percentageChange))})</span></div>
          <div class="kv-row"><span class="kv-label">Resulting Unit Margin</span><span class="kv-val num">${escapeHtml(formatCurrency(prop.newMarginAmount))} (${escapeHtml(formatMarginPercent(prop.newMarginPercentage))})</span></div>
          <div class="kv-row"><span class="kv-label">Audit Reason</span><span class="kv-val">${escapeHtml(prop.reason)}</span></div>
        `;
        if (prop.valid) applyBtn.classList.remove("hidden");
      } catch (err) {
        errBox.textContent = err.message;
        errBox.classList.remove("hidden");
      }
    });

    applyBtn.addEventListener("click", async () => {
      if (!currentProposal || !currentProposal.valid) return;
      applyBtn.disabled = true;
      try {
        const res = await api.post("/api/pricing/apply", {
          productId: selectedProduct.productId,
          proposedPrice: String(currentProposal.proposedPrice),
          reason: currentProposal.reason
        });
        showToast(`Price updated (${res.history.historyId})`, "success");
        await renderPricing(container);
      } catch (err) {
        errBox.textContent = err.message;
        errBox.classList.remove("hidden");
        applyBtn.disabled = false;
      }
    });
  }

  function renderBulkTab() {
    let lastPreviewParams = null;

    slot.innerHTML = `
      <div class="stack-md">
        <div id="bk-error" class="notice notice-error hidden"></div>
        <div class="form-grid">
          <div class="form-field">
            <label class="form-label" for="bk-scope">Target Scope</label>
            <select class="select" id="bk-scope">
              <option value="ALL">All Active Products</option>
              <option value="CATEGORY">Specific Category</option>
            </select>
          </div>
          <div class="form-field hidden" id="bk-cat-wrap">
            <label class="form-label" for="bk-cat">Category</label>
            <select class="select" id="bk-cat">
              ${categories.map(c => `<option value="${escapeHtml(c.name)}">${escapeHtml(c.name)} (${c.activeProducts})</option>`).join("")}
            </select>
          </div>
          <div class="form-field">
            <label class="form-label" for="bk-pct">Percentage Adjustment (%)</label>
            <input type="number" class="input num" id="bk-pct" step="0.1" min="-99" max="1000" placeholder="e.g. 5 or -10">
          </div>
          <div class="form-field">
            <label class="form-label" for="bk-reason">Audit Reason *</label>
            <input type="text" class="input" id="bk-reason" value="Bulk Pricing Revaluation">
          </div>
        </div>
        <div style="display:flex;gap:10px;flex-wrap:wrap;">
          <button type="button" class="btn btn-secondary" id="bk-preview-btn">Preview Bulk Changes</button>
          <button type="button" class="btn btn-primary hidden" id="bk-apply-btn">Confirm &amp; Apply All Valid Updates</button>
        </div>
        <div id="bk-preview-table-wrap"></div>
      </div>
    `;

    const scopeSel = slot.querySelector("#bk-scope");
    const catWrap = slot.querySelector("#bk-cat-wrap");
    const catSel = slot.querySelector("#bk-cat");
    const pctIn = slot.querySelector("#bk-pct");
    const reasonIn = slot.querySelector("#bk-reason");
    const errBox = slot.querySelector("#bk-error");
    const previewBtn = slot.querySelector("#bk-preview-btn");
    const applyBtn = slot.querySelector("#bk-apply-btn");
    const tableWrap = slot.querySelector("#bk-preview-table-wrap");

    scopeSel.addEventListener("change", () => {
      catWrap.classList.toggle("hidden", scopeSel.value !== "CATEGORY");
      applyBtn.classList.add("hidden");
    });

    previewBtn.addEventListener("click", async () => {
      errBox.classList.add("hidden");
      applyBtn.classList.add("hidden");
      const params = {
        scope: scopeSel.value,
        category: catSel ? catSel.value : "",
        percentage: pctIn.value,
        reason: reasonIn.value.trim()
      };
      try {
        const res = await api.post("/api/pricing/bulk/preview", params);
        lastPreviewParams = params;
        const rows = (res.proposals || []).map(p => `
          <tr>
            <td class="mono" style="font-size:12px;">${escapeHtml(p.productId)}</td>
            <td>${escapeHtml(p.productName)}</td>
            <td class="right num">${escapeHtml(formatCurrency(p.currentPrice))}</td>
            <td class="right num" style="font-weight:600;">${escapeHtml(formatCurrency(p.proposedPrice))}</td>
            <td class="right num">${escapeHtml(formatSignedCurrency(p.difference))}</td>
            <td class="right num">${escapeHtml(formatPercent(p.percentageChange))}</td>
            <td>${p.valid ? (p.belowCost ? `<span class="badge badge-warning">Valid (Below Cost)</span>` : `<span class="badge badge-success">Ready</span>`) : `<span class="badge badge-error">${escapeHtml((p.errors || [])[0] || "Invalid")}</span>`}</td>
          </tr>
        `).join("");

        tableWrap.innerHTML = `
          <div class="notice notice-info" style="margin-bottom:10px;">
            Previewing <strong>${res.totalCount}</strong> product(s): <strong>${res.validCount}</strong> valid change(s) ready. An automatic backup snapshot will be created before applying.
          </div>
          <div class="table-wrap panel">
            <table class="data-table">
              <thead>
                <tr><th>ID</th><th>Product</th><th class="right">Old Price</th><th class="right">New Price</th><th class="right">Diff</th><th class="right">%</th><th>Status</th></tr>
              </thead>
              <tbody>${rows || `<tr><td colspan="7" style="text-align:center;padding:18px;">No matching active products.</td></tr>`}</tbody>
            </table>
          </div>
        `;
        if (res.validCount > 0) applyBtn.classList.remove("hidden");
      } catch (err) {
        errBox.textContent = err.message;
        errBox.classList.remove("hidden");
      }
    });

    applyBtn.addEventListener("click", async () => {
      if (!lastPreviewParams) return;
      applyBtn.disabled = true;
      try {
        const res = await api.post("/api/pricing/bulk/apply", lastPreviewParams);
        showToast(`Bulk adjustment applied to ${res.appliedCount} product(s).`, "success");
        await renderPricing(container);
      } catch (err) {
        errBox.textContent = err.message;
        errBox.classList.remove("hidden");
        applyBtn.disabled = false;
      }
    });
  }

  async function renderHistoryTab() {
    const histState = { productId: "", reason: "", startDate: "", endDate: "", page: 1, pageSize: 20 };

    slot.innerHTML = `
      <div class="stack-md">
        <div id="hist-extremes" class="grid-2"></div>
        <div class="filter-bar" style="border:1px solid var(--border);border-radius:var(--radius-sm);">
          <input type="text" class="input" id="hf-pid" placeholder="Filter by Product ID..." style="width:160px;">
          <input type="text" class="input" id="hf-reason" placeholder="Filter by reason keyword..." style="flex:1;min-width:180px;">
          <input type="date" class="input" id="hf-start" style="width:145px;" title="Start Date">
          <input type="date" class="input" id="hf-end" style="width:145px;" title="End Date">
          <button type="button" class="btn btn-secondary btn-sm" id="hf-export">Export History CSV</button>
        </div>
        <div class="panel">
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Audit ID</th>
                  <th>Product</th>
                  <th class="right">Old Price</th>
                  <th class="right">New Price</th>
                  <th class="right">Diff</th>
                  <th class="right">% Change</th>
                  <th>Reason</th>
                  <th>Timestamp</th>
                </tr>
              </thead>
              <tbody id="hf-tbody"></tbody>
            </table>
          </div>
          <div class="pagination-bar" id="hf-pag"></div>
        </div>
      </div>
    `;

    const loadHistory = async () => {
      const qs = new URLSearchParams({
        productId: histState.productId,
        reason: histState.reason,
        startDate: histState.startDate,
        endDate: histState.endDate,
        page: String(histState.page),
        pageSize: String(histState.pageSize)
      });
      const res = await api.get(`/api/history?${qs.toString()}`);

      const inc = res.largestIncrease;
      const dec = res.largestDecrease;
      slot.querySelector("#hist-extremes").innerHTML = `
        <div class="preview-box">
          <div class="kpi-label">Largest Single Price Increase</div>
          ${inc ? `<div style="font-weight:600;margin-top:4px;">${escapeHtml(inc.productName)} (${escapeHtml(formatSignedCurrency(inc.difference))} / ${escapeHtml(formatPercent(inc.percentageChange))})</div><div class="kpi-meta">${escapeHtml(inc.reason)} &bull; ${escapeHtml(formatDateTime(inc.changedAt))}</div>`
                : `<div class="kpi-meta" style="margin-top:4px;">No increases recorded yet.</div>`}
        </div>
        <div class="preview-box">
          <div class="kpi-label">Largest Single Price Decrease</div>
          ${dec ? `<div style="font-weight:600;margin-top:4px;">${escapeHtml(dec.productName)} (${escapeHtml(formatSignedCurrency(dec.difference))} / ${escapeHtml(formatPercent(dec.percentageChange))})</div><div class="kpi-meta">${escapeHtml(dec.reason)} &bull; ${escapeHtml(formatDateTime(dec.changedAt))}</div>`
                : `<div class="kpi-meta" style="margin-top:4px;">No decreases recorded yet.</div>`}
        </div>
      `;

      const items = res.items || [];
      slot.querySelector("#hf-tbody").innerHTML = items.length === 0
        ? `<tr><td colspan="8" style="text-align:center;padding:24px;color:var(--text-secondary);">No matching price history records.</td></tr>`
        : items.map(h => `
            <tr>
              <td class="mono" style="font-size:12px;">${escapeHtml(h.historyId)}</td>
              <td><a href="#/products/${encodeURIComponent(h.productId)}" class="link-btn">${escapeHtml(h.productName)}</a> <span class="mono nowrap" style="font-size:11.5px;color:var(--text-secondary);">${escapeHtml(h.productId)}</span></td>
              <td class="right num">${escapeHtml(formatCurrency(h.oldPrice))}</td>
              <td class="right num" style="font-weight:600;">${escapeHtml(formatCurrency(h.newPrice))}</td>
              <td class="right num">${escapeHtml(formatSignedCurrency(h.difference))}</td>
              <td class="right num"><span class="badge ${h.isIncrease ? "badge-success" : "badge-warning"}">${escapeHtml(formatPercent(h.percentageChange))}</span></td>
              <td>${escapeHtml(h.reason)}</td>
              <td style="font-size:12px;color:var(--text-secondary);white-space:nowrap;">${escapeHtml(formatDateTime(h.changedAt))}</td>
            </tr>
          `).join("");

      const pag = slot.querySelector("#hf-pag");
      pag.innerHTML = `
        <div>${res.totalCount} total record(s)</div>
        <div style="display:flex;gap:6px;align-items:center;">
          <button type="button" class="btn btn-secondary btn-sm" id="hf-prev" ${res.page <= 1 ? "disabled" : ""}>Previous</button>
          <span class="num">Page ${res.page} of ${res.totalPages}</span>
          <button type="button" class="btn btn-secondary btn-sm" id="hf-next" ${res.page >= res.totalPages ? "disabled" : ""}>Next</button>
        </div>
      `;
      pag.querySelector("#hf-prev").addEventListener("click", () => { if (histState.page > 1) { histState.page--; loadHistory(); } });
      pag.querySelector("#hf-next").addEventListener("click", () => { if (histState.page < res.totalPages) { histState.page++; loadHistory(); } });
    };

    const sync = () => {
      histState.productId = slot.querySelector("#hf-pid").value.trim();
      histState.reason = slot.querySelector("#hf-reason").value.trim();
      histState.startDate = slot.querySelector("#hf-start").value;
      histState.endDate = slot.querySelector("#hf-end").value;
      histState.page = 1;
      loadHistory();
    };
    ["#hf-pid", "#hf-reason"].forEach(s => slot.querySelector(s).addEventListener("input", sync));
    ["#hf-start", "#hf-end"].forEach(s => slot.querySelector(s).addEventListener("change", sync));
    slot.querySelector("#hf-export").addEventListener("click", () => api.downloadUrl("/api/reports/export/history"));

    await loadHistory();
  }

  switchTab(activeTab);
}
