const REQUIRED_SCHEME = 'x402'
const DEFAULT_PAYMENT_HEADER = 'X-PAYMENT'
const DEFAULT_EIP3009_DOMAIN_NAME = 'USD Coin'
const DEFAULT_EIP3009_DOMAIN_VERSION = '2'
const USDC_DECIMALS = 6

type JsonRecord = Record<string, unknown>

export type X402PaymentChallenge = {
  scheme: string
  network: string
  chainId: number
  tokenAddress: string
  priceUsdc: string
  recipient: string
  paymentHeader: string
  nonce: string
  challengeExpiresAt: string
}

export type SignedX402PaymentHeader = {
  headerName: string
  headerValue: string
}

type BuildSignedX402PaymentHeaderInput = {
  challenge: X402PaymentChallenge
  agentWalletAddress: string
  provider?: Eip1193Provider
}

type Eip712TypedData = {
  types: {
    EIP712Domain: Array<{ name: string; type: string }>
    TransferWithAuthorization: Array<{ name: string; type: string }>
  }
  primaryType: 'TransferWithAuthorization'
  domain: {
    name: string
    version: string
    chainId: number
    verifyingContract: string
  }
  message: {
    from: string
    to: string
    value: string
    validAfter: string
    validBefore: string
    nonce: string
  }
}

export interface Eip1193Provider {
  request(args: { method: string; params?: unknown[] | object }): Promise<unknown>
}

export function parseX402Challenge(rawBody: string): X402PaymentChallenge | null {
  if (!rawBody || !rawBody.trim()) {
    return null
  }

  let parsed: unknown
  try {
    parsed = JSON.parse(rawBody)
  } catch {
    return null
  }

  if (!isJsonRecord(parsed)) {
    return null
  }

  const scheme = readString(parsed, 'scheme')
  const network = readString(parsed, 'network')
  const tokenAddress = readString(parsed, 'tokenAddress')
  const recipient = readString(parsed, 'recipient')
  const nonce = readString(parsed, 'nonce')
  const challengeExpiresAt = readString(parsed, 'challengeExpiresAt')

  if (
    !scheme ||
    scheme.toLowerCase() !== REQUIRED_SCHEME ||
    !network ||
    !tokenAddress ||
    !recipient ||
    !nonce ||
    !challengeExpiresAt
  ) {
    return null
  }

  const chainId = readNumber(parsed, 'chainId')
  const priceUsdc = readDecimalString(parsed, 'priceUsdc')
  if (chainId == null || priceUsdc == null) {
    return null
  }

  const paymentHeader = readString(parsed, 'paymentHeader') || DEFAULT_PAYMENT_HEADER

  return {
    scheme,
    network,
    chainId,
    tokenAddress,
    priceUsdc,
    recipient,
    paymentHeader,
    nonce,
    challengeExpiresAt,
  }
}

