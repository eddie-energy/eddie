import { html, LitElement } from "https://esm.sh/lit";
import { createRef, ref } from "https://esm.sh/lit/directives/ref.js";

function range(n) {
  return [...Array(n).keys()];
}

const MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000;

class ProduceSimulatedMeterReadingCe extends LitElement {
  static properties = {
    connectionId: { attribute: "connection-id" },
    dataNeedId: { attribute: "data-need-id" },
    _measurementsPerDay: {},
    _date: {},
  };
  static meteringIntervalOptions = {
    PT1D: 1,
    P1D: 1,
    PT1H: 24,
    PT30M: 48,
    PT15M: 96,
  };

  constructor() {
    super();
    this._measurementsPerDay =
      ProduceSimulatedMeterReadingCe.meteringIntervalOptions.PT1H;
    this._date = new Date().toISOString().split("T")[0];
    this.meteringPointInputRef = createRef();
    this.permissionIdInputRef = createRef();
    this.measurementsPerDayRef = createRef();
    this.measurementsDivRef = createRef();
  }

  timeInterval(i) {
    const start = new Date(
      (i * MILLISECONDS_PER_DAY) / this._measurementsPerDay
    );
    const end = new Date(
      ((i + 1) * MILLISECONDS_PER_DAY) / this._measurementsPerDay
    );
    const timeStr = (date) =>
      date.getUTCHours().toString().padStart(2, "0") +
      ":" +
      date.getUTCMinutes().toString().padStart(2, "0");
    return timeStr(start) + " - " + timeStr(end);
  }

  changeMeteringInterval(event) {
    this._measurementsPerDay =
      ProduceSimulatedMeterReadingCe.meteringIntervalOptions[
        event.target.value
      ];
  }

  changeStartDate(event) {
    this._date = event.target.value;
  }

  randomize() {
    function nums(count) {
      const result = [];
      const d = 40;
      const max = 80;
      let last = max / 2;
      for (let i = 0; i < count; ++i) {
        result.push(Math.floor(Math.random() * d) - d / 2 + last);
      }
      return result;
    }

    const values = nums(this._measurementsPerDay);

    [
      ...this.measurementsDivRef.value
        .querySelectorAll("input[type='number']")
        .values(),
    ].forEach((node, index) => (node.value = values[index]));
  }

  submit() {
    const isDataPointMeasured = [
      ...this.measurementsDivRef.value
        .querySelectorAll("input[type='radio']")
        .values(),
    ]
      .filter((node) => node.id.startsWith("measured"))
      .map((node) => node.checked);
    const measurements = [
      ...this.measurementsDivRef.value
        .querySelectorAll("input[type='number']")
        .values(),
    ].map((node, i) => ({
      value: node.value,
      measurementType: isDataPointMeasured[i] ? "measured" : "extrapolated",
    }));
    const meterReading = {
      connectionId: this.connectionId,
      dataNeedId: this.dataNeedId,
      permissionId: this.permissionIdInputRef?.value?.value,
      meteringPoint: this.meteringPointInputRef?.value?.value,
      startDateTime: new Date(this._date + "T00:00:00").toISOString(),
      meteringInterval: this.measurementsPerDayRef?.value?.value,
      measurements: measurements,
    };
    fetch("simulated-meter-reading", {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify(meterReading),
    })
      .then((res) => {
        if (res.ok) {
          console.log(
            `sucessfully sent meter reading for connectionId:${meterReading.connectionId} meteringPoint:${meterReading.meteringPoint}`
          );
        } else {
          console.error(
            `unable to send meter reading for connectionId:${meterReading.connectionId} meteringPoint:${meterReading.meteringPoint}`,
            res
          );
        }
      })
      .catch(console.error);
  }

  render() {
    return html`
      <link
        rel="stylesheet"
        href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0-alpha3/dist/css/bootstrap.min.css"
      />
      <div class="card">
        <div class="card-header">Meter Readings</div>
        <div class="card-body">
          <div class="row mb-3">
            <label class="col-2" for="startDate">Date</label>
            <div class="col-2">
              <input
                class="form-control"
                type="date"
                id="startDate"
                value="${this._date}"
                @change="${this.changeStartDate}"
              />
            </div>
          </div>
          <div class="row mb-3">
            <label class="col-2" for="meteringPoint">Metering Point</label>
            <div class="col-2">
              <input
                class="form-control"
                type="text"
                id="meteringPoint"
                ${ref(this.meteringPointInputRef)}
                value="MP-4711"
              />
            </div>
          </div>
          <div class="row mb-3">
            <label class="col-2" for="permissionId">Permission Id</label>
            <div class="col-2">
              <input
                class="form-control"
                type="text"
                id="permissionId"
                ${ref(this.permissionIdInputRef)}
                value="MP-4711"
              />
            </div>
          </div>
          <div class="row mb-3">
            <label class="col-2" for="meteringInterval"
              >Measurement per day</label
            >
            <div class="col-2">
              <select
                class="form-select"
                id="meteringInterval"
                name="meteringInterval"
                @change="${this.changeMeteringInterval}"
                ${ref(this.measurementsPerDayRef)}
              >
                ${Object.entries(
                  ProduceSimulatedMeterReadingCe.meteringIntervalOptions
                )
                  .sort((a, b) => (a[1] > b[1] ? 1 : -1))
                  .map(
                    (o) =>
                      html` <option
                        value="${o[0]}"
                        ?selected="${o[1] === this._measurementsPerDay}"
                      >
                        ${o[1]} (${o[0]})
                      </option>`
                  )}
              </select>
            </div>
          </div>
          <div ${ref(this.measurementsDivRef)}>
            ${range(this._measurementsPerDay).map(
              (i) => html`
                <div class="row mb-1">
                  <div class="col-auto">${this.timeInterval(i)}</div>
                  <div class="col-auto">
                    <input
                      class="form-control form-control-sm text-end"
                      type="number"
                      value="0"
                    />
                  </div>
                  <div class="col-auto">
                    <span class="form-check d-inline-block">
                      <input
                        type="radio"
                        class="form-check-input"
                        name="type${i}"
                        id="measured${i}"
                        checked
                      />
                      <label class="form-check-label" for="measured${i}"
                        >measured</label
                      >
                    </span>
                    <span class="form-check d-inline-block">
                      <input
                        type="radio"
                        class="form-check-input"
                        name="type${i}"
                        id="interpolated${i}"
                      />
                      <label class="form-check-label" for="interpolated${i}"
                        >interpolated</label
                      >
                    </span>
                  </div>
                </div>
              `
            )}
          </div>
          <div class="row mt-3 mb-3">
            <button class="col-3 btn btn-secondary" @click="${this.randomize}">
              Randomize
            </button>
            <button
              class="col-3 offset-1 btn btn-primary"
              @click="${this.submit}"
            >
              Submit
            </button>
          </div>
        </div>
      </div>
    `;
  }
}

window.customElements.define(
  "produce-simulated-meter-reading",
  ProduceSimulatedMeterReadingCe
);
export default ProduceSimulatedMeterReadingCe;
