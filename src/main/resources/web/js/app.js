import { api } from "./api.js";
import { confirmDialog } from "./components/modal.js";
import { openProductFormModal } from "./components/productModals.js";
import { showToast } from "./components/toast.js";
import { escapeHtml, formatCurrency, setCurrencySymbol } from "./format.js";
import { hydrateIcons } from "./icons.js";
import { renderCategories } from "./views/categories.js";
import { renderDashboard } from "./views/dashboard.js";
import { renderPricing } from "./views/pricing.js";
import { renderProductDetail } from "./views/productDetail.js";
import { renderProducts } from "./views/products.js";
import { renderReports } from "./views/reports.js";
import { renderSettings } from "./views/settings.js";

const ROUTE_META = {
  dashboard: { title: "Dashboard", subtitle: "Real-time overview of catalog health and pricing updates" },
  products: { title: "Products", subtitle: "Catalogue inventory, cost pricing, search, and filtering" },
  productDetail: { title: "Product Details", subtitle: "Unit economics, stock valuation, and price history" },
  pricing: { title: "Price Management", subtitle: "Pre-commit price comparisons, bulk revaluation, and audit trail" },
  categories: { title: "Categories", subtitle: "Structured product groupings and category valuations" },
  reports: { title: "Reports & Analytics", subtitle: "Operational statements, CSV exports, and print-ready PDF views" },
  settings: { title: "Settings & Data Management", subtitle: "Local database status, snapshot backups, and CSV batch import" }
};

const viewRoot = document.getElementById("app-view");
const sidebar = document.getElementById("sidebar");
const sidebarBackdrop = document.getElementById("sidebar-backdrop");

function parseHash() {
  const raw = window.location.hash || "#/dashboard";
  const withoutHash = raw.startsWith("#") ? raw.slice(1) : raw;
  const [pathPart, queryPart] = withoutHash.split("?");
  const queryParams = {};
  if (queryPart) {
    new URLSearchParams(queryPart).forEach((v, k) => { queryParams[k] = v; });
  }
  const cleanPath = pathPart.startsWith("/") ? pathPart : `/${pathPart}`;
  if (cleanPath.startsWith("/products/") && cleanPath.length > "/products/".length) {
    return {
      route: "productDetail",
      productId: decodeURIComponent(cleanPath.slice("/products/".length)),
      queryParams
    };
  }
  const key = cleanPath.replace(/^\/+/, "") || "dashboard";
  return { route: ROUTE_META[key] ? key : "dashboard", queryParams };
}

async function refreshShellMetadata() {
  try {
    const settings = await api.get("/api/settings");
    setCurrencySymbol(settings.currencySymbol);
    const bizEl = document.getElementById("sidebar-business-name");
    const printBizEl = document.getElementById("print-business-name");
    if (bizEl) bizEl.textContent = settings.businessName || "Local Business Ledger";
    if (printBizEl) printBizEl.textContent = `${settings.businessName || "PPMS"} — Product Price Management System`;

    const pill = document.getElementById("db-status-pill");
    if (pill) {
      const dot = pill.querySelector(".status-dot");
      if (dot) dot.classList.toggle("error", !settings.databaseHealthy);
    }
  } catch (_) {}
}

async function navigate() {
  closeMobileSidebar();
  const parsed = parseHash();
  const meta = ROUTE_META[parsed.route] || ROUTE_META.dashboard;

  document.getElementById("topbar-title").textContent = meta.title;
  document.getElementById("topbar-subtitle").textContent = meta.subtitle;

  const activeNavKey = parsed.route === "productDetail" ? "products" : parsed.route;
  document.querySelectorAll(".nav-item").forEach(el => {
    el.classList.toggle("active", el.dataset.route === activeNavKey);
  });

  try {
    switch (parsed.route) {
      case "dashboard":
        await renderDashboard(viewRoot, { onNavigate: navigate, onRefreshShell: refreshShellMetadata });
        break;
      case "products":
        await renderProducts(viewRoot, { queryParams: parsed.queryParams });
        break;
      case "productDetail":
        await renderProductDetail(viewRoot, { productId: parsed.productId });
        break;
      case "pricing":
        await renderPricing(viewRoot);
        break;
      case "categories":
        await renderCategories(viewRoot);
        break;
      case "reports":
        await renderReports(viewRoot);
        break;
      case "settings":
        await renderSettings(viewRoot, { onRefreshShell: refreshShellMetadata });
        break;
    }
  } catch (err) {
    viewRoot.innerHTML = `<div class="panel"><div class="panel-body"><div class="notice notice-error">${escapeHtml(err.message)}</div></div></div>`;
  }
}

function closeMobileSidebar() {
  sidebar.classList.remove("open");
  sidebarBackdrop.classList.remove("open");
}

