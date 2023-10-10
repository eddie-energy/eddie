import { resolve } from "path";
import { defineConfig } from "vite";

export default defineConfig({
  build: {
    outDir: resolve(__dirname, "src/main/resources/public/lib"),
    assetsDir: "",
    rollupOptions: {
      input: resolve(__dirname, "src/main/js/eddie-components.js"),
      output: {
        entryFileNames: "[name].js",
      },
    },
  },
});
