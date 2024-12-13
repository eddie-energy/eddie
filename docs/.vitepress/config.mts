import { defineConfig } from "vitepress";
import { withMermaid } from "vitepress-plugin-mermaid";
import { transformerTwoslash } from "@shikijs/vitepress-twoslash";

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
  markdown: {
    codeTransformers: [
      transformerTwoslash(),
    ],
  },
  themeConfig: {
    // https://vitepress.dev/reference/default-theme-config
    logo: "/images/favicon-32x32.png",
    nav: [
      { text: "Home", link: "/" },
    ],
    search: {
      provider: "local",
    },
    sidebar: [
      {
        text: "Running",
        items: [
          { text: "Operation", link: "1-running/OPERATION" },
          {
            text: "Region Connectors",
            link: "1-running/region-connectors/region-connectors.md",
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
              { text: "Apache Kafka", link: "1-running/outbound-connectors/outbound-connector-kafka.md" },
              { text: "AMQP", link: "1-running/outbound-connectors/outbound-connector-amqp.md" },
            ],
          },
          { text: "Admin Console", link: "1-running/admin-console" },
          { text: "Example App", link: "1-running/example-app" },
        ],
      },
      {
        text: "Integrating",
        link: "2-integrating/integrating.md",
        items: [
          {
            text: "Messages and Documents",
            link: "2-integrating/messages/messages.md",
            items: [
              {
                text: "Connection Status Messages",
                link: "2-integrating/messages/connection-status-messages.md",
              },
              {
                text: "Raw Data Messages",
                link: "2-integrating/messages/raw-data-messages.md",
              },
              {
                text: "Permission Market Documents",
                link: "2-integrating/messages/permission-market-documents.md",
              },
              {
                text: "Validated Historical Data Market Documents",
                link: "2-integrating/messages/validated-historical-data-market-documents.md",
              },
              {
                text: "Accounting Point Data Market Documents",
                link: "2-integrating/messages/accounting-point-data-market-documents.md",
              },
            ],
          },
          {
            text: "Data Needs",
            link: "2-integrating/data-needs.md",
          },
        ],
      },
      {
        text: "Extending",
        items: [
          { text: "Tech Stack", link: "3-extending/tech-stack" },
          {
            text: "Add a region connector",
            link: "3-extending/region-connector/add-region-connector",
            items: [
              {
                text: "Quickstart",
                link: "3-extending/region-connector/quickstart",
              },
              {
                text: "Build and Setup",
                link: "3-extending/region-connector/build-and-setup",
              },
              {
                text: "API",
                link: "3-extending/region-connector/api",
              },
              {
                text: "Internal Architecture",
                link: "3-extending/region-connector/internal-architecture",
              },
              {
                text: "Configuration",
                link: "3-extending/region-connector/configuration",
              },
              {
                text: "Frontend",
                link: "3-extending/region-connector/frontend",
              },
              {
                text: "Beans of Interest",
                link: "3-extending/region-connector/beans-of-interest",
              },
              {
                text: "Shared Functionality",
                link: "3-extending/region-connector/shared-functionality",
              },
              {
                text: "Dispatcher Servlet",
                link: "3-extending/region-connector/dispatcher-servlet",
              },
            ],
          },
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
