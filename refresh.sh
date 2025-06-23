#!/bin/bash

echo "🔄 Substituting vault mappings into HTML files..."

for file in *.html; do
  if grep -q '{{SUBST_BLOCK}}' "$file"; then
    sed "/{{SUBST_BLOCK}}/{
      r vaultmap.subst
      d
    }" "$file" > temp && mv temp "$file"
    echo "✅ Refreshed $file"
  fi
done

echo "📦 Committing updated files..."
git add *.html
git commit -m 'Global substitution: embedded vault mappings'
git push

echo "🌍 Live on GitHub Pages!"

