let currentCurrencySymbol = "\u20B9";

export function setCurrencySymbol(symbol) {
  if (typeof symbol === "string" && symbol.trim().length > 0) {
    currentCurrencySymbol = symbol.trim();
  }
}

export function getCurrencySymbol() {
  return currentCurrencySymbol;
}

export function formatCurrency(amount) {
  if (amount === null || amount === undefined || Number.isNaN(Number(amount))) {
    return `${currentCurrencySymbol}0.00`;
  }
  const num = Number(amount);
  const sign = num < 0 ? "-" : "";
  const absFormatted = Math.abs(num).toLocaleString("en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
  const spacer = currentCurrencySymbol.endsWith(".") ? " " : "";
  return `${sign}${currentCurrencySymbol}${spacer}${absFormatted}`;
}

export function formatSignedCurrency(amount) {
  if (amount === null || amount === undefined || Number.isNaN(Number(amount))) {
    return `${currentCurrencySymbol}0.00`;
  }
  const num = Number(amount);
  const prefix = num > 0 ? "+" : "";
  return `${prefix}${formatCurrency(num)}`;
}

export function formatPercent(val) {
  if (val === null || val === undefined || Number.isNaN(Number(val))) {
    return "N/A";
  }
  const num = Number(val);
  const sign = num > 0 ? "+" : "";
  return `${sign}${num.toFixed(2)}%`;
}

export function formatMarginPercent(val) {
  if (val === null || val === undefined || Number.isNaN(Number(val))) {
    return "N/A";
  }
  return `${Number(val).toFixed(2)}%`;
}

export function formatDateTime(iso) {
  if (!iso) return "-";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return escapeHtml(iso);
  return d.toLocaleString("en-GB", {
    year: "numeric",
    month: "short",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  });
}

export function formatDateOnly(iso) {
  if (!iso) return "-";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return escapeHtml(iso);
  return d.toLocaleDateString("en-GB", {
    year: "numeric",
    month: "short",
    day: "2-digit"
  });
}

export function formatBytes(bytes) {
  const b = Number(bytes) || 0;
  if (b < 1024) return `${b} B`;
  if (b < 1024 * 1024) return `${(b / 1024).toFixed(1)} KB`;
  return `${(b / (1024 * 1024)).toFixed(2)} MB`;
}

export function escapeHtml(str) {
  if (str === null || str === undefined) return "";
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}
