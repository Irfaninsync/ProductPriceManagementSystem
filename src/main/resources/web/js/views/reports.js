import { api } from "../api.js";
import { escapeHtml, formatCurrency, formatDateTime, formatMarginPercent, formatPercent, formatSignedCurrency } from "../format.js";

export async function renderReports(container) {
  let activeTab = "catalogue";

  container.innerHTML = `
    <div class="stack-lg">
      <div class="panel">
        <div class="panel-header no-print">
          <div>
            <h2 class="panel-title">Business Reports &amp; Analytics</h2>
            <p class="panel-subtitle">Audited financial summaries, price comparisons, and exportable records</p>
          </div>
          <div style="display:flex;gap:8px;flex-wrap:wrap;">
            <button type="button" class="btn btn-secondary btn-sm" id="rep-export-csv">Export CSV</button>
            <button type="button" class="btn btn-primary btn-sm" id="rep-print-pdf">Print / Save as PDF</button>
          </div>
        </div>

        <div class="tab-bar no-print" role="tablist">
          <button type="button" class="tab-btn active" data-tab="catalogue">Product Catalogue</button>
          <button type="button" class="tab-btn" data-tab="categories">Category Summary</button>
          <button type="button" class="tab-btn" data-tab="price-comparison">Price Comparison</button>
          <button type="button" class="tab-btn" data-tab="price-history">Price Change History</button>
          <button type="button" class="tab-btn" data-tab="valuation">Inventory Valuation &amp; Risks</button>
        </div>

        <div id="rep-content" class="panel-body">Loading report...</div>
      </div>
    </div>
  `;

  const contentEl = container.querySelector("#rep-content");
  const exportBtn = container.querySelector("#rep-export-csv");
  const printBtn = container.querySelector("#rep-print-pdf");
  const tabBtns = container.querySelectorAll(".tab-btn");

  const exportMap = {
    catalogue: "/api/reports/export/catalogue",
    categories: "/api/reports/export/categories",
    "price-comparison": "/api/reports/export/price-comparison",
    "price-history": "/api/reports/export/history",
    valuation: "/api/reports/export/catalogue"
  };

  exportBtn.addEventListener("click", () => {
    api.downloadUrl(exportMap[activeTab] || "/api/reports/export/catalogue");
  });

  printBtn.addEventListener("click", () => {
    const meta = document.getElementById("print-report-meta");
    if (meta) {
      meta.textContent = `Report: ${activeTab.replace("-", " ").toUpperCase()} | Printed: ${new Date().toLocaleString()}`;
    }
    window.print();
  });

  tabBtns.forEach(btn => btn.addEventListener("click", () => loadTab(btn.dataset.tab)));

  async function loadTab(tab) {
    activeTab = tab;
    tabBtns.forEach(b => b.classList.toggle("active", b.dataset.tab === tab));
    contentEl.innerHTML = `<div style="color:var(--text-secondary);">Generating report from database...</div>`;

    try {
      if (tab === "catalogue") {
        const res = await api.get("/api/reports/catalogue");
        const s = res.summary;
        const rows = (res.items || []).map(p => `
          <tr>
            <td class="mono" style="font-size:12px;">${escapeHtml(p.productId)}</td>
            <td>${escapeHtml(p.name)}</td>
            <td>${escapeHtml(p.category)}</td>
            <td class="right num">${escapeHtml(formatCurrency(p.costPrice))}</td>
            <td class="right num">${escapeHtml(formatCurrency(p.sellingPrice))}</td>
            <td class="right num">${escapeHtml(formatMarginPercent(p.grossMarginPercentage))}</td>
            <td class="right num">${p.quantity}</td>
            <td><span class="badge ${p.status === "ACTIVE" ? "badge-success" : "badge-neutral"}">${escapeHtml(p.status)}</span></td>
          </tr>
        `).join("");
        contentEl.innerHTML = `
          <div class="kpi-strip" style="margin-bottom:16px;">
            <div class="kpi-cell"><div class="kpi-label">Total SKUs</div><div class="kpi-value">${s.totalProducts}</div></div>
            <div class="kpi-cell"><div class="kpi-label">Active SKUs</div><div class="kpi-value">${s.activeProducts}</div></div>
            <div class="kpi-cell"><div class="kpi-label">Total Units</div><div class="kpi-value">${s.totalStockUnits.toLocaleString()}</div></div>
            <div class="kpi-cell"><div class="kpi-label">Retail Valuation</div><div class="kpi-value">${escapeHtml(formatCurrency(s.totalInventorySellingValue))}</div></div>
          </div>
          <div class="table-wrap">
            <table class="data-table">
              <thead><tr><th>ID</th><th>Name</th><th>Category</th><th class="right">Cost</th><th class="right">Selling</th><th class="right">Margin %</th><th class="right">Qty</th><th>Status</th></tr></thead>
              <tbody>${rows || `<tr><td colspan="8" style="text-align:center;padding:20px;">No products stored.</td></tr>`}</tbody>
            </table>
          </div>
        `;
      } else if (tab === "categories") {
        const res = await api.get("/api/reports/categories");
        const t = res.totals;
        const rows = (res.items || []).map(c => `
          <tr>
            <td class="mono" style="font-size:12px;">${escapeHtml(c.categoryId)}</td>
            <td style="font-weight:600;">${escapeHtml(c.name)}</td>
            <td class="right num">${c.activeProducts} (${c.totalProducts})</td>
            <td class="right num">${c.totalUnits.toLocaleString()}</td>
            <td class="right num">${escapeHtml(formatCurrency(c.totalCostValue))}</td>
            <td class="right num">${escapeHtml(formatCurrency(c.totalSellingValue))}</td>
            <td class="right num">${escapeHtml(formatCurrency(c.totalMarginAmount))}</td>
            <td class="right num">${escapeHtml(formatMarginPercent(c.marginPercentage))}</td>
          </tr>
        `).join("");
        contentEl.innerHTML = `
          <div class="kpi-strip" style="margin-bottom:16px;">
            <div class="kpi-cell"><div class="kpi-label">Categories</div><div class="kpi-value">${t.categoryCount}</div></div>
            <div class="kpi-cell"><div class="kpi-label">Active Stock Units</div><div class="kpi-value">${t.totalUnits.toLocaleString()}</div></div>
            <div class="kpi-cell"><div class="kpi-label">Total Cost Basis</div><div class="kpi-value">${escapeHtml(formatCurrency(t.totalCostValue))}</div></div>
            <div class="kpi-cell"><div class="kpi-label">Potential Gross Margin</div><div class="kpi-value">${escapeHtml(formatCurrency(t.totalMarginAmount))}</div></div>
          </div>
          <div class="table-wrap">
            <table class="data-table">
              <thead><tr><th>ID</th><th>Category</th><th class="right">Active (Total)</th><th class="right">Units</th><th class="right">Cost Value</th><th class="right">Selling Value</th><th class="right">Gross Margin</th><th class="right">Margin %</th></tr></thead>
              <tbody>${rows || `<tr><td colspan="8" style="text-align:center;padding:20px;">No categories stored.</td></tr>`}</tbody>
            </table>
          </div>
        `;
      } else if (tab === "price-comparison") {
        const res = await api.get("/api/reports/price-comparison");
        const rows = (res.items || []).map(r => `
          <tr>
            <td class="mono" style="font-size:12px;">${escapeHtml(r.productId)}</td>
            <td><a href="#/products/${encodeURIComponent(r.productId)}" class="link-btn">${escapeHtml(r.name)}</a></td>
            <td>${escapeHtml(r.category)}</td>
            <td class="right num">${escapeHtml(formatCurrency(r.costPrice))}</td>
            <td class="right num" style="font-weight:600;">${escapeHtml(formatCurrency(r.currentPrice))}</td>
            <td class="right num">${escapeHtml(formatMarginPercent(r.marginPercentage))}</td>
            <td class="right num">${r.previousPrice !== null ? escapeHtml(formatCurrency(r.previousPrice)) : "—"}</td>
            <td class="right num">${r.lastDelta !== null ? `${escapeHtml(formatSignedCurrency(r.lastDelta))} (${escapeHtml(formatPercent(r.lastDeltaPercentage))})` : "—"}</td>
            <td style="font-size:12px;color:var(--text-secondary);">${r.lastChangedAt ? escapeHtml(formatDateTime(r.lastChangedAt)) : "Unchanged"}</td>
          </tr>
        `).join("");
        contentEl.innerHTML = `
          <div class="table-wrap">
            <table class="data-table">
              <thead><tr><th>ID</th><th>Product</th><th>Category</th><th class="right">Cost</th><th class="right">Current Price</th><th class="right">Margin %</th><th class="right">Previous Price</th><th class="right">Last Change</th><th>Updated</th></tr></thead>
              <tbody>${rows || `<tr><td colspan="9" style="text-align:center;padding:20px;">No active products to compare.</td></tr>`}</tbody>
            </table>
          </div>
        `;
      } else if (tab === "price-history") {
        const res = await api.get("/api/reports/price-history");
        const rows = (res.items || []).map(h => `
          <tr>
            <td class="mono" style="font-size:12px;">${escapeHtml(h.historyId)}</td>
            <td>${escapeHtml(h.productName)} <span class="mono" style="font-size:11.5px;color:var(--text-secondary);">${escapeHtml(h.productId)}</span></td>
            <td class="right num">${escapeHtml(formatCurrency(h.oldPrice))}</td>
            <td class="right num" style="font-weight:600;">${escapeHtml(formatCurrency(h.newPrice))}</td>
            <td class="right num">${escapeHtml(formatSignedCurrency(h.difference))}</td>
            <td class="right num">${escapeHtml(formatPercent(h.percentageChange))}</td>
            <td>${escapeHtml(h.reason)}</td>
            <td style="font-size:12px;color:var(--text-secondary);">${escapeHtml(formatDateTime(h.changedAt))}</td>
          </tr>
        `).join("");
        contentEl.innerHTML = `
          <div class="table-wrap">
            <table class="data-table">
              <thead><tr><th>Audit ID</th><th>Product</th><th class="right">Old Price</th><th class="right">New Price</th><th class="right">Diff</th><th class="right">%</th><th>Reason</th><th>Date</th></tr></thead>
              <tbody>${rows || `<tr><td colspan="8" style="text-align:center;padding:20px;">No price changes logged yet.</td></tr>`}</tbody>
            </table>
          </div>
        `;
      } else if (tab === "valuation") {
        const res = await api.get("/api/reports/valuation");
        const s = res.summary;
        const renderMiniTable = (list, emptyMsg) => list.length === 0
          ? `<div style="color:var(--text-secondary);font-size:13px;">${escapeHtml(emptyMsg)}</div>`
          : `<div class="table-wrap"><table class="data-table"><thead><tr><th>Product</th><th class="right">Cost</th><th class="right">Selling</th><th class="right">Margin %</th><th class="right">Qty</th></tr></thead><tbody>
              ${list.map(p => `<tr>
                <td><a href="#/products/${encodeURIComponent(p.productId)}" class="link-btn">${escapeHtml(p.name)}</a></td>
                <td class="right num">${escapeHtml(formatCurrency(p.costPrice))}</td>
                <td class="right num">${escapeHtml(formatCurrency(p.sellingPrice))}</td>
                <td class="right num">${escapeHtml(formatMarginPercent(p.grossMarginPercentage))}</td>
                <td class="right num">${p.quantity}</td>
              </tr>`).join("")}
            </tbody></table></div>`;

        contentEl.innerHTML = `
          <div class="stack-lg">
            <div class="kpi-strip">
              <div class="kpi-cell"><div class="kpi-label">Inventory Cost Value</div><div class="kpi-value">${escapeHtml(formatCurrency(s.totalInventoryCostValue))}</div></div>
              <div class="kpi-cell"><div class="kpi-label">Inventory Retail Value</div><div class="kpi-value">${escapeHtml(formatCurrency(s.totalInventorySellingValue))}</div></div>
              <div class="kpi-cell"><div class="kpi-label">Potential Gross Profit</div><div class="kpi-value">${escapeHtml(formatCurrency(s.totalGrossMarginAmount))}</div></div>
              <div class="kpi-cell"><div class="kpi-label">Overall Margin / Markup</div><div class="kpi-value">${escapeHtml(formatMarginPercent(s.overallGrossMarginPercentage))}</div><div class="kpi-meta">Markup: ${escapeHtml(formatMarginPercent(s.overallMarkupPercentage))}</div></div>
            </div>

            <div class="grid-2">
              <div class="panel">
                <div class="panel-header"><h3 class="panel-title">Highest Margin Products</h3></div>
                <div class="panel-body" style="padding:0;">${renderMiniTable(res.topMarginProducts || [], "No active products.")}</div>
              </div>
              <div class="panel">
                <div class="panel-header"><h3 class="panel-title">Below-Cost &amp; Low-Margin Items</h3></div>
                <div class="panel-body" style="padding:0;">${renderMiniTable(res.belowCostProducts?.length ? res.belowCostProducts : (res.lowestMarginProducts || []), "No below-cost items.")}</div>
              </div>
            </div>

            <div class="panel">
              <div class="panel-header"><h3 class="panel-title">Low Stock &amp; Out-of-Stock Alerts (&le; ${s.lowStockThreshold} units)</h3></div>
              <div class="panel-body" style="padding:0;">${renderMiniTable([...(res.outOfStockProducts || []), ...(res.lowStockProducts || [])], "All active products are sufficiently stocked.")}</div>
            </div>
          </div>
        `;
      }
    } catch (err) {
      contentEl.innerHTML = `<div class="notice notice-error">${escapeHtml(err.message)}</div>`;
    }
  }

  await loadTab("catalogue");
}
