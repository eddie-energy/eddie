# Edit Documentation

The technical documentation of the EDDIE Framework including this page is stored as Markdown files in the project
repository contained in the `/docs` folder which are generated into a website using [VitePress](https://vitepress.dev/).
Editing and adding markdown files shouldn't be different from anywhere else.

## Important Information for Editing

- VitePress configuration and styling is located in the `/docs/.vitepress` folder.
- The menu structure is contained in the file `/docs/.vitepress/config.mts` and new files have to be added there to
  appear in the Menu on the left side.
- VitePress will check for dead links in Markdown files and abort site generation if it identifies one.
- VitePress has some build in Markdown Extensions that might help [VitePress: Markdown Extensions](https://vitepress.dev/guide/markdown)
- A VitePress plugin for generating mermaid images from Markdown files is included, see
  [vitepress-plugin-mermaid](https://github.com/emersonbottero/vitepress-plugin-mermaid). To render a mermaid image the
  code block should start with `` ```mermaid`` and to show the literal mermaid code `` ```mmd`` should be used.

## Documentation Site Generation

`gradlew pnpmBuildDocs` in the project root will generate the documentation website using Gradle and pnpm which can be found in
`/docs/.vitepress/dist`.

For local development running the following commands in the `/docs` folder can become handy:

- `pnpm install`: to install needed JS dependencies
- `pnpm docs:dev`: will start a live-preview mode of the documentation site
- `pnpm docs:build`: will build the documentation website into `/docs/.vitepress/dist`
- `pnpm docs:preview`: will serve the previously generated website as it will be served by a webserver