export async function buildSignedX402PaymentHeader(
  input: BuildSignedX402PaymentHeaderInput
): Promise<SignedX402PaymentHeader> {
  const provider = resolveProvider(input.provider)
  const challenge = input.challenge
  const targetChainHex = toChainHex(challenge.chainId)
  await ensureWalletChain(provider, challenge.chainId, targetChainHex)

  const signerWallet = await requestSignerWalletAddress(provider)
  const expectedWallet = normalizeAddress(input.agentWalletAddress, 'selected agent wallet')
  if (signerWallet !== expectedWallet) {
    throw new Error(
      `Connected wallet ${shortAddress(signerWallet)} does not match selected agent wallet ${shortAddress(expectedWallet)}.`
    )
  }

  const nowEpochSeconds = Math.floor(Date.now() / 1000)
  const challengeExpiresAtEpochSeconds = toEpochSeconds(challenge.challengeExpiresAt)
  if (challengeExpiresAtEpochSeconds <= nowEpochSeconds + 5) {
    throw new Error('x402 challenge expired before signing. Refresh and try again.')
  }

  const validAfter = Math.max(0, nowEpochSeconds - 30)
  const validBefore = challengeExpiresAtEpochSeconds
  const authorizationNonce = randomBytes32Hex()
  const transferValue = usdcToBaseUnits(challenge.priceUsdc, USDC_DECIMALS)

  const typedData: Eip712TypedData = {
    types: {
      EIP712Domain: [
        { name: 'name', type: 'string' },
        { name: 'version', type: 'string' },
        { name: 'chainId', type: 'uint256' },
        { name: 'verifyingContract', type: 'address' },
      ],
      TransferWithAuthorization: [
        { name: 'from', type: 'address' },
        { name: 'to', type: 'address' },
        { name: 'value', type: 'uint256' },
        { name: 'validAfter', type: 'uint256' },
        { name: 'validBefore', type: 'uint256' },
        { name: 'nonce', type: 'bytes32' },
      ],
    },
    primaryType: 'TransferWithAuthorization',
    domain: {
      name: DEFAULT_EIP3009_DOMAIN_NAME,
      version: DEFAULT_EIP3009_DOMAIN_VERSION,
      chainId: challenge.chainId,
      verifyingContract: normalizeAddress(challenge.tokenAddress, 'challenge tokenAddress'),
    },
    message: {
      from: signerWallet,
      to: normalizeAddress(challenge.recipient, 'challenge recipient'),
      value: transferValue,
      validAfter: String(validAfter),
      validBefore: String(validBefore),
      nonce: authorizationNonce,
    },
  }

  const signature = await provider.request({
    method: 'eth_signTypedData_v4',
    params: [signerWallet, JSON.stringify(typedData)],
  })

  if (typeof signature !== 'string' || !/^0x[0-9a-fA-F]{130}$/.test(signature)) {
    throw new Error('Wallet returned an invalid EIP-3009 signature.')
  }

  const paymentHeaderPayload = {
    requestNonce: challenge.nonce,
    idempotencyKey: randomIdempotencyKey(),
    payload: {
      authorizationNonce,
      domain: typedData.domain,
      authorization: {
        from: typedData.message.from,
        to: typedData.message.to,
        value: typedData.message.value,
        validAfter,
        validBefore,
        nonce: authorizationNonce,
        signature,
      },
    },
  }

  return {
    headerName: challenge.paymentHeader || DEFAULT_PAYMENT_HEADER,
    headerValue: JSON.stringify(paymentHeaderPayload),
  }
}

async function ensureWalletChain(
  provider: Eip1193Provider,
  targetChainId: number,
  targetChainHex: string
): Promise<void> {
  const rawChainId = await provider.request({ method: 'eth_chainId' })
  const currentChainId = parseChainId(rawChainId)
  if (currentChainId === targetChainId) {
    return
  }

  try {
    await provider.request({
      method: 'wallet_switchEthereumChain',
      params: [{ chainId: targetChainHex }],
    })
  } catch {
    throw new Error(
      `Wallet is connected to chain ${currentChainId}. Switch to chain ${targetChainId} and retry.`
    )
  }
}

async function requestSignerWalletAddress(provider: Eip1193Provider): Promise<string> {
  const rawAccounts = await provider.request({ method: 'eth_requestAccounts' })
  if (!Array.isArray(rawAccounts) || rawAccounts.length === 0 || typeof rawAccounts[0] !== 'string') {
    throw new Error('No EVM wallet account is connected.')
  }
  return normalizeAddress(rawAccounts[0], 'wallet account')
}

function resolveProvider(provider?: Eip1193Provider): Eip1193Provider {
  if (provider) {
    return provider
  }

  if (typeof window === 'undefined') {
    throw new Error('Wallet provider is unavailable outside the browser.')
  }

  const windowWithEthereum = window as Window & { ethereum?: Eip1193Provider }
  if (!windowWithEthereum.ethereum) {
    throw new Error('No EVM wallet detected. Install MetaMask (or another EIP-1193 wallet) and retry.')
  }

  return windowWithEthereum.ethereum
}

