import { ApiError } from './types';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';
const TOKEN_KEY = 'shift_happens_token';

let tokenGetter: () => string | null = () => {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
};
let unauthorizedHandler: () => void = () => {};

function extractErrorMessage(payload: unknown): string | null {
  if (!payload) return null;
  if (typeof payload === 'string') {
    return payload.trim() || null;
  }

  if (typeof payload === 'object') {
    const candidate = payload as Record<string, unknown>;
    const directKeys = ['message', 'error', 'reason', 'detail', 'title'];
    for (const key of directKeys) {
      const value = candidate[key];
      if (typeof value === 'string' && value.trim()) {
        return value;
      }
    }

    const errors = candidate['errors'];
    if (Array.isArray(errors) && errors.length > 0) {
      const first = errors[0];
      if (typeof first === 'string' && first.trim()) return first;
      if (first && typeof first === 'object') {
        const firstObj = first as Record<string, unknown>;
        const firstMessage = firstObj['defaultMessage'] ?? firstObj['message'];
        if (typeof firstMessage === 'string' && firstMessage.trim()) {
          return firstMessage;
        }
      }
    }
  }

  return null;
}

export function configureApiClient(opts: {
  getToken: () => string | null;
  onUnauthorized: () => void;
}) {
  tokenGetter = opts.getToken;
  unauthorizedHandler = opts.onUnauthorized;
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PATCH' | 'PUT' | 'DELETE';
  body?: unknown;
  skipAuth?: boolean;
}

export async function apiRequest<T>(
  path: string,
  { method = 'GET', body, skipAuth = false }: RequestOptions = {}
): Promise<T> {
  const headers: Record<string, string> = {};

  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }

  if (!skipAuth) {
    const token = tokenGetter();
    if (token) headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (response.status === 401 && !skipAuth) {
    unauthorizedHandler();
  }

  const contentType = response.headers.get('content-type') ?? '';
  const payload = contentType.includes('application/json')
    ? await response.json().catch(() => null)
    : await response.text().catch(() => null);

  if (!response.ok) {
    const message =
      extractErrorMessage(payload) ??
      `${response.status} ${response.statusText || 'Request failed'}`;
    throw new ApiError(response.status, payload, message);
  }

  return payload as T;
}
