import type { HttpClient, RestResponse } from '../generated/api'

export class FetchHttpClient implements HttpClient {
  request<R>(requestConfig: {
    method: string
    url: string
    queryParams?: Record<string, unknown>
    data?: unknown
    copyFn?: (data: R) => R
  }): RestResponse<R> {
    const url = new URL('/' + requestConfig.url, window.location.origin)

    if (requestConfig.queryParams) {
      for (const [key, value] of Object.entries(requestConfig.queryParams)) {
        if (value !== undefined && value !== null) {
          url.searchParams.set(key, String(value))
        }
      }
    }

    return fetch(url.toString(), {
      method: requestConfig.method,
      ...(requestConfig.data && {
        body: JSON.stringify(requestConfig.data),
        headers: { 'Content-Type': 'application/json' },
      }),
    }).then(res => {
      if (!res.ok) throw new Error(`HTTP ${res.status} ${res.statusText}`)
      return res.json() as R
    })
  }
}