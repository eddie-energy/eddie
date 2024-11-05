import { defineConfig } from "vitepress";

// https://vitepress.dev/reference/site-config
export default defineConfig({
  title: "EDDIE Framework",
  description: "Using and Extending the EDDIE Framework",
  ignoreDeadLinks: "localhostLinks",
  themeConfig: {
    // https://vitepress.dev/reference/default-theme-config
    nav: [
      { text: "Home", link: "/" },
    ],

    sidebar: [
      {
        text: "Running",
        items: [
          { text: "Operation", link: "/OPERATION" },
        ],
      },
      {
        text: "Using",
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
});
