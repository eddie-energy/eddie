import { resolve } from "path";
import { defineConfig } from "vite";

export default defineConfig({
  build: {
    outDir: resolve(
      __dirname,
      "src/main/resources/public/region-connectors/at-eda"
    ),
    assetsDir: "",
    rollupOptions: {
      input: resolve(__dirname, "src/main/web/permission-request-form.js"),
      output: {
        entryFileNames: "ce.js",
      },
      preserveEntrySignatures: "strict",
    },
  },
});
