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

    const propertiesContent = token.content.trim();

    const envContent = propertiesContent.replace(
      /^([^=]+)=(.*)$/gm,
      (_, key, value) =>
        key.toUpperCase().replace(/[^A-Z0-9]/g, "_") + "=" + value
    );

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

type Tree = {
  [key: string]: string | Tree;
};

function propertiesToYaml(properties: string) {
  const obj: Tree = {};

  properties.split("\n").forEach((line) => {
    const [key, value] = line.split("=");

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
  });

  return objectToYaml(obj);
}

function objectToYaml(obj: Tree, level = 0) {
  let yamlString = "";
  const indent = "  ".repeat(level);

  for (const key in obj) {
    if (typeof obj[key] === "string") {
      yamlString += `${indent}${key}: ${obj[key]}\n`;
    } else {
      yamlString += `${indent}${key}:\n${objectToYaml(obj[key], level + 1)}`;
    }
  }

  return yamlString;
}
