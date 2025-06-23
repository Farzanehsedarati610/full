#!/bin/bash

# Path to your private key
KEY_PATH="$HOME/.ssh/id_ed25519"

# Path to your vault map
MAP_PATH="vault_map.txt"

# Remote config file to patch
CONFIG_PATH="/etc/vault/vault.cfg"

# Loop through each line in vault_map.txt
while read cpu ip hash routing account; do
  echo "🚀 [$cpu] Dispatching to $ip"

  ssh -i "$KEY_PATH" -o StrictHostKeyChecking=no farza@"$ip" "
    if [ -f $CONFIG_PATH ]; then
      cp $CONFIG_PATH ${CONFIG_PATH}.bak
      sed -i 's/PLACEHOLDER_HASH/$hash/g' $CONFIG_PATH &&
      sed -i 's/PLACEHOLDER_ROUTING/$routing/g' $CONFIG_PATH &&
      sed -i 's/PLACEHOLDER_ACCOUNT/$account/g' $CONFIG_PATH &&
      echo '✅ [$cpu] vault.cfg updated.'
    else
      echo '⚠️ [$cpu] Config file missing on $ip'
    fi
  " || echo "❌ [$cpu] SSH failed on $ip"

done < "$MAP_PATH"

