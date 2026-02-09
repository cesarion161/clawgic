# MoltRank Token Scripts

Scripts for managing the SURGE SPL token on Solana devnet.

## Prerequisites

Install Solana CLI tools:
```bash
sh -c "$(curl -sSfL https://release.solana.com/stable/install)"
```

## Setup

### 1. Create Token Mint

Run the setup script to create the SURGE token on devnet:

```bash
./scripts/setup-token.sh
```

This will:
- Configure Solana CLI for devnet
- Create or use existing wallet (`~/.config/solana/id.json`)
- Request SOL airdrop for transaction fees
- Create SURGE token mint with 9 decimals
- Create initial token account
- Mint 1 billion SURGE tokens for testing
- Save configuration to `config/token.json`

### 2. Airdrop Tokens to Test Wallets

Use the faucet script to send tokens to test wallets:

```bash
./scripts/faucet.sh <wallet_address> [amount]
```

**Examples:**
```bash
# Airdrop 1000 SURGE (default amount)
./scripts/faucet.sh 5FHneW1nPE5ZxKZZJeR8UrqFXqzG8qXYz8YCh5VY6KSr

# Airdrop custom amount
./scripts/faucet.sh 5FHneW1nPE5ZxKZZJeR8UrqFXqzG8qXYz8YCh5VY6KSr 5000
```

The faucet will:
- Automatically create an associated token account if needed
- Transfer the requested amount of SURGE tokens
- Fund the recipient account if necessary

## Token Configuration

After running `setup-token.sh`, the token configuration is saved to `config/token.json`:

```json
{
  "network": "devnet",
  "mintAddress": "...",
  "mintAuthority": "...",
  "tokenAccount": "...",
  "decimals": 9,
  "initialSupply": 1000000000,
  "symbol": "SURGE",
  "createdAt": "..."
}
```

This configuration file can be used by other modules (Anchor programs, frontend, etc.) to interact with the token.

## Common Commands

View token supply:
```bash
spl-token supply <mint_address>
```

View token accounts:
```bash
spl-token accounts
```

Check token balance:
```bash
spl-token balance <mint_address>
```

## Notes

- This is for **devnet only** - not production
- The mint authority wallet is stored in `~/.config/solana/id.json`
- Keep the mint authority private key secure even on devnet
- Devnet tokens have no real value