function parseChainId(value: unknown): number {
  let parsed: number
  if (typeof value === 'number' && Number.isInteger(value) && value >= 0) {
    parsed = value
  } else if (typeof value === 'string') {
    if (value.startsWith('0x') || value.startsWith('0X')) {
      parsed = Number.parseInt(value, 16)
    } else {
      parsed = Number.parseInt(value, 10)
    }
  } else {
    throw new Error('Wallet returned an invalid chain id.')
  }

  if (!Number.isInteger(parsed) || parsed < 0) {
    throw new Error('Wallet returned an invalid chain id.')
  }
  return parsed
}

function toChainHex(chainId: number): string {
  if (!Number.isInteger(chainId) || chainId <= 0) {
    throw new Error(`Invalid challenge chain id: ${chainId}`)
  }
  return `0x${chainId.toString(16)}`
}

function toEpochSeconds(value: string): number {
  const epochMillis = Date.parse(value)
  if (Number.isNaN(epochMillis)) {
    throw new Error('Invalid x402 challenge expiration timestamp.')
  }
  return Math.floor(epochMillis / 1000)
}

function usdcToBaseUnits(amountUsdc: string, decimals: number): string {
  const normalized = amountUsdc.trim()
  if (!/^\d+(\.\d+)?$/.test(normalized)) {
    throw new Error(`Invalid USDC amount in x402 challenge: ${amountUsdc}`)
  }

  const [wholePart, fractionPart = ''] = normalized.split('.')
  if (fractionPart.length > decimals) {
    throw new Error(`USDC amount precision exceeds ${decimals} decimals: ${amountUsdc}`)
  }

  const paddedFraction = `${fractionPart}${'0'.repeat(decimals)}`.slice(0, decimals)
  const whole = BigInt(wholePart)
  const fraction = BigInt(paddedFraction || '0')
  const multiplier = 10n ** BigInt(decimals)
  return (whole * multiplier + fraction).toString()
}

function randomBytes32Hex(): string {
  if (!globalThis.crypto?.getRandomValues) {
    throw new Error('Secure random generation is unavailable in this environment.')
  }

  const bytes = new Uint8Array(32)
  globalThis.crypto.getRandomValues(bytes)
  const hex = Array.from(bytes, (value) => value.toString(16).padStart(2, '0')).join('')
  return `0x${hex}`
}

function randomIdempotencyKey(): string {
  if (globalThis.crypto?.randomUUID) {
    return globalThis.crypto.randomUUID()
  }

  return randomBytes32Hex().slice(2)
}

function normalizeAddress(address: string, label: string): string {
  const value = address.trim().toLowerCase()
  if (!/^0x[0-9a-f]{40}$/.test(value)) {
    throw new Error(`Invalid ${label}: ${address}`)
  }
  return value
}

function shortAddress(address: string): string {
  if (address.length < 10) {
    return address
  }
  return `${address.slice(0, 6)}...${address.slice(-4)}`
}

function isJsonRecord(value: unknown): value is JsonRecord {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function readString(input: JsonRecord, key: string): string | null {
  const value = input[key]
  if (typeof value !== 'string') {
    return null
  }
  const trimmed = value.trim()
  return trimmed.length > 0 ? trimmed : null
}

function readNumber(input: JsonRecord, key: string): number | null {
  const value = input[key]
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }
  if (typeof value === 'string' && value.trim()) {
    const parsed = Number.parseInt(value, 10)
    if (Number.isFinite(parsed)) {
      return parsed
    }
  }
  return null
}

function readDecimalString(input: JsonRecord, key: string): string | null {
  const value = input[key]
  if (typeof value === 'number' && Number.isFinite(value) && value >= 0) {
    return value.toString()
  }
  if (typeof value === 'string' && /^\d+(\.\d+)?$/.test(value.trim())) {
    return value.trim()
  }
  return null
}
