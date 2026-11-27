import { escapeHtml, formatCurrency, formatDateOnly } from "../format.js";

/**
 * Renders clean horizontal bar breakdown (e.g. Category Inventory Valuation).
 */
export function renderHorizontalBarList(items, { emptyText = "No data available." } = {}) {
  if (!items || items.length === 0) {
    return `<div class="empty-state" style="padding:24px;"><span class="empty-state-copy">${escapeHtml(emptyText)}</span></div>`;
  }
  const maxVal = Math.max(...items.map(i => Number(i.value) || 0), 1);
  return items.map(item => {
    const val = Number(item.value) || 0;
    const pct = Math.max(2, Math.round((val / maxVal) * 100));
    return `
      <div class="bar-row">
        <div style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis;" title="${escapeHtml(item.label)}">
          ${escapeHtml(item.label)}
        </div>
        <div class="bar-track">
          <div class="bar-fill" style="width:${pct}%;"></div>
        </div>
        <div class="num" style="text-align:right;font-weight:500;">${escapeHtml(item.formattedValue)}</div>
      </div>
    `;
  }).join("");
}

/**
 * Renders a crisp SVG price distribution chart across active catalog price bands.
 */
export function renderPriceDistributionSvg(bands) {
  if (!bands || bands.length === 0) {
    return `<div class="empty-state" style="padding:24px;"><span class="empty-state-copy">Add active products to visualize price distribution.</span></div>`;
  }

  const width = 520;
  const height = 200;
  const pad = { top: 28, right: 16, bottom: 44, left: 36 };
  const innerW = width - pad.left - pad.right;
  const innerH = height - pad.top - pad.bottom;

  const maxCount = Math.max(...bands.map(b => b.count || 0), 1);
  const slotW = innerW / bands.length;
  const barW = Math.min(54, slotW * 0.58);

  const gridLines = [0, 0.5, 1].map(ratio => {
    const y = pad.top + innerH - ratio * innerH;
    const tick = Math.round(maxCount * ratio);
    return `
      <line x1="${pad.left}" y1="${y}" x2="${width - pad.right}" y2="${y}" stroke="#E5E3DC" stroke-width="1"/>
      <text x="${pad.left - 8}" y="${y + 4}" text-anchor="end" fill="#77766F" font-size="10.5" font-family="IBM Plex Mono, monospace">${tick}</text>
    `;
  }).join("");

  const bars = bands.map((b, i) => {
    const count = b.count || 0;
    const barH = Math.max(count > 0 ? 4 : 0, (count / maxCount) * innerH);
    const x = pad.left + i * slotW + (slotW - barW) / 2;
    const y = pad.top + innerH - barH;
    const label = b.label
      ? formatCurrency(b.label)
      : `${formatCompact(b.minPrice)}–${formatCompact(b.maxPrice)}`;

    return `
      <g>
        <rect x="${x.toFixed(1)}" y="${y.toFixed(1)}" width="${barW.toFixed(1)}" height="${barH.toFixed(1)}" rx="3" fill="#713B43"/>
        <text x="${(x + barW / 2).toFixed(1)}" y="${Math.max(12, y - 5).toFixed(1)}" text-anchor="middle" fill="#252522" font-size="11" font-weight="600">${count}</text>
        <text x="${(x + barW / 2).toFixed(1)}" y="${height - 14}" text-anchor="middle" fill="#77766F" font-size="10">${escapeHtml(label)}</text>
      </g>
    `;
  }).join("");

  return `
    <svg viewBox="0 0 ${width} ${height}" class="svg-chart" role="img" aria-label="Product Price Distribution">
      ${gridLines}
      ${bars}
    </svg>
  `;
}

/**
 * Renders a chronological SVG line chart for a product's actual recorded price history.
 */
export function renderPriceHistoryChart(historyList, currentPrice) {
  if (!historyList || historyList.length === 0) {
    return `<div class="empty-state" style="padding:20px;"><span class="empty-state-copy">No historical price changes recorded yet. Current price: ${escapeHtml(formatCurrency(currentPrice))}.</span></div>`;
  }

  // Construct chronological series: initial oldPrice -> each subsequent newPrice
  const points = [
    { price: Number(historyList[0].oldPrice), date: historyList[0].changedAt, label: "Initial Recorded" }
  ];
  historyList.forEach(h => {
    points.push({ price: Number(h.newPrice), date: h.changedAt, label: h.reason });
  });

  const width = 620;
  const height = 200;
  const pad = { top: 20, right: 26, bottom: 36, left: 72 };
  const innerW = width - pad.left - pad.right;
  const innerH = height - pad.top - pad.bottom;

  const prices = points.map(p => p.price);
  const minP = Math.min(...prices);
  const maxP = Math.max(...prices);
  const span = maxP - minP || Math.max(1, maxP * 0.1);
  const low = Math.max(0, minP - span * 0.15);
  const high = maxP + span * 0.15;

  const coords = points.map((p, idx) => {
    const x = points.length === 1 ? pad.left + innerW / 2 : pad.left + (idx / (points.length - 1)) * innerW;
    const y = pad.top + innerH - ((p.price - low) / (high - low)) * innerH;
    return { ...p, x, y };
  });

  const pathD = coords.map((c, idx) => `${idx === 0 ? "M" : "L"}${c.x.toFixed(1)},${c.y.toFixed(1)}`).join(" ");

  const yTicks = [low, (low + high) / 2, high].map(val => {
    const y = pad.top + innerH - ((val - low) / (high - low)) * innerH;
    return `
      <line x1="${pad.left}" y1="${y.toFixed(1)}" x2="${width - pad.right}" y2="${y.toFixed(1)}" stroke="#E5E3DC" stroke-width="1"/>
      <text x="${pad.left - 8}" y="${(y + 4).toFixed(1)}" text-anchor="end" fill="#77766F" font-size="10.5" font-family="IBM Plex Mono, monospace">${escapeHtml(formatCompact(val))}</text>
    `;
  }).join("");

  const dots = coords.map(c => `
    <g>
      <circle cx="${c.x.toFixed(1)}" cy="${c.y.toFixed(1)}" r="4" fill="#FFFFFF" stroke="#713B43" stroke-width="2">
        <title>${escapeHtml(formatCurrency(c.price))} (${escapeHtml(formatDateOnly(c.date))} — ${escapeHtml(c.label)})</title>
      </circle>
    </g>
  `).join("");

  return `
    <svg viewBox="0 0 ${width} ${height}" class="svg-chart" role="img" aria-label="Historical Selling Price Trend">
      ${yTicks}
      <path d="${pathD}" fill="none" stroke="#713B43" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
      ${dots}
      <text x="${pad.left}" y="${height - 10}" fill="#77766F" font-size="10">${escapeHtml(formatDateOnly(points[0].date))}</text>
      <text x="${width - pad.right}" y="${height - 10}" text-anchor="end" fill="#77766F" font-size="10">${escapeHtml(formatDateOnly(points[points.length - 1].date))}</text>
    </svg>
  `;
}

function formatCompact(n) {
  const num = Number(n) || 0;
  if (num >= 100000) return `${(num / 1000).toFixed(0)}k`;
  if (num >= 1000) return `${(num / 1000).toFixed(1)}k`;
  return num.toFixed(0);
}
