import { resolve } from "path";
import { defineConfig } from "vite";

export default defineConfig({
  build: {
    // Needed for simulation.html
    emptyOutDir: false,
    outDir: resolve(
      __dirname,
      "src/main/resources/public/region-connectors/sim"
    ),
    assetsDir: "",
    rollupOptions: {
      input: {
        ce: resolve(__dirname, "src/main/web/permission-request-form.js"),
      },
      output: {
        entryFileNames: "[name].js",
      },
      preserveEntrySignatures: "strict",
    },
  },
});