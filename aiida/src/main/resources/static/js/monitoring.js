const MONITORING_BASE_URL = document
  .querySelector('meta[name="monitoring-base-url"]')
  .getAttribute("content");

const gauges = new Map();
const charts = new Map();

function normalizeId(name) {
  return name.replace(/\s+/g, "-").toLowerCase();
}

function formatUptime(seconds) {
  const d = Math.floor(seconds / (3600 * 24));
  const h = Math.floor((seconds % (3600 * 24)) / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  return `${d}d ${h}h ${m}m`;
}

function createMetricCard(id, title, isHost = false, status = "", uptime = null) {
  const statusColor = status === "Running" ? "#4CAF50" : "#f44336";
  const statusHtml = !isHost
    ? `<div class="service-status" id="status-${id}" style="color: ${statusColor};"><strong>Status:</strong> ${status}</div>`
    : "";

  const uptimeHtml = uptime !== null
    ? `<span style="font-size: 0.9rem; color: #777;">Uptime: ${isHost ? formatUptime(uptime) : `${uptime.toFixed(1)}%`}</span>`
    : "";

  return `
    <sl-card class="metric-card" id="card-${id}" style="max-width: 50%; display: flex; flex-direction: column;">
      <div slot="header" style="display: flex; justify-content: space-between; align-items: center;">
        <div style="display: flex; gap: 0.5rem;">
          <strong>${isHost ? "Host:" : "Service:"}</strong>
          <span>${title}</span>
        </div>
        ${uptimeHtml}
      </div>
      ${statusHtml}

      <!-- CPU, Memory, Disk -->
      <div class="card-meters" style="display: flex; justify-content: center; gap: 2rem; margin-top: 0.5rem; flex-wrap: wrap;">
        <div class="wrapper" style="width: 120px; text-align: center;">
          <h4 style="margin-bottom: 0.5rem;">CPU</h4>
          <div id="cpu-gauge-${id}" class="gauge" style="width: 120px; height: 100px;"></div>
        </div>
        <div class="wrapper" style="width: 120px; text-align: center;">
          <h4 style="margin-bottom: 0.5rem;">Memory</h4>
          <div id="memory-gauge-${id}" class="gauge" style="width: 120px; height: 100px;"></div>
        </div>
        ${isHost ? `
        <div class="wrapper" style="width: 120px; text-align: center;">
          <h4 style="margin-bottom: 0.5rem;">Disk</h4>
          <div id="disk-gauge-${id}" class="gauge" style="width: 120px; height: 100px;"></div>
        </div>` : ''}
      </div>

      <!-- Network Chart -->
      <div class="wrapper" style="margin-top: 1rem; width: 100%;">
        <h4 style="margin-bottom: 0.5rem;">Network I/O</h4>
        <canvas id="network-chart-${id}" style="width: 100%; height: 100px;"></canvas>
      </div>
    </sl-card>
  `;
}

function updateGauge(id, type, value, options = {}) {
  const key = `${type}-${id}`;
  const max = options.max || 100;
  const title = options.title || `${type.toUpperCase()} Usage`;
  const suffix = options.suffix || "%";
  const formatted = options.formatted || null;

  if (!gauges.has(key)) {
    const gauge = new JustGage({
      id: `${type}-gauge-${id}`,
      value: Math.round(value),
      min: 0,
      max: max,
      title: title,
      label: suffix,
      gaugeWidthScale: 0.6,
      counter: true,
      customSectors: options.sectors || [],
    });
    gauges.set(key, gauge);
  } else {
    const gauge = gauges.get(key);
    gauge.refresh(Math.round(value));
  }

  if (formatted && document.getElementById(`${type}-gauge-${id}`)) {
    const label = document.querySelector(`#${type}-gauge-${id} .justgage .value`);
    if (label) label.textContent = formatted;
  }
}

function initNetworkChart(id) {
  const ctx = document.getElementById(`network-chart-${id}`).getContext("2d");
  ctx.canvas.width = 300;
  ctx.canvas.height = 120;
  const chart = new Chart(ctx, {
    type: 'line',
    data: {
      labels: [],
      datasets: [
        {
          label: 'Incoming (KBps)',
          data: [],
          borderColor: '#2196F3',
          backgroundColor: 'rgba(33, 150, 243, 0.1)',
          fill: true,
          tension: 0.3,
        },
        {
          label: 'Outgoing (KBps)',
          data: [],
          borderColor: '#4CAF50',
          backgroundColor: 'rgba(76, 175, 80, 0.1)',
          fill: true,
          tension: 0.3,
        }
      ]
    },
    options: {
      animation: false,
      responsive: true,
      scales: {
        x: {
          ticks: {
            maxTicksLimit: 5,
          }
        },
        y: {
          beginAtZero: true,
          title: {
            display: true,
            text: 'KBps'
          }
        }
      },
      plugins: {
        legend: {
          position: 'bottom'
        }
      }
    }
  });

  charts.set(id, chart);
}

function updateNetworkChart(id, inBps, outBps) {
  const chart = charts.get(id);
  if (!chart) return;

  const now = new Date().toLocaleTimeString();
  chart.data.labels.push(now);
  chart.data.datasets[0].data.push((inBps / 1000).toFixed(2));
  chart.data.datasets[1].data.push((outBps / 1000).toFixed(2));

  // Keep last 20 points
  if (chart.data.labels.length > 20) {
    chart.data.labels.shift();
    chart.data.datasets[0].data.shift();
    chart.data.datasets[1].data.shift();
  }

  chart.update();
}

function renderHostAndServiceMetrics() {
  const hostContainer = document.getElementById("host-metrics-container");
  const serviceContainer = document.getElementById("service-metrics-container");

  // Host Metrics
  fetch(`${MONITORING_BASE_URL}/host-metrics`)
    .then((response) => response.json())
    .then((host) => {
      const id = normalizeId(host.hostname || "Host");

      if (!document.getElementById(`card-${id}`)) {
        hostContainer.insertAdjacentHTML(
          "beforeend",
          createMetricCard(id, host.hostname || "Host", true, "", host.uptime)
        );
        initNetworkChart(id);
      }

      updateGauge(id, "cpu", host.cpuUsage);
      updateGauge(id, "memory", host.memoryUsage);
      updateGauge(id, "disk", host.diskUsage);
      updateNetworkChart(id, host.networkIncoming, host.networkOutgoing);
    })
    .catch((error) => console.error("Failed to fetch host metrics:", error));

  // Service Metrics
  fetch(`${MONITORING_BASE_URL}/service-metrics`)
    .then((response) => response.json())
    .then((services) => {
      services.forEach((service) => {
        const id = normalizeId(service.serviceName);

        if (!document.getElementById(`card-${id}`)) {
          serviceContainer.insertAdjacentHTML(
            "beforeend",
            createMetricCard(id, service.serviceName, false, service.status, service.uptime24h)
          );
          initNetworkChart(id);
        } else {
          const statusEl = document.getElementById(`status-${id}`);
          if (statusEl) {
            statusEl.innerHTML = `<strong>Status:</strong> ${service.status}`;
            statusEl.style.color = service.status === "Running" ? "#4CAF50" : "#f44336";
          }
        }

        updateGauge(id, "cpu", service.cpuUsage);
        updateGauge(id, "memory", service.memoryUsage);
        updateNetworkChart(id, service.networkIncoming, service.networkOutgoing);
      });
    })
    .catch((error) => console.error("Failed to fetch service metrics:", error));
}

setInterval(renderHostAndServiceMetrics, 5000);
renderHostAndServiceMetrics();
