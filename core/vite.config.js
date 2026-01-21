import { resolve } from "node:path";
import { defineConfig } from "vite";

// format current date as YYYYMMdd and use it as build version indicator (same format as GitHub tags)
const currentDate = new Date();
const formattedDate = currentDate.getFullYear().toString() +
  (currentDate.getMonth() + 1).toString().padStart(2, "0") +
  currentDate.getDate().toString().padStart(2, "0");

export default defineConfig({
  define: {
    __EDDIE_VERSION__: formattedDate,
  },
  build: {
    outDir: resolve(__dirname, "src/main/resources/public/lib"),
    assetsDir: "",
    rollupOptions: {
      input: {
        "eddie-components": resolve(__dirname, "src/main/js/eddie-components.js"),
        "data-need-summary": resolve(__dirname, "src/main/js/data-need-summary.js"),
      },
      output: {
        entryFileNames: "[name].js",
      },
    },
  },
  server: {
    open: "./src/main/resources/templates/demo.html",
  },
});
