import { css, html, LitElement } from "lit";

function countryCodeToEmoji(countryCode) {
  const baseCode = 127397;
  return String.fromCodePoint(
    ...countryCode
      .toUpperCase()
      .split("")
      .map((char) => {
        return baseCode + char.charCodeAt(0);
      })
  );
}

class ListItem extends LitElement {
  static styles = css`
    :host {
      display: block;
      padding: 1rem 0.75rem;
      cursor: pointer;
      border: solid var(--eddie-blue) 0.05rem;
      border-radius: 0.75rem;
      transition: background-color 0.3s ease;
    }

    :host(:hover) {
      background-color: #fff;
    }

    article {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      grid-template-rows: repeat(2, 1fr);
      align-items: center;
    }

    h3 {
      grid-column-start: 1;
      grid-column-end: span 3;
      margin: 0;
    }

    span {
      grid-column-start: 4;
      grid-column-end: 4;
    }

    p {
      grid-column-start: 1;
      grid-column-end: span 4;
      margin: 0;
    }
    
    span {
      text-align: end;
    }
  `;

  static properties = {
    regionConnector: { type: Object },
  };

  render() {
    if(!this.regionConnector) {
      return html`No Region Connector Available`;
    }
    return html`
      <article>
        <h3>${this.regionConnector.mdaDisplayName}</h3>
        <span>${countryCodeToEmoji(this.regionConnector.countryCode)}</span>
        <p>${this.regionConnector.urlPath}</p>
        <p>Covered Metering Points: ${this.regionConnector.coveredMeteringPoints}</p>
      </article>
    `;
  }
}

customElements.define("list-item", ListItem);

class RegionConnectorList extends LitElement {
  static styles = css`
    :host {
      display: grid;
      justify-content: center;
      align-items: center;
      font-family: Arial, sans-serif;
      padding: 20px;
    }

    .list {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
      gap: 10px;
    }
  `;

  static properties = {
    regionConnectors: { type: Array, state: true },
  };

  connectedCallback() {
    super.connectedCallback();
    this._fetchRegionConnectors().catch(console.log);
  }

  async _fetchRegionConnectors() {
    await fetch(new URL("http://localhost:8080/api/region-connectors-metadata"))
      .then((res) => res.json())
      .then((res) => (this.regionConnectors = res));
  }

  render() {
    if (!this.regionConnectors) {
      return html`
        <h2>Available Region Connectors</h2>
        <p>Loading...</p>
      `
    }
    return html`
      <h2>Available Region Connectors</h2>
      <div class="list">
        ${this.regionConnectors?.map(
          (rc) => html` <list-item .regionConnector=${rc}></list-item>`
        )}
      </div>
    `;
  }
}

customElements.define("region-connector-list", RegionConnectorList);
