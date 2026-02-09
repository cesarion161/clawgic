#!/bin/bash
set -e

# MoltRank Token Faucet
# Airdrops SURGE tokens to test wallets on devnet

CONFIG_FILE="config/token.json"

# Check if config exists
if [ ! -f "$CONFIG_FILE" ]; then
    echo "❌ Token config not found. Run scripts/setup-token.sh first."
    exit 1
fi

# Read token configuration
MINT_ADDRESS=$(grep '"mintAddress"' $CONFIG_FILE | sed 's/.*"mintAddress":[[:space:]]*"\([^"]*\)".*/\1/')

if [ -z "$MINT_ADDRESS" ]; then
    echo "❌ Could not read mint address from config"
    exit 1
fi

# Default airdrop amount: 1000 tokens
AIRDROP_AMOUNT=${2:-1000}

# Check if wallet address provided
if [ -z "$1" ]; then
    echo "Usage: $0 <wallet_address> [amount]"
    echo ""
    echo "Example:"
    echo "  $0 5FHneW1nPE5ZxKZZJeR8UrqFXqzG8qXYz8YCh5VY6KSr 1000"
    echo ""
    echo "Defaults:"
    echo "  Amount: 1000 SURGE tokens"
    exit 1
fi

WALLET_ADDRESS=$1

# Configure Solana CLI for devnet
solana config set --url https://api.devnet.solana.com > /dev/null 2>&1

echo "💧 Airdropping $AIRDROP_AMOUNT SURGE tokens to $WALLET_ADDRESS..."

# Check if recipient has a token account for this mint
HAS_ACCOUNT=$(spl-token accounts --owner $WALLET_ADDRESS 2>/dev/null | grep -c $MINT_ADDRESS || echo "0")

if [ "$HAS_ACCOUNT" = "0" ]; then
    echo "📦 Creating associated token account for recipient..."
    spl-token create-account $MINT_ADDRESS --owner $WALLET_ADDRESS
fi

# Transfer tokens
spl-token transfer $MINT_ADDRESS $AIRDROP_AMOUNT $WALLET_ADDRESS --fund-recipient

echo "✅ Airdrop complete!"
echo "   Recipient: $WALLET_ADDRESS"
echo "   Amount: $AIRDROP_AMOUNT SURGE"
echo ""
