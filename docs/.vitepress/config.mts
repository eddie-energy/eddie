import { defineConfig } from "vitepress";
import { withMermaid } from "vitepress-plugin-mermaid";
import { transformerTwoslash } from "@shikijs/vitepress-twoslash";
import { propertiesPlugin } from "./propertiesPlugin.mjs";

// https://vitepress.dev/reference/site-config
export default withMermaid(
  defineConfig({
    title: "EDDIE Framework",
    description: "Using and Extending the EDDIE Framework",
    ignoreDeadLinks: "localhostLinks",
    head: [["link", { rel: "icon", href: "/framework/images/favicon.svg" }]],
    vite: {
      // workaround for a vite/pnpm related mermaid bug: https://github.com/mermaid-js/mermaid/issues/4320
      // bug occurs in vitepress dev mode only
      optimizeDeps: { include: ["mermaid"] },
    },
    base: "/framework/",
    markdown: {
      config: (md) => {
        md.use(propertiesPlugin);
      },
      codeTransformers: [transformerTwoslash()],
    },
    themeConfig: {
      // https://vitepress.dev/reference/default-theme-config
      logo: "/images/favicon.svg",
      nav: [
        { text: "Home", link: "/" },
        {
          text: "Architecture",
          link: "https://eddie-web.projekte.fh-hagenberg.at/architecture",
        },
      ],
      search: {
        provider: "local",
      },
      sidebar: [
        {
          text: "Running",
          items: [
            { text: "Operation", link: "/1-running/OPERATION" },
            {
              text: "EDDIE Button",
              link: "/1-running/eddie-button/eddie-button.md",
              items: [
                { text: "Angular", link: "/1-running/eddie-button/angular.md" },
              ],
            },
            {
              text: "Region Connectors",
              link: "/1-running/region-connectors/region-connectors.md",
              items: [
                {
                  text: "AIIDA",
                  link: "/1-running/region-connectors/region-connector-aiida.md",
                },
                {
                  text: "EDA (Austria)",
                  link: "/1-running/region-connectors/region-connector-at-eda.md",
                },
                {
                  text: "Fluvius (Belgium)",
                  link: "/1-running/region-connectors/region-connector-be-fluvius.md",
                },
                {
                  text: "Energinet (Denmark)",
                  link: "/1-running/region-connectors/region-connector-dk-energinet.md",
                },
                {
                  text: "Datadis (Spain)",
                  link: "/1-running/region-connectors/region-connector-es-datadis.md",
                },
                {
                  text: "Fingrid (Finland)",
                  link: "/1-running/region-connectors/region-connector-fi-fingrid.md",
                },
                {
                  text: "Enedis (France)",
                  link: "/1-running/region-connectors/region-connector-fr-enedis.md",
                },
                {
                  text: "Mijn Aansluiting (Netherlands)",
                  link: "/1-running/region-connectors/region-connector-nl-mijn-aansluiting.md",
                },
                {
                  text: "Green Button (USA)",
                  link: "/1-running/region-connectors/region-connector-us-green-button.md",
                },
                {
                  text: "CDS",
                  link: "/1-running/region-connectors/region-connector-cds.md",
                },
              ],
            },
            {
              text: "Outbound-connectors",
              link: "/1-running/outbound-connectors/outbound-connectors.md",
              items: [
                {
                  text: "Apache Kafka",
                  link: "/1-running/outbound-connectors/outbound-connector-kafka.md",
                },
                {
                  text: "AMQP",
                  link: "/1-running/outbound-connectors/outbound-connector-amqp.md",
                },
              ],
            },
            { text: "Admin Console", link: "/1-running/admin-console" },
            { text: "Demo Button", link: "/1-running/demo-button" },
            { text: "Example App", link: "/1-running/example-app" },
          ],
        },
        {
          text: "Integrating",
          link: "/2-integrating/integrating.md",
          items: [
            {
              text: "Data Needs",
              link: "/2-integrating/data-needs.md",
            },
            {
              text: "Messages and Documents",
              link: "/2-integrating/messages/messages.md",
              items: [
                {
                  text: "Agnostic",
                  link: "/2-integrating/messages/agnostic.md",
                  items: [
                    {
                      text: "Connection Status Messages",
                      link: "/2-integrating/messages/agnostic.md#connection-status-messages",
                    },
                    {
                      text: "Raw Data Messages",
                      link: "/2-integrating/messages/agnostic.md#raw-data-messages",
                    }
                  ]
                },
                {
                  text: "Common Information Model (CIM)",
                  link: "/2-integrating/messages/cim/cim.md",
                  items: [
                    {
                      text: "Permission Market Documents",
                      link: "/2-integrating/messages/cim/permission-market-documents.md",
                    },
                    {
                      text: "Validated Historical Data Market Documents",
                      link: "/2-integrating/messages/cim/validated-historical-data-market-documents.md",
                    },
                    {
                      text: "Accounting Point Data Market Documents",
                      link: "/2-integrating/messages/cim/accounting-point-data-market-documents.md",
                    },
                    {
                      text: "Redistribution Transaction Request Documents",
                      link: "/2-integrating/messages/cim/redistribution-transaction-request-documents.md",
                    },
                    {
                      text: "Client Libraries",
                      link: "/2-integrating/messages/cim/client-libraries.md",
                    },
                  ],
                },
              ],
            },
          ],
        },
        {
          text: "Extending",
          items: [
            { text: "Tech Stack", link: "/3-extending/tech-stack" },
            {
              text: "Add a region connector",
              link: "/3-extending/region-connector/add-region-connector",
              items: [
                {
                  text: "Quickstart",
                  link: "/3-extending/region-connector/quickstart",
                },
                {
                  text: "Build and Setup",
                  link: "/3-extending/region-connector/build-and-setup",
                },
                {
                  text: "API",
                  link: "/3-extending/region-connector/api",
                },
                {
                  text: "Internal Architecture",
                  link: "/3-extending/region-connector/internal-architecture",
                },
                {
                  text: "Configuration",
                  link: "/3-extending/region-connector/configuration",
                },
                {
                  text: "Frontend",
                  link: "/3-extending/region-connector/frontend",
                },
                {
                  text: "Beans of Interest",
                  link: "/3-extending/region-connector/beans-of-interest",
                },
                {
                  text: "Shared Functionality",
                  link: "/3-extending/region-connector/shared-functionality",
                },
                {
                  text: "Dispatcher Servlet",
                  link: "/3-extending/region-connector/dispatcher-servlet",
                },
              ],
            },
            {
              text: "Add an outbound-connector",
              link: "/3-extending/add-outbound-connector",
            },
            { text: "Edit Documentation", link: "/3-extending/documentation" },
          ],
        },
      ],

      socialLinks: [
        { icon: "github", link: "https://github.com/eddie-energy/eddie" },
      ],
    },
  })
);
