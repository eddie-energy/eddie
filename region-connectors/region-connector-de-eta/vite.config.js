// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import { resolve } from "path";
import { defineConfig } from "vite";

export default defineConfig({
  build: {
    outDir: resolve(
      __dirname,
      "src/main/resources/public/region-connectors/de-eta"
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
