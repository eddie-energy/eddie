import { css, html, LitElement } from "lit";

class ConnectionStatusMessage extends LitElement {
  static styles = css`
    :host {
      display: block;
      padding: 1rem 0.75rem;
      cursor: pointer;
      border: solid black 1px;
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
    
    :host(:first-child) {
      border: solid var(--eddie-blue) 1px;
    }

    h3 {
      color: var(--eddie-blue);
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
  `;

  static properties = {
    message: { type: Object },
  };

  render() {
    return html`
      <article>
        <p>ConnectionID: ${this.message.connectionId}</p>
        <p>PermissionID: ${this.message.permissionId}</p>
        <p>Timestamp: ${new Date(this.message.timestamp * 1000).toISOString()}</p>
        <p>Status: ${this.message.status}</p>
      </article>
    `;
  }
}

customElements.define("connection-status-message", ConnectionStatusMessage);

class ConnectionStatusMessages extends LitElement {
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
      gap: 8rem;
    }
  `;

  static properties = {
    messages: { type: Array, state: true },
  };

  constructor() {
    super();
    this.messages = []
  }

  connectedCallback() {
    super.connectedCallback();

    const statusMessageSocket = new WebSocket(
        "ws://localhost:8080/api/connection-status-messages"
    );
    statusMessageSocket.onopen = () => {
      console.log("Opened WebSocket Connection")
    }
    statusMessageSocket.onmessage = (event) => {
      this.messages = [...this.messages, JSON.parse(event.data)]
          .sort((m1, m2) => m2.timestamp - m1.timestamp)
          .slice(0, 10)
    };
    statusMessageSocket.onclose = () => {
      console.log("Websocket Connection closed")
    }
  }

  render() {
    return html`
      <h2>Connection Status Messages</h2>
      <div class="list">
        ${this.messages?.map(
          (msg) => html` <connection-status-message .message=${msg}></connection-status-message>`
        )}
      </div>
    `;
  }
}

customElements.define("connection-status-messages", ConnectionStatusMessages);
