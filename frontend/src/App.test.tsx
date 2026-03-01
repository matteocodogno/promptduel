import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import App from './App'

vi.mock('./config/env', () => ({
  env: {
    apiBaseUrl: 'http://localhost:8080',
    wsUrl: 'http://localhost:8080/ws',
  },
}))

describe('App', () => {
  it('renders without crashing', () => {
    render(<App />)
    expect(document.body).toBeInTheDocument()
  })

  it('renders the lobby route by default', () => {
    render(<App />)
    expect(screen.getByRole('main')).toBeInTheDocument()
  })
})
