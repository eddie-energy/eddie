/*
 * SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
 * SPDX-License-Identifier: Apache-2.0
 */

import type MarkdownIt from "markdown-it";

/**
 * Converts Spring properties into environment variables and YAML format
 * if they are located in a code fence 'properties :spring', and shows them in a code group.
 */
export function propertiesPlugin(md: MarkdownIt) {
  const originalFence = md.renderer.rules.fence!;
  md.renderer.rules.fence = (...args) => {
    const [tokens, idx] = args;
    const token = tokens[idx];

    if (!token.info.trim().startsWith("properties :spring")) {
      return originalFence(...args);
    }

    const propertiesContent = trimLines(token.content);

    const envContent = propertiesToEnv(propertiesContent);
    const yamlContent = propertiesToYaml(propertiesContent);

    const markdownContent = `
::: code-group
\`\`\`properties [application.properties]
${propertiesContent}
\`\`\`
\`\`\`yaml [application.yml]
${yamlContent}
\`\`\`
\`\`\`dotenv [Environment Variables]
${envContent}
\`\`\`
:::
      `.trim();

    return md.render(markdownContent);
  };
}

function trimLines(content: string) {
  return content
    .split("\n")
    .map((line) => line.trim())
    .join("\n");
}

function propertiesToEnv(properties: string) {
  return properties
    .split("\n")
    .map((line) => {
      // Keep empty lines and comments
      if (line === "" || line.startsWith("#")) return line;
      // Make key uppercase and replace non-alphanumeric characters with underscores
      const [key, value] = line.split(/=(.*)/);
      return `${key.toUpperCase().replace(/[^A-Z0-9]/g, "_")}=${value}`;
    })
    .join("\n");
}

type Tree = {
  [key: string]: string | Tree;
};

function propertiesToYaml(properties: string) {
  const obj: Tree = {};
  let commentCount = 0;
  let emptyCount = 0;

  const lines = properties.split("\n");
  for (const line of lines) {
    if (line === "") {
      obj["empty" + emptyCount] = null;
      emptyCount++;
      continue;
    }
    // Save comments
    if (line.startsWith("#")) {
      obj["comment" + commentCount] = line;
      commentCount++;
      continue;
    }

    const [key, value] = line.split(/=(.*)/);

    const parts = key.split(".");
    let current = obj;

    // Traverse or create nested objects
    for (let i = 0; i < parts.length - 1; i++) {
      const part = parts[i];
      if (!current[part]) {
        current[part] = {};
      }
      current = current[part] as Tree;
    }

    // Assign final value
    current[parts[parts.length - 1]] = value;
  }

  return objectToYaml(obj);
}

function objectToYaml(obj: Tree, level = 0) {
  let yamlString = "";
  const indent = "  ".repeat(level);

  for (const key in obj) {
    if (key.startsWith("empty")) {
      yamlString += `\n`;
      continue;
    }

    if (key.startsWith("comment")) {
      yamlString += `${obj[key]}\n`;
      continue;
    }

    if (typeof obj[key] === "string") {
      yamlString += `${indent}${key}: ${obj[key]}\n`;
      continue;
    }

    yamlString += `${indent}${key}:\n${objectToYaml(obj[key], level + 1)}`;
  }

  return yamlString;
}
