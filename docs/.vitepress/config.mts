import { defineConfig } from "vitepress";
import { withMermaid } from "vitepress-plugin-mermaid";

// https://vitepress.dev/reference/site-config
export default withMermaid(defineConfig({
  title: "EDDIE Framework",
  description: "Using and Extending the EDDIE Framework",
  ignoreDeadLinks: "localhostLinks",
  head: [["link", { rel: "icon", href: "/images/favicon-32x32.png" }]],
  vite: {
    // workaround for a vite/pnpm related mermaid bug: https://github.com/mermaid-js/mermaid/issues/4320
    // bug occurs in vitepress dev mode only
    optimizeDeps: { include: ["mermaid"] },
  },
  base: "/framework/",
  themeConfig: {
    // https://vitepress.dev/reference/default-theme-config
    logo: "/images/favicon-32x32.png",
    nav: [
      { text: "Home", link: "/" },
    ],

    sidebar: [
      {
        text: "Running",
        items: [
          { text: "Operation", link: "1-running/OPERATION" },
          {
            text: "Region Connectors",
            items: [
              { text: "AIIDA", link: "1-running/region-connectors/region-connector-aiida.md" },
              { text: "EDA (Austria)", link: "1-running/region-connectors/region-connector-at-eda.md" },
              { text: "Fluvius (Belgium)", link: "1-running/region-connectors/region-connector-be-fluvius.md" },
              { text: "Energinet (Denmark)", link: "1-running/region-connectors/region-connector-dk-energinet.md" },
              { text: "Datadis (Spain)", link: "1-running/region-connectors/region-connector-es-datadis.md" },
              { text: "Fingrid (Finland)", link: "1-running/region-connectors/region-connector-fi-fingrid.md" },
              { text: "Enedis (France)", link: "1-running/region-connectors/region-connector-fr-enedis.md" },
              {
                text: "Mijn Aansluiting (Netherlands)",
                link: "1-running/region-connectors/region-connector-nl-mijn-aansluiting.md",
              },
              { text: "Green Button (USA)", link: "1-running/region-connectors/region-connector-us-green-button.md" },
            ],
          },
          {
            text: "Outbound-connectors",
            link: "1-running/outbound-connectors/outbound-connectors.md",
            items: [
              { text: "AMQP", link: "1-running/outbound-connectors/outbound-connector-amqp.md" },
            ],
          },
          { text: "Admin Console", link: "1-running/admin-console" },
          { text: "Example App", link: "1-running/example-app" },
        ],
      },
      {
        text: "Integrating",
        items: [
          { text: "Permission States", link: "2-integrating//PERMISSION_STATES" },
          {
            text: "Kafka Topics", link: "2-integrating/KAFKA",
            items: [
              {
                text: "accounting point market documents",
                link: "2-integrating/topics/ACCOUNTING_POINT_MARKET_DOCUMENTS",
              },
              { text: "permission market documents", link: "2-integrating/topics/PERMISSION_MARKET_DOCUMENTS" },
              { text: "raw data in proprietary format", link: "2-integrating/topics/RAW_DATA_IN_PROPRIETARY_FORMAT" },
              { text: "status messages", link: "2-integrating/topics/STATUS_MESSAGES" },
              { text: "validated historical data", link: "2-integrating/topics/VALIDATED_HISTORICAL_DATA" },
            ],
          },
        ],
      },
      {
        text: "Extending",
        items: [
          { text: "Tech Stack", link: "3-extending/tech-stack" },
          { text: "Add a region connector", link: "3-extending/add-region-connector" },
          {
            text: "Add an outbound-connector",
            link: "3-extending/add-outbound-connector",
          },
          { text: "Edit Documentation", link: "3-extending/documentation" },
        ],
      },
    ],

    socialLinks: [
      { icon: "github", link: "https://github.com/eddie-energy/eddie" },
    ],
  },
}));
