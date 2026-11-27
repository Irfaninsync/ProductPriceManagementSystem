import { api } from "../api.js";
import { renderHorizontalBarList, renderPriceDistributionSvg } from "../components/chart.js";
import { openProductFormModal } from "../components/productModals.js";
import { showToast } from "../components/toast.js";
import { escapeHtml, formatCurrency, formatDateTime, formatMarginPercent, formatPercent } from "../format.js";

export async function renderDashboard(container, { onNavigate, onRefreshShell }) {
  container.innerHTML = `<div class="panel"><div class="panel-body">Loading dashboard metrics...</div></div>`;
  const data = await api.get("/api/dashboard");
  const k = data.kpis;

  if (data.isEmptyCatalog) {
    container.innerHTML = `
      <div class="panel">
        <div class="empty-state">
          <div class="empty-state-title">Your local business catalog is ready</div>
          <div class="empty-state-copy">
            All records are saved locally on this computer in SQLite. Start by adding your first product,
            importing an existing CSV file, or optionally loading a sample dataset for evaluation.
          </div>
          <div style="display:flex;gap:10px;flex-wrap:wrap;justify-content:center;margin-top:8px;">
            <button type="button" class="btn btn-primary" id="dash-empty-add">Add First Product</button>
            <a href="#/settings" class="btn btn-secondary">Import from CSV</a>
            ${data.hasLegacyCsv ? `<button type="button" class="btn btn-secondary" id="dash-import-legacy">Import Legacy data/products.csv</button>` : ""}
            <button type="button" class="btn btn-ghost" id="dash-load-demo">Load Sample Catalog</button>
          </div>
        </div>
      </div>
    `;

    container.querySelector("#dash-empty-add").addEventListener("click", () => {
      openProductFormModal({
        onSaved: async () => {
          if (onRefreshShell) await onRefreshShell();
          await renderDashboard(container, { onNavigate, onRefreshShell });
        }
      });
    });

    const legacyBtn = container.querySelector("#dash-import-legacy");
    if (legacyBtn) {
      legacyBtn.addEventListener("click", async () => {
        legacyBtn.disabled = true;
        try {
          const res = await api.post("/api/data/import-legacy-csv", {});
          showToast(`Imported ${res.importedProducts} products and ${res.importedHistory} price records.`, "success");
          await renderDashboard(container, { onNavigate, onRefreshShell });
        } catch (err) {
          showToast(err.message, "error");
          legacyBtn.disabled = false;
        }
      });
    }

    container.querySelector("#dash-load-demo").addEventListener("click", async e => {
      const btn = e.currentTarget;
      btn.disabled = true;
      try {
        const res = await api.post("/api/data/load-demo", {});
        showToast(`Loaded ${res.loadedProducts} sample demonstration products.`, "success");
        await renderDashboard(container, { onNavigate, onRefreshShell });
      } catch (err) {
        showToast(err.message, "error");
        btn.disabled = false;
      }
    });
    return;
  }

  const alertNotices = [];
  if (k.belowCostCount > 0) {
    alertNotices.push(`
      <div class="notice notice-error">
        <div><strong>Below-Cost Alert:</strong> ${k.belowCostCount} active product(s) are priced below procurement cost.</div>
        <a href="#/products?belowCost=true" class="link-btn">Review Below-Cost</a>
      </div>
    `);
  }
  if (k.lowStockCount > 0 || k.outOfStockCount > 0) {
    alertNotices.push(`
      <div class="notice notice-warning">
        <div><strong>Inventory Alert:</strong> ${k.lowStockCount} low-stock (&le; ${k.lowStockThreshold} units) and ${k.outOfStockCount} out-of-stock active item(s).</div>
        <a href="#/products?lowStock=true" class="link-btn">View Low Stock</a>
      </div>
    `);
  }

  const categoryBars = (data.categoryValuation || [])
    .filter(c => Number(c.totalSellingValue) > 0 || c.activeProducts > 0)
    .slice(0, 6)
    .map(c => ({
      label: `${c.name} (${c.activeProducts})`,
      value: Number(c.totalSellingValue),
      formattedValue: formatCurrency(c.totalSellingValue)
    }));

  const recentPriceRows = (data.recentPriceUpdates || []).length === 0
    ? `<tr><td colspan="5" style="text-align:center;color:var(--text-secondary);padding:20px;">No price updates recorded yet.</td></tr>`
    : data.recentPriceUpdates.map(h => `
        <tr>
          <td><a href="#/products/${encodeURIComponent(h.productId)}" class="link-btn">${escapeHtml(h.productName)}</a><div class="mono" style="font-size:11.5px;color:var(--text-secondary);">${escapeHtml(h.productId)}</div></td>
          <td class="right num">${escapeHtml(formatCurrency(h.oldPrice))}</td>
          <td class="right num" style="font-weight:600;">${escapeHtml(formatCurrency(h.newPrice))}</td>
          <td class="right num"><span class="badge ${h.isIncrease ? "badge-success" : "badge-warning"}">${escapeHtml(formatPercent(h.percentageChange))}</span></td>
          <td style="color:var(--text-secondary);font-size:12px;">${escapeHtml(formatDateTime(h.changedAt))}</td>
        </tr>
      `).join("");

  const recentProductRows = (data.recentProducts || []).map(p => `
    <tr>
      <td><a href="#/products/${encodeURIComponent(p.productId)}" class="link-btn">${escapeHtml(p.name)}</a><div class="mono" style="font-size:11.5px;color:var(--text-secondary);">${escapeHtml(p.productId)}</div></td>
      <td>${escapeHtml(p.category)}</td>
      <td class="right num">${escapeHtml(formatCurrency(p.sellingPrice))}</td>
      <td class="right num">${p.quantity}</td>
    </tr>
  `).join("");

  const activityRows = (data.recentActivity || []).length === 0
    ? `<div style="color:var(--text-secondary);font-size:13px;">No activity logged yet.</div>`
    : data.recentActivity.map(a => `
        <div class="kv-row" style="align-items:flex-start;padding:8px 0;">
          <div style="min-width:0;flex:1 1 200px;">
            <div style="font-size:13px;color:var(--text-primary);overflow-wrap:anywhere;">${escapeHtml(a.summary)}</div>
            <div style="font-size:11.5px;color:var(--text-secondary);">${escapeHtml(formatDateTime(a.createdAt))}</div>
          </div>
          <span class="badge badge-neutral mono" style="display:inline-block;max-width:210px;overflow:hidden;text-overflow:ellipsis;vertical-align:top;" title="${escapeHtml(a.entityId || a.eventType)}">${escapeHtml(a.entityId || a.eventType)}</span>
        </div>
      `).join("");

  container.innerHTML = `
    <div class="stack-lg">
      ${alertNotices.join("")}

      <section class="kpi-strip" aria-label="Key Business Metrics">
        <div class="kpi-cell">
          <div class="kpi-label">Active Products</div>
          <div class="kpi-value">${k.activeProducts}</div>
          <div class="kpi-meta">${k.totalProducts} total (${k.archivedProducts} archived) &bull; ${k.totalStockUnits.toLocaleString()} units</div>
        </div>
        <div class="kpi-cell">
          <div class="kpi-label">Categories</div>
          <div class="kpi-value">${k.totalCategories}</div>
          <div class="kpi-meta">${k.totalHistoryRecords} price changes audited</div>
        </div>
        <div class="kpi-cell">
          <div class="kpi-label">Inventory Retail Value</div>
          <div class="kpi-value">${escapeHtml(formatCurrency(k.totalInventorySellingValue))}</div>
          <div class="kpi-meta">Cost basis: ${escapeHtml(formatCurrency(k.totalInventoryCostValue))} &bull; Margin: ${escapeHtml(formatMarginPercent(k.overallGrossMarginPercentage))}</div>
        </div>
        <div class="kpi-cell">
          <div class="kpi-label">Average Active Price</div>
          <div class="kpi-value">${escapeHtml(formatCurrency(k.avgSellingPrice))}</div>
          <div class="kpi-meta">Median: ${escapeHtml(formatCurrency(k.medianSellingPrice))} &bull; Range: ${escapeHtml(formatCurrency(k.minSellingPrice))}–${escapeHtml(formatCurrency(k.maxSellingPrice))}</div>
        </div>
      </section>

      <div class="grid-2">
        <section class="panel">
          <div class="panel-header">
            <div>
              <h2 class="panel-title">Inventory Valuation by Category</h2>
              <p class="panel-subtitle">Active stock retail value across top categories</p>
            </div>
            <a href="#/categories" class="link-btn" style="font-size:12.5px;">Manage Categories</a>
          </div>
          <div class="panel-body">
            ${renderHorizontalBarList(categoryBars, { emptyText: "No category valuation data yet." })}
          </div>
        </section>

        <section class="panel">
          <div class="panel-header">
            <div>
              <h2 class="panel-title">Product Price Overview</h2>
              <p class="panel-subtitle">Distribution of active selling prices across price bands</p>
            </div>
            <a href="#/reports" class="link-btn" style="font-size:12.5px;">Full Reports</a>
          </div>
          <div class="panel-body">
            ${renderPriceDistributionSvg(data.priceBands)}
          </div>
        </section>
      </div>

      <div class="grid-2">
        <section class="panel">
          <div class="panel-header">
            <div>
              <h2 class="panel-title">Recently Updated Prices</h2>
              <p class="panel-subtitle">Latest audited selling price adjustments</p>
            </div>
            <a href="#/pricing" class="link-btn" style="font-size:12.5px;">Update Prices</a>
          </div>
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Product</th>
                  <th class="right">Previous</th>
                  <th class="right">Updated</th>
                  <th class="right">Change</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>${recentPriceRows}</tbody>
            </table>
          </div>
        </section>

        <section class="panel">
          <div class="panel-header">
            <div>
              <h2 class="panel-title">Recently Added Products</h2>
              <p class="panel-subtitle">Newest SKUs registered in local inventory</p>
            </div>
            <a href="#/products" class="link-btn" style="font-size:12.5px;">All Products</a>
          </div>
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Product</th>
                  <th>Category</th>
                  <th class="right">Selling Price</th>
                  <th class="right">Qty</th>
                </tr>
              </thead>
              <tbody>${recentProductRows}</tbody>
            </table>
          </div>
        </section>
      </div>

      <section class="panel">
        <div class="panel-header">
          <div>
            <h2 class="panel-title">Recent Activity</h2>
            <p class="panel-subtitle">Real-time ledger of catalog, pricing, category, and backup events</p>
          </div>
        </div>
        <div class="panel-body">${activityRows}</div>
      </section>
    </div>
  `;
}
