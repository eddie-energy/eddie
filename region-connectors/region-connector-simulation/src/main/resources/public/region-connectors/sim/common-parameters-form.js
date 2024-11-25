import { html, LitElement } from "https://esm.sh/lit";
import { createRef, ref } from "https://esm.sh/lit/directives/ref.js";

class CommonParameters {
  _commonParameterForm = null;

  get dataNeedId() {
    return this._commonParameterForm.dataNeedIdInputRef.value.value;
  }

  get connectionId() {
    return this._commonParameterForm.connectionIdInputRef.value.value;
  }

  get permissionId() {
    return this._commonParameterForm.permissionIdInputRef.value.value;
  }
}

export const commonParameters = new CommonParameters();

class CommonParametersFormCe extends LitElement {
  static properties = {
    dataNeedId: { attribute: "data-need-id" },
    connectionId: { attribute: "connection-id" },
    permissionId: { attribute: false },
  };

  constructor() {
    super();
    this.dataNeedIdInputRef = createRef();
    this.connectionIdInputRef = createRef();
    this.permissionIdInputRef = createRef();
    if (commonParameters._commonParameterForm) {
      throw Error("common-parameters-form must be used only once");
    }
    commonParameters._commonParameterForm = this;
  }

  render() {
    return html`
      <link
        rel="stylesheet"
        href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0-alpha3/dist/css/bootstrap.min.css"
      />
      <div class="card">
        <div class="card-header">Common Parameters</div>
        <div class="card-body">
          <div class="row mb-3">
            <label class="col-2" for="dataNeedId">Data Need Id</label>
            <div class="col-2">
              <input
                class="form-control"
                type="text"
                id="dataNeedIdId"
                ${ref(this.dataNeedIdInputRef)}
                value="${this.dataNeedId}"
              />
            </div>
          </div>
          <div class="row mb-3">
            <label class="col-2" for="meteringPoint">Connection Id</label>
            <div class="col-2">
              <input
                class="form-control"
                type="text"
                id="connectionId"
                ${ref(this.connectionIdInputRef)}
                value="${this.connectionId}"
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
                value="pm::${this.dataNeedId}::${this.connectionId}"
              />
            </div>
          </div>
        </div>
      </div>
    `;
  }
}

window.customElements.define("common-parameters-form", CommonParametersFormCe);
export default CommonParametersFormCe;
