import { describe, expect, it, vi } from 'vitest'
import {
  buildSignedX402PaymentHeader,
  parseX402Challenge,
  type Eip1193Provider,
  type X402PaymentChallenge,
} from './x402-payment'

function challengeFixture(): X402PaymentChallenge {
  return {
    scheme: 'x402',
    network: 'base-sepolia',
    chainId: 84532,
    tokenAddress: '0x0000000000000000000000000000000000000a11',
    priceUsdc: '5.000000',
    recipient: '0x0000000000000000000000000000000000000b22',
    paymentHeader: 'X-PAYMENT',
    nonce: 'x402-challenge-nonce-001',
    challengeExpiresAt: new Date(Date.now() + 5 * 60_000).toISOString(),
  }
}

describe('x402 payment helpers', () => {
  it('parses a valid x402 challenge payload', () => {
    const challenge = challengeFixture()
    const parsed = parseX402Challenge(JSON.stringify(challenge))

    expect(parsed).not.toBeNull()
    expect(parsed?.scheme).toBe('x402')
    expect(parsed?.chainId).toBe(84532)
    expect(parsed?.priceUsdc).toBe('5.000000')
  })

  it('returns null for non-x402 challenge payloads', () => {
    const parsed = parseX402Challenge(
      JSON.stringify({
        scheme: 'other',
        chainId: 84532,
      })
    )
    expect(parsed).toBeNull()
  })

  it('builds signed payment header JSON from challenge and wallet signer', async () => {
    const providerRequest = vi.fn(async (args: { method: string; params?: unknown[] | object }) => {
      if (args.method === 'eth_chainId') {
        return '0x14a34'
      }
      if (args.method === 'eth_requestAccounts') {
        return ['0x1111111111111111111111111111111111111111']
      }
      if (args.method === 'eth_signTypedData_v4') {
        return `0x${'1'.repeat(130)}`
      }
      return null
    })

    const provider: Eip1193Provider = {
      request: providerRequest,
    }

    const header = await buildSignedX402PaymentHeader({
      challenge: challengeFixture(),
      agentWalletAddress: '0x1111111111111111111111111111111111111111',
      provider,
    })

    expect(header.headerName).toBe('X-PAYMENT')
    const parsedHeader = JSON.parse(header.headerValue) as {
      requestNonce: string
      idempotencyKey: string
      payload: {
        authorizationNonce: string
        domain: {
          chainId: number
        }
        authorization: {
          from: string
          to: string
          value: string
          signature: string
        }
      }
    }

    expect(parsedHeader.requestNonce).toBe('x402-challenge-nonce-001')
    expect(parsedHeader.idempotencyKey.length).toBeGreaterThan(0)
    expect(parsedHeader.payload.authorizationNonce).toMatch(/^0x[0-9a-f]{64}$/)
    expect(parsedHeader.payload.domain.chainId).toBe(84532)
    expect(parsedHeader.payload.authorization.from).toBe('0x1111111111111111111111111111111111111111')
    expect(parsedHeader.payload.authorization.to).toBe('0x0000000000000000000000000000000000000b22')
    expect(parsedHeader.payload.authorization.value).toBe('5000000')
    expect(parsedHeader.payload.authorization.signature).toBe(`0x${'1'.repeat(130)}`)

    const signCall = providerRequest.mock.calls.find(
      ([args]) => (args as { method?: string }).method === 'eth_signTypedData_v4'
    )
    expect(signCall).toBeDefined()
    const signParams = (signCall?.[0] as { params?: unknown[] }).params as unknown[]
    const typedData = JSON.parse(signParams[1] as string) as {
      domain: {
        chainId: number
        name: string
      }
      message: {
        value: string
      }
    }

    expect(typedData.domain.chainId).toBe(84532)
    expect(typedData.domain.name).toBe('USD Coin')
    expect(typedData.message.value).toBe('5000000')
  })

  it('rejects signing when connected wallet does not match selected agent wallet', async () => {
    const provider: Eip1193Provider = {
      request: vi.fn(async (args: { method: string }) => {
        if (args.method === 'eth_chainId') {
          return '0x14a34'
        }
        if (args.method === 'eth_requestAccounts') {
          return ['0x9999999999999999999999999999999999999999']
        }
        if (args.method === 'eth_signTypedData_v4') {
          return `0x${'2'.repeat(130)}`
        }
        return null
      }),
    }

    await expect(
      buildSignedX402PaymentHeader({
        challenge: challengeFixture(),
        agentWalletAddress: '0x1111111111111111111111111111111111111111',
        provider,
      })
    ).rejects.toThrow(/does not match selected agent wallet/i)
  })
})