function setupGlobalEvents() {
  document.getElementById("btn-mobile-menu").addEventListener("click", () => {
    sidebar.classList.toggle("open");
    sidebarBackdrop.classList.toggle("open");
  });
  sidebarBackdrop.addEventListener("click", closeMobileSidebar);

  document.getElementById("btn-topbar-add-product").addEventListener("click", () => {
    openProductFormModal({
      onSaved: async () => {
        await refreshShellMetadata();
        await navigate();
      }
    });
  });

  document.getElementById("btn-quick-backup").addEventListener("click", async () => {
    try {
      const snap = await api.post("/api/backups", { note: "Quick backup from sidebar" });
      showToast(`Backup saved: ${snap.fileName}`, "success");
    } catch (err) {
      showToast(err.message, "error");
    }
  });

  document.getElementById("btn-quit-app").addEventListener("click", () => {
    confirmDialog({
      title: "Exit PPMS Application?",
      message: "Your SQLite database is saved locally on this computer. Exiting will safely close the local application server.",
      confirmLabel: "Shut Down Safely",
      onConfirm: async () => {
        await api.post("/api/system/shutdown", {});
        document.body.innerHTML = `
          <div style="min-height:100vh;display:flex;align-items:center;justify-content:center;padding:24px;background:#F7F6F2;color:#252522;font-family:Inter,Segoe UI,sans-serif;">
            <div style="max-width:440px;background:#FFFFFF;border:1px solid #E5E3DC;border-radius:8px;padding:28px;text-align:center;">
              <h2 style="margin:0 0 8px;font-size:18px;">PPMS has closed safely</h2>
              <p style="margin:0;color:#77766F;font-size:14px;line-height:1.5;">
                All product and pricing records have been saved to your local database. You may close this browser tab.
              </p>
            </div>
          </div>
        `;
      }
    });
  });

  // Global Quick Search (Ctrl+K)
  const searchInput = document.getElementById("global-quick-search");
  const searchDrop = document.getElementById("quick-search-results");
  let qsTimer = null;

  document.addEventListener("keydown", e => {
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") {
      e.preventDefault();
      searchInput.focus();
      searchInput.select();
    }
  });

  searchInput.addEventListener("input", () => {
    const q = searchInput.value.trim();
    clearTimeout(qsTimer);
    if (!q) {
      searchDrop.classList.add("hidden");
      searchDrop.innerHTML = "";
      return;
    }
    qsTimer = setTimeout(async () => {
      try {
        const [prodRes, catRes] = await Promise.all([
          api.get(`/api/products?status=ALL&q=${encodeURIComponent(q)}&pageSize=6`),
          api.get(`/api/categories?q=${encodeURIComponent(q)}`)
        ]);
        const prods = prodRes.items || [];
        const cats = (catRes.items || []).slice(0, 3);
        if (prods.length === 0 && cats.length === 0) {
          searchDrop.innerHTML = `<div class="quick-result-item" style="color:var(--text-secondary);cursor:default;">No matches for "${escapeHtml(q)}"</div>`;
        } else {
          const prodHtml = prods.map(p => `
            <a href="#/products/${encodeURIComponent(p.productId)}" class="quick-result-item">
              <div>
                <div style="font-weight:500;">${escapeHtml(p.name)}</div>
                <div class="mono" style="font-size:11.5px;color:var(--text-secondary);">${escapeHtml(p.productId)} &bull; ${escapeHtml(p.category)}</div>
              </div>
              <div class="num" style="font-weight:600;">${escapeHtml(formatCurrency(p.sellingPrice))}</div>
            </a>
          `).join("");
          const catHtml = cats.map(c => `
            <a href="#/products?category=${encodeURIComponent(c.name)}" class="quick-result-item">
              <div>
                <div style="font-weight:500;">Category: ${escapeHtml(c.name)}</div>
                <div style="font-size:11.5px;color:var(--text-secondary);">${c.activeProducts} active item(s)</div>
              </div>
              <span class="badge badge-neutral">Filter</span>
            </a>
          `).join("");
          searchDrop.innerHTML = prodHtml + catHtml;
        }
        searchDrop.classList.remove("hidden");
      } catch (_) {}
    }, 180);
  });

  document.addEventListener("click", e => {
    if (!document.getElementById("quick-search-wrap").contains(e.target)) {
      searchDrop.classList.add("hidden");
    }
  });
  searchDrop.addEventListener("click", () => {
    searchDrop.classList.add("hidden");
    searchInput.value = "";
  });
}

async function init() {
  hydrateIcons(document);
  setupGlobalEvents();
  await refreshShellMetadata();
  window.addEventListener("hashchange", navigate);
  if (!window.location.hash) {
    window.location.hash = "#/dashboard";
  } else {
    await navigate();
  }
}

init();
