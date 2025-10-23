import { defineConfig } from "vitepress";
import { withMermaid } from "vitepress-plugin-mermaid";
import { transformerTwoslash } from "@shikijs/vitepress-twoslash";
import { propertiesPlugin } from "./propertiesPlugin.mjs";

// https://vitepress.dev/reference/site-config
export default withMermaid(
  defineConfig({
    title: "AIIDA",
    description: "Using and Extending AIIDA",
    ignoreDeadLinks: "localhostLinks",
    head: [["link", { rel: "icon", href: "/aiida/images/favicon.svg" }]],
    vite: {
      // workaround for a vite/pnpm related mermaid bug: https://github.com/mermaid-js/mermaid/issues/4320
      // bug occurs in vitepress dev mode only
      optimizeDeps: { include: ["mermaid"] },
    },
    base: "/aiida/",
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
          link: "https://architecture.eddie.energy/architecture",
        },
        {
          text: "EDDIE Framework",
          link: "https://architecture.eddie.energy/framework",
        },
      ],
      search: {
        provider: "local",
      },
      sidebar: [
        {
          text: "Running",
          items: [
            {
              text: "Operation",
              link: "/1-running/OPERATION.md",
            },
            {
              text: "Components",
              link: "/1-running/components.md",
            },
            {
              text: "Database",
              link: "/1-running/database.md",
            },
            {
              text: "Keycloak",
              link: "/1-running/keycloak.md",
            },
            {
              text: "Data Sources",
              link: "/1-running/data-sources/data-sources.md",
              collapsed: true,
              items: [
                {
                  text: "MQTT-based",
                  link: "/1-running/data-sources/mqtt/mqtt-data-sources.md",
                  items: [
                    {
                      text: "CIM",
                      link: "/1-running/data-sources/mqtt/cim/cim-data-source.md",
                    },
                    {
                      text: "Inbound",
                      link: "/1-running/data-sources/mqtt/inbound/inbound-data-source.md",
                    },
                    {
                      text: "Micro Teleinfo v3.0 (France)",
                      link: "/1-running/data-sources/mqtt/fr/micro-teleinfo-v3-data-source.md",
                    },
                    {
                      text: "Österreichs Energie (Austria)",
                      link: "/1-running/data-sources/mqtt/at/oesterreichs-energie-data-source.md",
                    },
                    {
                      text: "Shelly",
                      link: "/1-running/data-sources/mqtt/shelly/shelly-data-source.md",
                    },
                    {
                      text: "Sinapsi Alfa (Italy)",
                      link: "/1-running/data-sources/mqtt/it/sinapsi-alfa-data-source.md",
                    },
                  ],
                },
                {
                  text: "Interval-based",
                  link: "/1-running/data-sources/interval/interval-data-sources.md",
                  items: [
                    {
                      text: "Simulation",
                      link: "/1-running/data-sources/interval/simulation/simulation-data-source.md",
                    },
                  ],
                },
              ],
            },
            {
              text: "EMQX",
              link: "/1-running/emqx.md"
            },
            {
              text: "Permissions",
              link: "/1-running/permission.md",
            },
            {
              text: "Data Flow",
              link: "/1-running/data-flow.md",
            },
            {
              text: "Errors and Logging",
              link: "/1-running/errors-logging.md",
            },
          ],
        },
        {
          text: "Integrating",
          items: [
            {
              text: "EDDIE Framework",
              link: "/2-integrating/eddie.md",
            },
            {
              text: "Schemas",
              collapsed: true,
              items: [
                {
                  text: "CIM",
                  link: "/2-integrating/schemas/cim/cim.md",
                  items: [
                    {
                      text: "Real Time Data Market Document",
                      link: "/2-integrating/schemas/cim/real-time-data-market-document.md",
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
            {
              text: "Adding a data source",
              link: "/3-extending/data-source/data-source.md",
              collapsed: true,
              items: [
                {
                  text: "Documentation",
                  link: "/3-extending/data-source/documentation.md",
                },
              ],
            },
            {
              text: "Edit documentation",
              link: "/3-extending/documentation",
            },
          ],
        },
      ],

      socialLinks: [
        { icon: "github", link: "https://github.com/eddie-energy/eddie" },
      ],
    },
  }),
);
