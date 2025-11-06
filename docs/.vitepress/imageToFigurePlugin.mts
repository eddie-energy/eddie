import type MarkdownIt from "markdown-it";

/**
 * Transforms a markdown image into an html figure.
 * @param md
 * @see https://github.com/vuejs/vitepress/issues/3262#issuecomment-2019023339
 */
export function imageToFigurePlugin(md: MarkdownIt) {
  const _super = md.renderer.rules.image;
  md.renderer.rules.image = function(tokens, idx, options, env, self) {
    let title = tokens[idx].attrs[2];
    if (title) {
      title = title[1];
      const src = tokens[idx].attrs[0][1];
      const alt = tokens[idx].content;
      return `
                        <figure>
                            <img src="${src}" alt="${alt}" title="${title}" />
                            <figcaption>
                                <small>${title}</small>
                            </figcaption>
                        </figure>`;
    }
    return _super(tokens, idx, options, env, self);
  };
}