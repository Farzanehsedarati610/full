# 1. Clean hashes (modify sed as needed)
find . -type f -name "*.html" -exec sed -i -E 's/^.*=\s*((\([0-9]+\))(\([0-9]+\)))\}$/\1/' {} +

# 2. Append USD value (assuming hash-to-USD.txt has lines like: (hash) - $USD)
awk 'NR==FNR {map[$1]=$3; next} {print $0, "-", map[$0]}' hash-to-USD.txt cleaned-values.txt > final-output.txt

# 3. Commit & push
git add .
git commit -m "Update hash mappings and USD values"
git push origin main

