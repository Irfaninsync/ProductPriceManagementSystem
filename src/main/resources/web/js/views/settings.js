import { api } from "../api.js";
import { confirmDialog } from "../components/modal.js";
import { showToast } from "../components/toast.js";
import { escapeHtml, formatBytes, formatDateTime, setCurrencySymbol } from "../format.js";

export async function renderSettings(container, { onRefreshShell }) {
  container.innerHTML = `<div class="panel"><div class="panel-body">Loading system settings and storage status...</div></div>`;

  const [settings, backupsData] = await Promise.all([
    api.get("/api/settings"),
    api.get("/api/backups")
  ]);

  let pendingImportCsv = "";

  const backupsList = backupsData.items || [];
  const backupRows = backupsList.length === 0
    ? `<tr><td colspan="4" style="text-align:center;padding:20px;color:var(--text-secondary);">No database backups created yet.</td></tr>`
    : backupsList.map(b => `
        <tr>
          <td class="mono" style="font-size:12px;">${escapeHtml(b.fileName)}</td>
          <td>${escapeHtml(b.note || "Snapshot")}</td>
          <td style="font-size:12px;color:var(--text-secondary);">${escapeHtml(formatDateTime(b.createdAt))}</td>
          <td class="right num">${escapeHtml(formatBytes(b.sizeBytes))}</td>
          <td class="right">
            <button type="button" class="btn btn-secondary btn-sm bkp-restore" data-file="${escapeHtml(b.fileName)}">Restore</button>
          </td>
        </tr>
      `).join("");

  container.innerHTML = `
    <div class="stack-lg">
      <div class="grid-2">
        <section class="panel">
          <div class="panel-header">
            <div>
              <h2 class="panel-title">Business Preferences</h2>
              <p class="panel-subtitle">Display currency, store name, and inventory alert thresholds</p>
            </div>
          </div>
          <form id="settings-form" class="panel-body stack-md">
            <div id="set-err" class="notice notice-error hidden"></div>
            <div class="form-field">
              <label class="form-label" for="st-biz">Business / Store Name</label>
              <input class="input" id="st-biz" required minlength="2" maxlength="80" value="${escapeHtml(settings.businessName)}">
            </div>
            <div class="form-grid">
              <div class="form-field">
                <label class="form-label" for="st-curr">Currency Symbol</label>
                <input class="input" id="st-curr" required maxlength="8" value="${escapeHtml(settings.currencySymbol)}" list="curr-presets">
                <datalist id="curr-presets">
                  <option value="₹">Indian Rupee (₹)</option>
                  <option value="Rs.">Rupees (Rs.)</option>
                  <option value="$">US Dollar ($)</option>
                  <option value="€">Euro (€)</option>
                  <option value="£">Pound (£)</option>
                </datalist>
              </div>
              <div class="form-field">
                <label class="form-label" for="st-thresh">Low-Stock Threshold (Units)</label>
                <input type="number" class="input num" id="st-thresh" min="1" max="10000" required value="${settings.lowStockThreshold}">
              </div>
            </div>
            <label class="checkbox-label">
              <input type="checkbox" id="st-auto-bkp" ${settings.autoBackupStartup ? "checked" : ""}>
              <span>Create an automatic daily backup on startup</span>
            </label>
            <div style="display:flex;justify-content:flex-end;">
              <button type="submit" class="btn btn-primary" id="st-save-btn">Save Preferences</button>
            </div>
          </form>
        </section>

        <section class="panel">
          <div class="panel-header">
            <div>
              <h2 class="panel-title">Application &amp; Database Status</h2>
              <p class="panel-subtitle">Local-first SQLite storage and runtime environment</p>
            </div>
            <span class="badge ${settings.databaseHealthy ? "badge-success" : "badge-error"}">
              ${settings.databaseHealthy ? "Database Connected" : "Database Issue"}
            </span>
          </div>
          <div class="panel-body">
            <div class="kv-row"><span class="kv-label">Application</span><span class="kv-val">PPMS v${escapeHtml(settings.appVersion)}</span></div>
            <div class="kv-row"><span class="kv-label">Java Runtime</span><span class="kv-val mono">${escapeHtml(settings.javaVersion)}</span></div>
            <div class="kv-row"><span class="kv-label">Schema Version</span><span class="kv-val mono">v${settings.schemaVersion} (SQLite WAL)</span></div>
            <div class="kv-row"><span class="kv-label">Database Size</span><span class="kv-val num">${escapeHtml(formatBytes(settings.databaseSizeBytes))}</span></div>
            <div class="kv-row"><span class="kv-label">Records Stored</span><span class="kv-val num">${settings.totalProducts} products &bull; ${settings.totalPriceHistory} price audits</span></div>
            <div class="kv-row"><span class="kv-label">Database Location</span><span class="kv-val mono" style="font-size:11.5px;text-align:right;word-break:break-all;">${escapeHtml(settings.databaseFile)}</span></div>
            <div class="kv-row"><span class="kv-label">Backups Folder</span><span class="kv-val mono" style="font-size:11.5px;text-align:right;word-break:break-all;">${escapeHtml(settings.backupsDir)}</span></div>
          </div>
        </section>
      </div>

      <section class="panel">
        <div class="panel-header">
          <div>
            <h2 class="panel-title">Database Backup &amp; Recovery</h2>
            <p class="panel-subtitle">Single-file SQLite snapshots verified with integrity checks prior to restoration</p>
          </div>
          <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap;">
            <input type="text" class="input" id="bkp-note" placeholder="Optional backup label..." style="width:220px;height:30px;">
            <button type="button" class="btn btn-primary btn-sm" id="bkp-create-btn">Create Backup Now</button>
          </div>
        </div>
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>Backup File</th>
                <th>Note</th>
                <th>Created</th>
                <th class="right">Size</th>
                <th class="right">Action</th>
              </tr>
            </thead>
            <tbody>${backupRows}</tbody>
          </table>
        </div>
      </section>

      <section class="panel">
        <div class="panel-header">
          <div>
            <h2 class="panel-title">CSV Data Import &amp; Migration</h2>
            <p class="panel-subtitle">Validate external CSV spreadsheets row-by-row before committing to SQLite</p>
          </div>
          <div style="display:flex;gap:8px;flex-wrap:wrap;">
            <button type="button" class="btn btn-secondary btn-sm" id="exp-all-products">Export Products CSV</button>
            <button type="button" class="btn btn-secondary btn-sm" id="exp-all-history">Export Price History CSV</button>
          </div>
        </div>
        <div class="panel-body stack-md">
          <div style="display:flex;gap:12px;align-items:center;flex-wrap:wrap;">
            <input type="file" id="csv-file-input" accept=".csv,text/csv" class="input" style="max-width:340px;padding:5px 8px;">
            <button type="button" class="btn btn-secondary" id="csv-preview-btn">Validate &amp; Preview CSV</button>
            <button type="button" class="btn btn-primary hidden" id="csv-commit-btn">Commit Valid Products</button>
            ${settings.hasLegacyCsv ? `<button type="button" class="btn btn-secondary" id="legacy-csv-btn">Import Existing data/products.csv</button>` : ""}
            ${settings.totalProducts === 0 ? `<button type="button" class="btn btn-ghost" id="settings-load-demo">Load Sample Catalog</button>` : ""}
          </div>
          <textarea class="textarea mono" id="csv-paste-input" placeholder="Or paste CSV content directly here (required columns: name, category, costPrice, sellingPrice, quantity)..."></textarea>
          <div id="csv-preview-result"></div>
        </div>
      </section>
    </div>
  `;

  container.querySelector("#settings-form").addEventListener("submit", async e => {
    e.preventDefault();
    const errBox = container.querySelector("#set-err");
    errBox.classList.add("hidden");
    const btn = container.querySelector("#st-save-btn");
    btn.disabled = true;
    try {
      const updated = await api.put("/api/settings", {
        businessName: container.querySelector("#st-biz").value.trim(),
        currencySymbol: container.querySelector("#st-curr").value.trim(),
        lowStockThreshold: Number(container.querySelector("#st-thresh").value),
        autoBackupStartup: container.querySelector("#st-auto-bkp").checked
      });
      setCurrencySymbol(updated.currencySymbol);
      showToast("Preferences saved", "success");
      if (onRefreshShell) await onRefreshShell();
      await renderSettings(container, { onRefreshShell });
    } catch (err) {
      errBox.textContent = err.message;
      errBox.classList.remove("hidden");
      btn.disabled = false;
    }
  });

  container.querySelector("#bkp-create-btn").addEventListener("click", async e => {
    const btn = e.currentTarget;
    btn.disabled = true;
    try {
      const note = container.querySelector("#bkp-note").value.trim() || "Manual backup";
      const snap = await api.post("/api/backups", { note });
      showToast(`Backup created: ${snap.fileName}`, "success");
      await renderSettings(container, { onRefreshShell });
    } catch (err) {
      showToast(err.message, "error");
      btn.disabled = false;
    }
  });

  container.querySelectorAll(".bkp-restore").forEach(btn => {
    btn.addEventListener("click", () => {
      const fileName = btn.dataset.file;
      confirmDialog({
        title: "Restore Database from Backup?",
        message: `Your current database will be safely backed up automatically first, then replaced with "${fileName}".`,
        confirmLabel: "Restore Backup",
        danger: true,
        onConfirm: async () => {
          await api.post("/api/backups/restore", { fileName });
          showToast("Database restored successfully", "success");
          if (onRefreshShell) await onRefreshShell();
          await renderSettings(container, { onRefreshShell });
        }
      });
    });
  });

  container.querySelector("#exp-all-products").addEventListener("click", () => api.downloadUrl("/api/reports/export/catalogue"));
  container.querySelector("#exp-all-history").addEventListener("click", () => api.downloadUrl("/api/reports/export/history"));

  const fileInput = container.querySelector("#csv-file-input");
  const pasteInput = container.querySelector("#csv-paste-input");
  const previewResult = container.querySelector("#csv-preview-result");
  const commitBtn = container.querySelector("#csv-commit-btn");

  fileInput.addEventListener("change", () => {
    const file = fileInput.files && fileInput.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = ev => {
      pasteInput.value = String(ev.target.result || "");
      commitBtn.classList.add("hidden");
    };
    reader.readAsText(file, "UTF-8");
  });

  container.querySelector("#csv-preview-btn").addEventListener("click", async () => {
    commitBtn.classList.add("hidden");
    const rawCsv = pasteInput.value;
    if (!rawCsv.trim()) {
      showToast("Select a CSV file or paste CSV content first.", "warning");
      return;
    }
    try {
      const res = await api.post("/api/import/preview", { csvContent: rawCsv });
      pendingImportCsv = rawCsv;
      const rejHtml = (res.rejections || []).length === 0 ? "" : `
        <div class="notice notice-warning" style="margin-top:10px;display:block;">
          <strong>Rejected Rows (${res.rejectedCount}):</strong>
          <ul style="margin:6px 0 0 18px;padding:0;font-size:12.5px;">
            ${res.rejections.slice(0, 15).map(r => `<li>Line ${r.lineNumber}: ${escapeHtml(r.reason)}</li>`).join("")}
          </ul>
        </div>
      `;
      previewResult.innerHTML = `
        <div class="notice notice-info">
          Processed <strong>${res.totalRows}</strong> row(s): <strong>${res.acceptedCount}</strong> valid, <strong>${res.rejectedCount}</strong> rejected.
        </div>
        ${rejHtml}
      `;
      if (res.acceptedCount > 0) {
        commitBtn.textContent = `Commit ${res.acceptedCount} Valid Product(s)`;
        commitBtn.classList.remove("hidden");
      }
    } catch (err) {
      previewResult.innerHTML = `<div class="notice notice-error">${escapeHtml(err.message)}</div>`;
    }
  });

  commitBtn.addEventListener("click", async () => {
    commitBtn.disabled = true;
    try {
      const res = await api.post("/api/import/commit", { csvContent: pendingImportCsv });
      showToast(`Imported ${res.importedCount} product(s) successfully.`, "success");
      await renderSettings(container, { onRefreshShell });
    } catch (err) {
      showToast(err.message, "error");
      commitBtn.disabled = false;
    }
  });

  const legacyBtn = container.querySelector("#legacy-csv-btn");
  if (legacyBtn) {
    legacyBtn.addEventListener("click", async () => {
      legacyBtn.disabled = true;
      try {
        const res = await api.post("/api/data/import-legacy-csv", {});
        showToast(`Migrated ${res.importedProducts} products and ${res.importedHistory} price history entries.`, "success");
        await renderSettings(container, { onRefreshShell });
      } catch (err) {
        showToast(err.message, "error");
        legacyBtn.disabled = false;
      }
    });
  }

  const demoBtn = container.querySelector("#settings-load-demo");
  if (demoBtn) {
    demoBtn.addEventListener("click", async () => {
      demoBtn.disabled = true;
      try {
        const res = await api.post("/api/data/load-demo", {});
        showToast(`Loaded ${res.loadedProducts} sample demonstration products.`, "success");
        await renderSettings(container, { onRefreshShell });
      } catch (err) {
        showToast(err.message, "error");
        demoBtn.disabled = false;
      }
    });
  }
}
