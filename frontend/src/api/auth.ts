import type { LoginCredentials, User } from "../types/auth";

/**
 * Calls POST /api/auth/login on the backend.
 * The Vite dev proxy rewrites /api/* → http://localhost:8080/*
 *
 * Backend endpoint needed (not yet implemented):
 *   POST /auth/login  { email, password }
 *   → 200 { employeeId, employeeNumber, firstName, lastName, email, roleId, roleName }
 *   → 401 { error: "Invalid email or password" }
 */
export async function login(credentials: LoginCredentials): Promise<User> {
  const res = await fetch("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(credentials),
  });

  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new Error(body?.error ?? "Login failed");
  }

  return res.json();
}
