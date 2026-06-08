import { useState, useEffect } from 'react'
import { RestApplicationClient } from './generated/api'
import type { HelloResponse } from './generated/api'
import { FetchHttpClient } from './api/client'
import './App.css'

const client = new RestApplicationClient(new FetchHttpClient())

function App() {
  const [response, setResponse] = useState<HelloResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [name, setName] = useState('World')

  const fetchGreeting = (nameToGreet: string) => {
    setLoading(true)
    setError(null)
    client
      .greet({ name: nameToGreet })
      .then(setResponse)
      .catch(err => setError(err.message))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    fetchGreeting(name)
  }, [])

  return (
    <div className="app">
      <h1>Hello World</h1>

      <div className="input-row">
        <input
          value={name}
          onChange={e => setName(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && fetchGreeting(name)}
          placeholder="Enter your name"
        />
        <button onClick={() => fetchGreeting(name)}>Greet</button>
      </div>

      {loading && <p className="status">Loading…</p>}

      {error && <p className="error">{error}</p>}

      {response && !loading && (
        <div className="card">
          <p className="message">{response.message}</p>
          <p className="timestamp">
            Server time: {new Date(response.timestamp).toLocaleTimeString()}
          </p>
        </div>
      )}
    </div>
  )
}

export default App