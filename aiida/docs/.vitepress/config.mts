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
          link: "https://eddie-web.projekte.fh-hagenberg.at/architecture",
        },
        {
          text: "EDDIE Framework",
          link: "https://eddie-web.projekte.fh-hagenberg.at/framework",
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
          ],
        },
        {
          text: "Integrating",
          link: "/2-integrating/integrating.md",
        },
        {
          text: "Extending",
          link: "/3-extending/extending.md",
        },
      ],

      socialLinks: [
        { icon: "github", link: "https://github.com/eddie-energy/eddie" },
      ],
    },
  }),
);
