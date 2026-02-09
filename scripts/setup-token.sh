#!/bin/bash
set -e

# MoltRank SPL Token Setup Script
# Creates SURGE token on Solana devnet for testing

echo "🚀 Setting up MoltRank SURGE token on devnet..."

# Configure Solana CLI for devnet
echo "📡 Configuring Solana CLI for devnet..."
solana config set --url https://api.devnet.solana.com

# Check if wallet exists, create if not
if [ ! -f ~/.config/solana/id.json ]; then
    echo "🔑 Creating new Solana wallet..."
    solana-keygen new --no-bip39-passphrase
fi

# Airdrop SOL for transaction fees
echo "💰 Requesting SOL airdrop for transaction fees..."
solana airdrop 2 || echo "⚠️  Airdrop may have failed or rate limited, continuing..."

# Create token mint with 9 decimals
echo "🪙 Creating SURGE token mint (9 decimals)..."
MINT_ADDRESS=$(spl-token create-token --decimals 9 | grep -o 'Creating token [^ ]*' | awk '{print $3}')

if [ -z "$MINT_ADDRESS" ]; then
    echo "❌ Failed to create token mint"
    exit 1
fi

echo "✅ Token mint created: $MINT_ADDRESS"

# Create associated token account for the mint authority
echo "📦 Creating token account for mint authority..."
TOKEN_ACCOUNT=$(spl-token create-account $MINT_ADDRESS | grep -o 'Creating account [^ ]*' | awk '{print $3}')

# Mint initial supply (1 billion tokens for testing)
INITIAL_SUPPLY=1000000000
echo "💎 Minting initial supply: $INITIAL_SUPPLY SURGE tokens..."
spl-token mint $MINT_ADDRESS $INITIAL_SUPPLY

# Save configuration
echo "💾 Saving token configuration..."
CONFIG_FILE="config/token.json"
WALLET_ADDRESS=$(solana address)

cat > $CONFIG_FILE <<EOF
{
  "network": "devnet",
  "mintAddress": "$MINT_ADDRESS",
  "mintAuthority": "$WALLET_ADDRESS",
  "tokenAccount": "$TOKEN_ACCOUNT",
  "decimals": 9,
  "initialSupply": $INITIAL_SUPPLY,
  "symbol": "SURGE",
  "createdAt": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
}
EOF

echo ""
echo "✅ SURGE token setup complete!"
echo ""
echo "📋 Token Details:"
echo "   Mint Address:    $MINT_ADDRESS"
echo "   Token Account:   $TOKEN_ACCOUNT"
echo "   Mint Authority:  $WALLET_ADDRESS"
echo "   Decimals:        9"
echo "   Initial Supply:  $INITIAL_SUPPLY SURGE"
echo "   Config File:     $CONFIG_FILE"
echo ""
echo "🎯 Next steps:"
echo "   - Use scripts/faucet.sh to airdrop tokens to test wallets"
echo "   - Import token in Phantom: $MINT_ADDRESS"
echo ""
