#!/bin/sh

#
# SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
# SPDX-License-Identifier: Apache-2.0
#

git ls-files -z |
while IFS= read -r -d '' file; do
	file=$(pwd)/$file
	if [[ "$file" == */.idea/* || "$file" == */.nvmrc || "$file" == */package.json || "$file" == */.gitignore || "$file" == */LICENSE || "$file" == */NOTICE || "$file" == */region-connector-us-green-button/src/main/schemas/* || "$file" == */.env || "$file" == "*/docker-compose.yml" || "$file" == */.prettierrc.json || "$file" == */eslint.config.ts || "$file" == */tsconfig.json || "$file" == */tsconfig.json || "$file" == */*.md  || "$file" == */*.http || "$file" == */.env ]]; then
		echo "Skipping $file"
		continue
	fi
	sidecar="--fallback-dot-license"
	if [[ "$file" == */migration/* ]]; then
		sidecar=--force-dot-license
  fi
	years=$(git blame --porcelain "$file" | grep '^author-time ' | sort -n | cut -f 2 -d' ' | xargs -I{} date -d @{} +%Y| uniq | awk 'NR==1{first=$0} {last=$0} END{if(first==last) print first; else print first "-" last}' )
	echo "$file $years"
	reuse annotate \
	  --copyright="The EDDIE Developers <eddie.developers@fh-hagenberg.at>" \
	  --license="Apache-2.0" \
	  --year=$years \
	  $sidecar \
	  --merge-copyrights \
	  --template=template \
	  -- \
	  "$file"
done
