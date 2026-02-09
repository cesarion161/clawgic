import { Connection, Transaction, TransactionInstruction } from '@solana/web3.js'
import { WalletContextState } from '@solana/wallet-adapter-react'

/**
 * Signs and sends a transaction using the connected wallet
 */
export async function signAndSendTransaction(
  connection: Connection,
  wallet: WalletContextState,
  instructions: TransactionInstruction[]
): Promise<string> {
  if (!wallet.publicKey || !wallet.signTransaction) {
    throw new Error('Wallet not connected')
  }

  const transaction = new Transaction().add(...instructions)
  transaction.recentBlockhash = (await connection.getLatestBlockhash()).blockhash
  transaction.feePayer = wallet.publicKey

  const signed = await wallet.signTransaction(transaction)
  const signature = await connection.sendRawTransaction(signed.serialize())

  await connection.confirmTransaction(signature)

  return signature
}

/**
 * Signs a message using the connected wallet
 */
export async function signMessage(
  wallet: WalletContextState,
  message: string
): Promise<Uint8Array> {
  if (!wallet.signMessage) {
    throw new Error('Wallet does not support message signing')
  }

  const encodedMessage = new TextEncoder().encode(message)
  return await wallet.signMessage(encodedMessage)
}

/**
 * Gets the Solana network endpoint from environment
 */
export function getSolanaEndpoint(): string {
  return process.env.NEXT_PUBLIC_SOLANA_RPC_URL || 'https://api.devnet.solana.com'
}

/**
 * Gets the Solana network name from environment
 */
export function getSolanaNetwork(): string {
  return process.env.NEXT_PUBLIC_SOLANA_NETWORK || 'devnet'
}
