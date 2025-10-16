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
              text: "Data Sources",
              link: "/1-running/datasources/data-sources.md",
              collapsed: true,
              items: [
                {
                  text: "MQTT-based",
                  link: "/1-running/datasources/mqtt/mqtt-data-sources.md",
                  items: [
                    {
                      text: "Inbound",
                      link: "/1-running/datasources/mqtt/inbound/inbound-data-source.md",
                    },
                    {
                      text: "Sinapsi Alfa (Italy)",
                      link: "/1-running/datasources/mqtt/it/sinapsi-alfa-data-source.md",
                    },
                  ],
                },
                {
                  text: "Interval-based",
                  link: "/1-running/datasources/interval/interval-data-sources.md",
                },
              ],
            },
            {
              text: "Permissions",
              link: "/1-running/permission/permission.md",
            },
            {
              text: "Keycloak",
              link: "/1-running/keycloak.md",
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
              link: "/3-extending/datasource/data-source.md",
              collapsed: true,
              items: [
                {
                  text: "Documentation",
                  link: "/3-extending/datasource/documentation.md",
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
