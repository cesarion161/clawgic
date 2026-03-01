import { describe, expect, it, vi } from 'vitest'
import ClawgicSectionStubPage from './page'

const notFoundMock = vi.fn(() => {
  // Next.js notFound() throws to halt rendering; simulate that behavior.
  throw new Error('NEXT_NOT_FOUND')
})

vi.mock('next/navigation', () => ({
  notFound: (...args: unknown[]) => notFoundMock(...args),
}))

describe('Clawgic [section] stub page', () => {
  it('renders matches stub content', async () => {
    const result = await ClawgicSectionStubPage({
      params: Promise.resolve({ section: 'matches' }),
    })

    expect(result).toBeTruthy()
    expect(notFoundMock).not.toHaveBeenCalled()
  })

  it('calls notFound for unknown sections', async () => {
    notFoundMock.mockClear()

    await expect(
      ClawgicSectionStubPage({ params: Promise.resolve({ section: 'nonexistent' }) })
    ).rejects.toThrow('NEXT_NOT_FOUND')
    expect(notFoundMock).toHaveBeenCalled()
  })

  it('calls notFound for agents since it now has a concrete route', async () => {
    notFoundMock.mockClear()

    await expect(
      ClawgicSectionStubPage({ params: Promise.resolve({ section: 'agents' }) })
    ).rejects.toThrow('NEXT_NOT_FOUND')
    expect(notFoundMock).toHaveBeenCalled()
  })

  it('calls notFound for tournaments since it has a concrete route', async () => {
    notFoundMock.mockClear()

    await expect(
      ClawgicSectionStubPage({ params: Promise.resolve({ section: 'tournaments' }) })
    ).rejects.toThrow('NEXT_NOT_FOUND')
    expect(notFoundMock).toHaveBeenCalled()
  })

  it('calls notFound for results since it has a concrete route', async () => {
    notFoundMock.mockClear()

    await expect(
      ClawgicSectionStubPage({ params: Promise.resolve({ section: 'results' }) })
    ).rejects.toThrow('NEXT_NOT_FOUND')
    expect(notFoundMock).toHaveBeenCalled()
  })
})
