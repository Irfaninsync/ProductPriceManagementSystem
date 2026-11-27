import { api } from "../api.js";
import { closeModal, confirmDialog, openModal } from "../components/modal.js";
import { showToast } from "../components/toast.js";
import { escapeHtml, formatCurrency, formatMarginPercent } from "../format.js";

export async function renderCategories(container) {
  let searchQuery = "";

  container.innerHTML = `
    <div class="stack-lg">
      <div class="panel">
        <div class="panel-header">
          <div>
            <h2 class="panel-title">Category Management</h2>
            <p class="panel-subtitle">Organize products, track category valuations, and maintain clean naming consistency</p>
          </div>
          <div style="display:flex;gap:8px;flex-wrap:wrap;">
            <button type="button" class="btn btn-secondary btn-sm" id="cat-export-csv">Export CSV</button>
            <button type="button" class="btn btn-primary btn-sm" id="cat-add-btn">+ New Category</button>
          </div>
        </div>

        <div class="filter-bar">
          <input type="search" class="input" id="cat-search" placeholder="Search category name or description..." style="min-width:240px;flex:1;">
        </div>

        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Category</th>
                <th class="right">Active / Total</th>
                <th class="right">Stock Units</th>
                <th class="right">Avg Price</th>
                <th class="right">Total Value</th>
                <th class="right">Margin %</th>
                <th class="right">Actions</th>
              </tr>
            </thead>
            <tbody id="cat-tbody">
              <tr><td colspan="8" style="text-align:center;padding:20px;">Loading categories...</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `;

  const tbody = container.querySelector("#cat-tbody");

  const loadCategories = async () => {
    const res = await api.get(`/api/categories?q=${encodeURIComponent(searchQuery)}`);
    const items = res.items || [];

    if (items.length === 0) {
      tbody.innerHTML = `
        <tr><td colspan="8">
          <div class="empty-state" style="padding:28px;">
            <div class="empty-state-title">No categories found</div>
            <div class="empty-state-copy">Create a category to classify your product inventory.</div>
          </div>
        </td></tr>
      `;
      return;
    }

    tbody.innerHTML = items.map(c => `
      <tr data-cid="${escapeHtml(c.categoryId)}">
        <td class="mono" style="font-size:12px;">${escapeHtml(c.categoryId)}</td>
        <td>
          <div style="font-weight:600;">${escapeHtml(c.name)}</div>
          ${c.description ? `<div style="font-size:12px;color:var(--text-secondary);">${escapeHtml(c.description)}</div>` : ""}
        </td>
        <td class="right num">${c.activeProducts} / ${c.totalProducts}</td>
        <td class="right num">${c.totalUnits.toLocaleString()}</td>
        <td class="right num">${escapeHtml(formatCurrency(c.avgSellingPrice))}</td>
        <td class="right num" style="font-weight:600;">${escapeHtml(formatCurrency(c.totalSellingValue))}</td>
        <td class="right num">${escapeHtml(formatMarginPercent(c.marginPercentage))}</td>
        <td class="right">
          <div class="row-actions">
            <a href="#/products?category=${encodeURIComponent(c.name)}" class="btn btn-secondary btn-sm">Products</a>
            <button type="button" class="btn btn-ghost btn-sm cat-edit">Edit</button>
            <button type="button" class="btn btn-danger btn-sm cat-del">Delete</button>
          </div>
        </td>
      </tr>
    `).join("");

    items.forEach(c => {
      const row = tbody.querySelector(`tr[data-cid="${CSS.escape(c.categoryId)}"]`);
      if (!row) return;
      row.querySelector(".cat-edit").addEventListener("click", () => openCategoryFormModal(c, loadCategories));
      row.querySelector(".cat-del").addEventListener("click", () => openDeleteCategoryModal(c, items, loadCategories));
    });
  };

  function openCategoryFormModal(existing, onSaved) {
    const isEdit = Boolean(existing);
    openModal({
      title: isEdit ? `Edit Category — ${existing.name}` : "Create New Category",
      bodyHtml: `
        <div id="cat-modal-err" class="notice notice-error hidden"></div>
        <form id="cat-form" class="stack-md">
          <div class="form-field">
            <label class="form-label" for="cf-name">Category Name *</label>
            <input class="input" id="cf-name" required minlength="2" maxlength="60"
                   value="${escapeHtml(existing ? existing.name : "")}" placeholder="e.g. Peripherals">
          </div>
          <div class="form-field">
            <label class="form-label" for="cf-desc">Description</label>
            <textarea class="textarea" id="cf-desc" maxlength="300"
                      placeholder="Optional description of products in this category">${escapeHtml(existing ? existing.description : "")}</textarea>
          </div>
          ${isEdit && existing.totalProducts > 0
            ? `<div class="notice notice-info">Renaming this category automatically updates all ${existing.totalProducts} assigned product(s).</div>`
            : ""}
        </form>
      `,
      footerHtml: `
        <button type="button" class="btn btn-secondary" id="cf-cancel">Cancel</button>
        <button type="submit" form="cat-form" class="btn btn-primary" id="cf-submit">${isEdit ? "Save Category" : "Create Category"}</button>
      `,
      onMount: root => {
        root.querySelector("#cf-cancel").addEventListener("click", closeModal);
        const errBox = root.querySelector("#cat-modal-err");
        root.querySelector("#cat-form").addEventListener("submit", async e => {
          e.preventDefault();
          errBox.classList.add("hidden");
          const btn = root.querySelector("#cf-submit");
          btn.disabled = true;
          try {
            const payload = {
              name: root.querySelector("#cf-name").value.trim(),
              description: root.querySelector("#cf-desc").value.trim()
            };
            if (isEdit) {
              await api.put(`/api/categories/${encodeURIComponent(existing.categoryId)}`, payload);
              showToast(`Updated category '${payload.name}'`, "success");
            } else {
              await api.post("/api/categories", payload);
              showToast(`Created category '${payload.name}'`, "success");
            }
            closeModal();
            await onSaved();
          } catch (err) {
            errBox.textContent = err.message;
            errBox.classList.remove("hidden");
            btn.disabled = false;
          }
        });
      }
    });
  }

  function openDeleteCategoryModal(category, allCategories, onDeleted) {
    if (category.totalProducts === 0) {
      confirmDialog({
        title: `Delete Category "${category.name}"?`,
        message: "This empty category will be permanently removed.",
        confirmLabel: "Delete Category",
        danger: true,
        onConfirm: async () => {
          await api.del(`/api/categories/${encodeURIComponent(category.categoryId)}`);
          showToast(`Deleted category '${category.name}'`, "success");
          await onDeleted();
        }
      });
      return;
    }

    const others = allCategories.filter(x => x.categoryId !== category.categoryId);
    if (others.length === 0) {
      openModal({
        title: `Cannot Delete "${category.name}"`,
        bodyHtml: `<div class="notice notice-warning">This category currently holds ${category.totalProducts} product(s) and no other category exists to reassign them to. Create another category first before deleting this one.</div>`,
        footerHtml: `<button type="button" class="btn btn-primary" id="cat-close-warn">Understood</button>`,
        onMount: root => root.querySelector("#cat-close-warn").addEventListener("click", closeModal)
      });
      return;
    }

    openModal({
      title: `Delete & Reassign "${category.name}"`,
      bodyHtml: `
        <div id="cat-del-err" class="notice notice-error hidden"></div>
        <div class="notice notice-warning">
          <strong>${category.totalProducts} product(s)</strong> currently belong to "${escapeHtml(category.name)}". Select a target category to reassign them before deletion.
        </div>
        <div class="form-field">
          <label class="form-label" for="cat-reassign-select">Reassign Products To *</label>
          <select class="select" id="cat-reassign-select">
            ${others.map(o => `<option value="${escapeHtml(o.categoryId)}">${escapeHtml(o.name)} (${o.totalProducts} items)</option>`).join("")}
          </select>
        </div>
      `,
      footerHtml: `
        <button type="button" class="btn btn-secondary" id="cd-cancel">Cancel</button>
        <button type="button" class="btn btn-danger-solid" id="cd-confirm">Reassign &amp; Delete</button>
      `,
      onMount: root => {
        root.querySelector("#cd-cancel").addEventListener("click", closeModal);
        const btn = root.querySelector("#cd-confirm");
        const errBox = root.querySelector("#cat-del-err");
        btn.addEventListener("click", async () => {
          btn.disabled = true;
          errBox.classList.add("hidden");
          const targetId = root.querySelector("#cat-reassign-select").value;
          try {
            await api.del(`/api/categories/${encodeURIComponent(category.categoryId)}?reassignTo=${encodeURIComponent(targetId)}`);
            closeModal();
            showToast(`Reassigned products and deleted '${category.name}'`, "success");
            await onDeleted();
          } catch (err) {
            errBox.textContent = err.message;
            errBox.classList.remove("hidden");
            btn.disabled = false;
          }
        });
      }
    });
  }

  let timer = null;
  container.querySelector("#cat-search").addEventListener("input", e => {
    searchQuery = e.target.value.trim();
    clearTimeout(timer);
    timer = setTimeout(loadCategories, 180);
  });
  container.querySelector("#cat-add-btn").addEventListener("click", () => openCategoryFormModal(null, loadCategories));
  container.querySelector("#cat-export-csv").addEventListener("click", () => api.downloadUrl("/api/reports/export/categories"));

  await loadCategories();
}
