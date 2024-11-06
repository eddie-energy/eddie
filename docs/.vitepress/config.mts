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
          { text: "Operation", link: "/OPERATION" },
          {
            text: "Region Connectors",
            items: [
              { text: "AIIDA", link: "region-connectors/region-connector-aiida.md" },
              { text: "AT: EDA", link: "region-connectors/region-connector-at-eda.md" },
              { text: "BE: Fluvius", link: "region-connectors/region-connector-be-fluvius.md" },
              { text: "DK: Energinet", link: "region-connectors/region-connector-dk-energinet.md" },
              { text: "ES: Datadis", link: "region-connectors/region-connector-es-datadis.md" },
              { text: "FI: Fingrid", link: "region-connectors/region-connector-fi-fingrid.md" },
              { text: "FR: Enedis", link: "region-connectors/region-connector-fr-enedis.md" },
              { text: "NL: Mijn Aansluiting", link: "region-connectors/region-connector-nl-mijn-aansluiting.md" },
              { text: "US: Green Button", link: "region-connectors/region-connector-us-green-button.md" },
            ],
          },
          { text: "Admin Console", link: "/admin-console" },
          { text: "Example App", link: "/example-app" },
        ],
      },
      {
        text: "Integrating",
        items: [
          { text: "Permission States", link: "/PERMISSION_STATES" },
          {
            text: "Kafka Topics", link: "/kafka/KAFKA",
            items: [
              { text: "accounting point market documents", link: "/kafka/topics/ACCOUNTING_POINT_MARKET_DOCUMENTS" },
              { text: "permission market documents", link: "/kafka/topics/PERMISSION_MARKET_DOCUMENTS" },
              { text: "raw data in proprietary format", link: "/kafka/topics/RAW_DATA_IN_PROPRIETARY_FORMAT" },
              { text: "status messages", link: "/kafka/topics/STATUS_MESSAGES" },
              { text: "validated historical data", link: "/kafka/topics/VALIDATED_HISTORICAL_DATA" },
            ],
          },
        ],
      },
      {
        text: "Extending",
        items: [
          { text: "Development", link: "/DEVELOPMENT" },
        ],
      },
    ],

    socialLinks: [
      { icon: "github", link: "https://github.com/eddie-energy/eddie" },
    ],
  },
}));
