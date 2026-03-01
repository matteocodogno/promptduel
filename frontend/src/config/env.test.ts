import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'

describe('env config', () => {
  beforeEach(() => {
    vi.resetModules()
  })

  afterEach(() => {
    vi.unstubAllEnvs()
  })

  it('exposes apiBaseUrl and wsUrl when both env vars are set', async () => {
    vi.stubEnv('VITE_API_BASE_URL', 'http://localhost:8080')
    vi.stubEnv('VITE_WS_URL', 'http://localhost:8080/ws')

    const { env } = await import('./env')
    expect(env.apiBaseUrl).toBe('http://localhost:8080')
    expect(env.wsUrl).toBe('http://localhost:8080/ws')
  })

  it('throws when VITE_API_BASE_URL is missing', async () => {
    vi.stubEnv('VITE_API_BASE_URL', '')
    vi.stubEnv('VITE_WS_URL', 'http://localhost:8080/ws')

    await expect(import('./env')).rejects.toThrow('VITE_API_BASE_URL')
  })

  it('throws when VITE_WS_URL is missing', async () => {
    vi.stubEnv('VITE_API_BASE_URL', 'http://localhost:8080')
    vi.stubEnv('VITE_WS_URL', '')

    await expect(import('./env')).rejects.toThrow('VITE_WS_URL')
  })
})
