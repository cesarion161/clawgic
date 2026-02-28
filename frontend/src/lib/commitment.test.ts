import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import { sha256 } from '@noble/hashes/sha2.js'
import {
  buildCommitAuthMessage,
  buildRevealPayload,
  commitRequestNonceHex,
  computeCommitmentHashHex,
  decodeRevealPayloadBase64,
  encryptRevealPayloadWithSignature,
  encodeRevealPayloadBase64,
  generateCommitRequestNonce,
  nonceFromHex,
  nonceHex,
  normalizeCommitmentHash,
} from './commitment'

interface CommitmentVector {
  name: string
  wallet: string
  pairId: number
  stakeAmount: number
  choice: 'A' | 'B'
  nonceHex: string
  revealPayloadBase64: string
  commitmentHash: string
}

function loadVectors(): CommitmentVector[] {
  const vectorsPath = resolve(process.cwd(), '..', 'config', 'commitment-test-vectors.json')
  return JSON.parse(readFileSync(vectorsPath, 'utf8')) as CommitmentVector[]
}

describe('commitment codec', () => {
  const vectors = loadVectors()

  it.each(vectors)('computes canonical hash and payload for $name', (vector) => {
    const nonce = nonceFromHex(vector.nonceHex)

    const payload = encodeRevealPayloadBase64(vector.choice, nonce)
    expect(payload).toBe(vector.revealPayloadBase64)

    const decoded = decodeRevealPayloadBase64(payload)
    expect(decoded.choice).toBe(vector.choice)
    expect(nonceHex(decoded.nonce)).toBe(vector.nonceHex.toLowerCase())

    const hash = computeCommitmentHashHex({
      wallet: vector.wallet,
      pairId: vector.pairId,
      choice: vector.choice,
      stakeAmount: vector.stakeAmount,
      nonce,
    })
    expect(hash).toBe(vector.commitmentHash.toLowerCase())
  })

  it('normalizes hash inputs with or without 0x prefix', () => {
    const digest = 'ABCD'.padEnd(64, '0')
    expect(normalizeCommitmentHash(`0x${digest}`)).toBe(`0x${digest.toLowerCase()}`)
    expect(normalizeCommitmentHash(digest)).toBe(`0x${digest.toLowerCase()}`)
  })

  it('rejects non-safe stake integers in hash preimage encoding', () => {
    const vector = vectors[0]
    const nonce = nonceFromHex(vector.nonceHex)

    expect(() =>
      computeCommitmentHashHex({
        wallet: vector.wallet,
        pairId: vector.pairId,
        choice: vector.choice,
        stakeAmount: Number.MAX_SAFE_INTEGER + 1,
        nonce,
      }),
    ).toThrow('stakeAmount must be a non-negative safe integer')
  })

  it('builds canonical commit auth message with normalized hash', () => {
    const message = buildCommitAuthMessage({
      wallet: vectors[0].wallet,
      pairId: vectors[0].pairId,
      commitmentHash: vectors[0].commitmentHash.toUpperCase(),
      stakeAmount: vectors[0].stakeAmount,
      signedAtEpochSeconds: 1730000000,
      requestNonceHex: '00112233445566778899aabbccddeeff',
    })

    expect(message).toContain('clawgic-commit-v1|')
    expect(message).toContain(`wallet=${vectors[0].wallet}`)
    expect(message).toContain(`pairId=${vectors[0].pairId}`)
    expect(message).toContain(`hash=${vectors[0].commitmentHash}`)
    expect(message).toContain(`stake=${vectors[0].stakeAmount}`)
    expect(message).toContain('signedAt=1730000000')
    expect(message).toContain('nonce=00112233445566778899aabbccddeeff')
  })

  it('encrypts reveal payload with signature-derived key and decrypts with matching aad', async () => {
    const nonce = nonceFromHex(vectors[0].nonceHex)
    const authMessage = buildCommitAuthMessage({
      wallet: vectors[0].wallet,
      pairId: vectors[0].pairId,
      commitmentHash: vectors[0].commitmentHash,
      stakeAmount: vectors[0].stakeAmount,
      signedAtEpochSeconds: 1730000000,
      requestNonceHex: '00112233445566778899aabbccddeeff',
    })
    const signature = new Uint8Array(64).fill(7)

    const { encryptedRevealBase64, revealIvBase64 } = await encryptRevealPayloadWithSignature({
      choice: 'A',
      nonce,
      signature,
      authMessage,
    })

    expect(encryptedRevealBase64).not.toBe(encodeRevealPayloadBase64('A', nonce))

    const key = await crypto.subtle.importKey(
      'raw',
      sha256(signature),
      { name: 'AES-GCM' },
      false,
      ['decrypt'],
    )
    const iv = Uint8Array.from(Buffer.from(revealIvBase64, 'base64').values())
    const ciphertext = Uint8Array.from(Buffer.from(encryptedRevealBase64, 'base64').values())
    const decrypted = await crypto.subtle.decrypt(
      { name: 'AES-GCM', iv, additionalData: new TextEncoder().encode(authMessage) },
      key,
      ciphertext,
    )

    expect(new Uint8Array(decrypted)).toEqual(buildRevealPayload('A', nonce))
  })

  it('generates request nonce hex with expected size', () => {
    const nonce = generateCommitRequestNonce()
    const hex = commitRequestNonceHex(nonce)
    expect(hex).toMatch(/^[0-9a-f]{32}$/)
  })
})
