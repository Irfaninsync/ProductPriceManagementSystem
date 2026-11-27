import { api } from "../api.js";
import { confirmDialog } from "../components/modal.js";
import { confirmDeleteProduct, openAdjustPriceModal, openProductFormModal } from "../components/productModals.js";
import { showToast } from "../components/toast.js";
import { escapeHtml, formatCurrency, formatDateOnly, formatMarginPercent } from "../format.js";

export async function renderProducts(container, { queryParams = {} } = {}) {
  const state = {
    q: queryParams.q || "",
    category: queryParams.category || "",
    status: queryParams.status || "ACTIVE",
    minPrice: queryParams.minPrice || "",
    maxPrice: queryParams.maxPrice || "",
    belowCost: queryParams.belowCost === "true",
    lowStock: queryParams.lowStock === "true",
    sortBy: queryParams.sortBy || "updated",
    asc: queryParams.asc === "true" ? "true" : "false",
    page: Number(queryParams.page || 1),
    pageSize: 15
  };

  const catResp = await api.get("/api/categories");
  const categories = catResp.items || [];

  container.innerHTML = `
    <div class="stack-lg">
      <div class="panel">
        <div class="panel-header">
          <div>
            <h2 class="panel-title">Product Catalogue</h2>
            <p class="panel-subtitle">Search, filter, sort, update prices, and manage inventory records</p>
          </div>
          <div style="display:flex;gap:8px;flex-wrap:wrap;">
            <button type="button" class="btn btn-secondary btn-sm" id="prd-export-csv">Export CSV</button>
            <button type="button" class="btn btn-primary btn-sm" id="prd-add-new">+ Add Product</button>
          </div>
        </div>

        <form id="prd-filter-form" class="filter-bar">
          <input type="search" class="input" id="flt-q" placeholder="Search ID, name, description..." style="min-width:210px;flex:1;" value="${escapeHtml(state.q)}">
          <select class="select" id="flt-cat" style="width:170px;">
            <option value="">All Categories</option>
            ${categories.map(c => `<option value="${escapeHtml(c.name)}" ${c.name === state.category ? "selected" : ""}>${escapeHtml(c.name)}</option>`).join("")}
          </select>
          <select class="select" id="flt-status" style="width:130px;">
            <option value="ACTIVE" ${state.status === "ACTIVE" ? "selected" : ""}>Active</option>
            <option value="ARCHIVED" ${state.status === "ARCHIVED" ? "selected" : ""}>Archived</option>
            <option value="ALL" ${state.status === "ALL" ? "selected" : ""}>All Status</option>
          </select>
          <input type="number" class="input num" id="flt-min-price" placeholder="Min Price" min="0" step="0.01" style="width:105px;" value="${escapeHtml(state.minPrice)}">
          <input type="number" class="input num" id="flt-max-price" placeholder="Max Price" min="0" step="0.01" style="width:105px;" value="${escapeHtml(state.maxPrice)}">
          <select class="select" id="flt-sort" style="width:160px;">
            <option value="updated:false" ${state.sortBy === "updated" && state.asc === "false" ? "selected" : ""}>Recently Updated</option>
            <option value="name:true" ${state.sortBy === "name" && state.asc === "true" ? "selected" : ""}>Name (A–Z)</option>
            <option value="name:false" ${state.sortBy === "name" && state.asc === "false" ? "selected" : ""}>Name (Z–A)</option>
            <option value="price:true" ${state.sortBy === "price" && state.asc === "true" ? "selected" : ""}>Price (Low–High)</option>
            <option value="price:false" ${state.sortBy === "price" && state.asc === "false" ? "selected" : ""}>Price (High–Low)</option>
            <option value="quantity:true" ${state.sortBy === "quantity" && state.asc === "true" ? "selected" : ""}>Stock (Low–High)</option>
            <option value="id:true" ${state.sortBy === "id" && state.asc === "true" ? "selected" : ""}>Product ID</option>
          </select>
          <label class="checkbox-label">
            <input type="checkbox" id="flt-below-cost" ${state.belowCost ? "checked" : ""}>
            <span>Below Cost</span>
          </label>
          <label class="checkbox-label">
            <input type="checkbox" id="flt-low-stock" ${state.lowStock ? "checked" : ""}>
            <span>Low Stock</span>
          </label>
          <button type="button" class="btn btn-ghost btn-sm" id="flt-reset">Reset</button>
        </form>

        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Product Name</th>
                <th>Category</th>
                <th class="right">Cost Price</th>
                <th class="right">Selling Price</th>
                <th class="right">Margin</th>
                <th class="right">Stock</th>
                <th>Updated</th>
                <th class="right">Actions</th>
              </tr>
            </thead>
            <tbody id="prd-table-body">
              <tr><td colspan="9" style="padding:20px;text-align:center;">Loading products...</td></tr>
            </tbody>
          </table>
        </div>

        <div class="pagination-bar" id="prd-pagination"></div>
      </div>
    </div>
  `;

  const tbody = container.querySelector("#prd-table-body");
  const pagBar = container.querySelector("#prd-pagination");

  const loadRows = async () => {
    const params = new URLSearchParams({
      q: state.q,
      category: state.category,
      status: state.status,
      minPrice: state.minPrice,
      maxPrice: state.maxPrice,
      belowCost: String(state.belowCost),
      lowStock: String(state.lowStock),
      sortBy: state.sortBy,
      asc: state.asc,
      page: String(state.page),
      pageSize: String(state.pageSize)
    });

    try {
      const res = await api.get(`/api/products?${params.toString()}`);
      const items = res.items || [];
      const threshold = res.lowStockThreshold || 10;

      if (items.length === 0) {
        tbody.innerHTML = `
          <tr><td colspan="9">
            <div class="empty-state" style="padding:28px;">
              <div class="empty-state-title">No matching products</div>
              <div class="empty-state-copy">Adjust your search or filters, or add a new product to the catalog.</div>
            </div>
          </td></tr>
        `;
      } else {
        tbody.innerHTML = items.map(p => {
          const stockBadge = p.outOfStock
            ? `<span class="badge badge-error num">0 (Out)</span>`
            : p.quantity <= threshold
              ? `<span class="badge badge-warning num">${p.quantity} (Low)</span>`
              : `<span class="num">${p.quantity}</span>`;

          const statusTag = p.status === "ARCHIVED"
            ? `<span class="badge badge-neutral" style="margin-left:6px;">Archived</span>`
            : p.belowCost
              ? `<span class="badge badge-error" style="margin-left:6px;">Below Cost</span>`
              : "";

          return `
            <tr data-id="${escapeHtml(p.productId)}">
              <td class="mono nowrap" style="font-size:12px;">
                <a href="#/products/${encodeURIComponent(p.productId)}" class="link-btn">${escapeHtml(p.productId)}</a>
              </td>
              <td>
                <div style="max-width:170px;">
                  <a href="#/products/${encodeURIComponent(p.productId)}" class="link-btn" style="color:var(--text-primary);display:inline-block;max-width:100%;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;vertical-align:bottom;" title="${escapeHtml(p.name)}">${escapeHtml(p.name)}</a>
                  ${statusTag}
                  ${p.description ? `<div style="font-size:11.5px;color:var(--text-secondary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;" title="${escapeHtml(p.description)}">${escapeHtml(p.description)}</div>` : ""}
                </div>
              </td>
              <td><span class="badge badge-neutral">${escapeHtml(p.category)}</span></td>
              <td class="right num">${escapeHtml(formatCurrency(p.costPrice))}</td>
              <td class="right num" style="font-weight:600;">${escapeHtml(formatCurrency(p.sellingPrice))}</td>
              <td class="right num" style="color:${p.belowCost ? "var(--error-text)" : "var(--text-secondary)"};">
                ${escapeHtml(formatMarginPercent(p.grossMarginPercentage))}
              </td>
              <td class="right">${stockBadge}</td>
              <td class="nowrap" style="font-size:12px;color:var(--text-secondary);">${escapeHtml(formatDateOnly(p.updatedAt))}</td>
              <td class="right nowrap">
                <div class="row-actions">
                  ${p.status === "ACTIVE" ? `<button type="button" class="btn btn-secondary btn-sm act-price">Price</button>` : ""}
                  <button type="button" class="btn btn-ghost btn-sm act-edit">Edit</button>
                  ${p.status === "ACTIVE"
                    ? `<button type="button" class="btn btn-ghost btn-sm act-archive">Archive</button>`
                    : `<button type="button" class="btn btn-secondary btn-sm act-restore">Restore</button>`}
                  <button type="button" class="btn btn-danger btn-sm act-delete">Delete</button>
                </div>
              </td>
            </tr>
          `;
        }).join("");

        items.forEach(p => {
          const row = tbody.querySelector(`tr[data-id="${CSS.escape(p.productId)}"]`);
          if (!row) return;
          const priceBtn = row.querySelector(".act-price");
          if (priceBtn) priceBtn.addEventListener("click", () => openAdjustPriceModal({ product: p, onSaved: loadRows }));
          row.querySelector(".act-edit").addEventListener("click", () => openProductFormModal({ product: p, onSaved: loadRows }));
          const archBtn = row.querySelector(".act-archive");
          if (archBtn) {
            archBtn.addEventListener("click", () => {
              confirmDialog({
                title: `Archive ${p.productId}?`,
                message: `Archiving "${p.name}" removes it from the active catalog while preserving all price history records.`,
                confirmLabel: "Archive Product",
                onConfirm: async () => {
                  await api.post(`/api/products/${encodeURIComponent(p.productId)}/archive`);
                  showToast(`Archived ${p.name}`, "success");
                  await loadRows();
                }
              });
            });
          }
          const restBtn = row.querySelector(".act-restore");
          if (restBtn) {
            restBtn.addEventListener("click", async () => {
              await api.post(`/api/products/${encodeURIComponent(p.productId)}/restore`);
              showToast(`Restored ${p.name}`, "success");
              await loadRows();
            });
          }
          row.querySelector(".act-delete").addEventListener("click", () => confirmDeleteProduct(p, loadRows));
        });
      }

      const startIdx = res.totalCount === 0 ? 0 : (res.page - 1) * res.pageSize + 1;
      const endIdx = Math.min(res.page * res.pageSize, res.totalCount);
      pagBar.innerHTML = `
        <div>Showing ${startIdx}–${endIdx} of ${res.totalCount} product(s)</div>
        <div style="display:flex;gap:6px;align-items:center;">
          <button type="button" class="btn btn-secondary btn-sm" id="pg-prev" ${res.page <= 1 ? "disabled" : ""}>Previous</button>
          <span class="num">Page ${res.page} of ${res.totalPages}</span>
          <button type="button" class="btn btn-secondary btn-sm" id="pg-next" ${res.page >= res.totalPages ? "disabled" : ""}>Next</button>
        </div>
      `;
      pagBar.querySelector("#pg-prev").addEventListener("click", () => {
        if (state.page > 1) { state.page--; loadRows(); }
      });
      pagBar.querySelector("#pg-next").addEventListener("click", () => {
        if (state.page < res.totalPages) { state.page++; loadRows(); }
      });
    } catch (err) {
      tbody.innerHTML = `<tr><td colspan="9"><div class="notice notice-error">${escapeHtml(err.message)}</div></td></tr>`;
    }
  };

  let debounceTimer = null;
  const applyFilters = (immediate = false) => {
    state.q = container.querySelector("#flt-q").value.trim();
    state.category = container.querySelector("#flt-cat").value;
    state.status = container.querySelector("#flt-status").value;
    state.minPrice = container.querySelector("#flt-min-price").value;
    state.maxPrice = container.querySelector("#flt-max-price").value;
    const [sb, asc] = container.querySelector("#flt-sort").value.split(":");
    state.sortBy = sb;
    state.asc = asc;
    state.belowCost = container.querySelector("#flt-below-cost").checked;
    state.lowStock = container.querySelector("#flt-low-stock").checked;
    state.page = 1;

    clearTimeout(debounceTimer);
    if (immediate) {
      loadRows();
    } else {
      debounceTimer = setTimeout(loadRows, 220);
    }
  };

  container.querySelector("#flt-q").addEventListener("input", () => applyFilters(false));
  container.querySelector("#flt-min-price").addEventListener("input", () => applyFilters(false));
  container.querySelector("#flt-max-price").addEventListener("input", () => applyFilters(false));
  ["#flt-cat", "#flt-status", "#flt-sort", "#flt-below-cost", "#flt-low-stock"].forEach(sel => {
    container.querySelector(sel).addEventListener("change", () => applyFilters(true));
  });

  container.querySelector("#flt-reset").addEventListener("click", () => {
    container.querySelector("#flt-q").value = "";
    container.querySelector("#flt-cat").value = "";
    container.querySelector("#flt-status").value = "ACTIVE";
    container.querySelector("#flt-min-price").value = "";
    container.querySelector("#flt-max-price").value = "";
    container.querySelector("#flt-sort").value = "updated:false";
    container.querySelector("#flt-below-cost").checked = false;
    container.querySelector("#flt-low-stock").checked = false;
    applyFilters(true);
  });

  container.querySelector("#prd-add-new").addEventListener("click", () => openProductFormModal({ onSaved: loadRows }));
  container.querySelector("#prd-export-csv").addEventListener("click", () => api.downloadUrl("/api/reports/export/catalogue"));

  await loadRows();
}
