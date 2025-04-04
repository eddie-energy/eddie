const MONITORING_BASE_URL = document
  .querySelector('meta[name="monitoring-base-url"]')
  .getAttribute("content");

function rangeChange(value, valueRaw, clock, active, percents) {
  clock.style.transform = "rotate(" + (-90 + (value * 180) / 100) + "deg)";
  percents.innerText = `${Math.round(valueRaw * 1000) / 1000}%`;
  active.style.stroke = value <= 75 ? "#4CAF50" : "#f44336";
  active.style["stroke-dasharray"] = `${(value * 157) / 100} ${314 - (value * 157) / 100}`;
}

function createMetricCard(id, title, isHost = false, status = "") {
  const statusColor = status === "Running" ? "#4CAF50" : "#f44336";
  const statusHtml = !isHost
    ? `<div class="service-status" id="status-${id}" style="color: ${statusColor};"><strong>Status:</strong> ${status}</div>`
    : "";

  return `
    <sl-card class="metric-card" id="card-${id}">
      <div slot="header">
        <strong>${isHost ? "Host" : "Service"}:</strong> ${title}
      </div>
      ${statusHtml}
      <div class="card-meters">
        <div class="wrapper">
          <h4>CPU</h4>
          <svg class="meter">
            <circle class="meter-bg" r="50" cx="137" cy="145"></circle>
            <circle class="meter-active" id="cpu-meter-active-${id}" r="50" cx="137" cy="145"></circle>
            <polygon class="meter-clock" id="cpu-meter-clock-${id}" points="129,145 137,90 145,145"></polygon>
            <circle class="meter-circle" r="10" cx="137" cy="145"></circle>
          </svg>
          <span id="cpu-percents-${id}">0%</span>
        </div>
        <div class="wrapper">
          <h4>Memory</h4>
          <svg class="meter">
            <circle class="meter-bg" r="50" cx="137" cy="145"></circle>
            <circle class="meter-active" id="memory-meter-active-${id}" r="50" cx="137" cy="145"></circle>
            <polygon class="meter-clock" id="memory-meter-clock-${id}" points="129,145 137,90 145,145"></polygon>
            <circle class="meter-circle" r="10" cx="137" cy="145"></circle>
          </svg>
          <span id="memory-percents-${id}">0%</span>
        </div>
      </div>
    </sl-card>
  `;
}

function updateMeters(id, cpuRounded, cpuRaw, memRounded, memRaw) {
  const cpuClock = document.querySelector(`#cpu-meter-clock-${id}`);
  const cpuActive = document.querySelector(`#cpu-meter-active-${id}`);
  const cpuPercents = document.querySelector(`#cpu-percents-${id}`);

  const memClock = document.querySelector(`#memory-meter-clock-${id}`);
  const memActive = document.querySelector(`#memory-meter-active-${id}`);
  const memPercents = document.querySelector(`#memory-percents-${id}`);

  if (cpuClock && cpuActive && cpuPercents) {
    rangeChange(cpuRounded, cpuRaw, cpuClock, cpuActive, cpuPercents);
  }

  if (memClock && memActive && memPercents) {
    rangeChange(memRounded, memRaw, memClock, memActive, memPercents);
  }
}

function normalizeId(name) {
  return name.replace(/\s+/g, "-").toLowerCase();
}

function renderHostAndServiceMetrics() {
  const container = document.getElementById("metrics-container");

  // Host Metrics
  fetch(`${MONITORING_BASE_URL}/host-metrics`)
    .then((response) => response.json())
    .then((host) => {
      const id = normalizeId(host.hostname || "Host");
      if (!document.getElementById(`card-${id}`)) {
        container.insertAdjacentHTML("beforeend", createMetricCard(id, host.hostname || "Host", true));
      }
      updateMeters(
        id,
        Math.round(host.cpuUsage),
        host.cpuUsage,
        Math.round(host.memoryUsage),
        host.memoryUsage
      );
    })
    .catch((error) => console.error("Failed to fetch host metrics:", error));

  // Service Metrics
  fetch(`${MONITORING_BASE_URL}/service-metrics`)
    .then((response) => response.json())
    .then((services) => {
      console.log("Service Metrics:", services);
      services.forEach((service) => {
        const id = normalizeId(service.podName);
        if (!document.getElementById(`card-${id}`)) {
          container.insertAdjacentHTML(
            "beforeend",
            createMetricCard(id, service.podName, false, service.status)
          );
        } else {
          // Update status if already exists
          const statusEl = document.getElementById(`status-${id}`);
          if (statusEl) {
            statusEl.innerHTML = `<strong>Status:</strong> ${service.status}`;
            statusEl.style.color = service.status === "Running" ? "#4CAF50" : "#f44336";
          }
        }
        updateMeters(
          id,
          Math.round(service.cpuUsage),
          service.cpuUsage,
          Math.round(service.memoryUsage),
          service.memoryUsage
        );
      });
    })
    .catch((error) => console.error("Failed to fetch service metrics:", error));
}

setInterval(renderHostAndServiceMetrics, 5000);
renderHostAndServiceMetrics();
