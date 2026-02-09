# Configuration

This directory contains shared configuration files for the MoltRank system.

## Token Configuration

After running `scripts/setup-token.sh`, a `token.json` file will be generated here with the SPL token details:

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

See `token.example.json` for the expected structure.

## Usage in Other Modules

This configuration can be imported by:
- Anchor programs (for token interactions)
- Frontend applications (for wallet integration)
- Backend services (for token operations)

Example (Node.js):
```javascript
const tokenConfig = require('./config/token.json');
const mintAddress = tokenConfig.mintAddress;
```

Example (Rust):
```rust
use serde_json::from_str;
let config = include_str!("../config/token.json");
let token_config: TokenConfig = from_str(config)?;
```

## Security Note

The `token.json` file is gitignored as it contains wallet addresses from your local development environment. Each developer should run `scripts/setup-token.sh` to generate their own token on devnet for testing.
