import { SignJWT, jwtVerify } from "jose";
import { cookies } from "next/headers";

const COOKIE = "tb_session";
const TTL = "30d";

export type SessionPayload = { sub: string; username: string };

function secretKey(): Uint8Array {
  const s = process.env.AUTH_SECRET;
  if (!s || s.length < 32) {
    throw new Error("AUTH_SECRET must be set and at least 32 characters");
  }
  return new TextEncoder().encode(s);
}

export async function signSession(userId: number, username: string): Promise<string> {
  return new SignJWT({ username })
    .setProtectedHeader({ alg: "HS256" })
    .setSubject(String(userId))
    .setIssuedAt()
    .setExpirationTime(TTL)
    .sign(secretKey());
}

async function sessionFromJwtString(token: string): Promise<SessionPayload | null> {
  try {
    const trimmed = token.trim();
    if (!trimmed) {
      return null;
    }
    const { payload } = await jwtVerify(trimmed, secretKey(), {
      algorithms: ["HS256"],
    });
    const sub = payload.sub;
    const username = typeof payload.username === "string" ? payload.username : "";
    if (!sub) {
      return null;
    }
    return { sub, username };
  } catch {
    return null;
  }
}

function parseBearerToken(request: Request): string | null {
  const raw = request.headers.get("authorization");
  if (!raw) {
    return null;
  }
  const m = /^Bearer\s+(.+)$/i.exec(raw.trim());
  return m ? m[1]!.trim() : null;
}

export async function getSession(): Promise<SessionPayload | null> {
  try {
    const store = await cookies();
    const token = store.get(COOKIE)?.value;
    if (!token) return null;
    return sessionFromJwtString(token);
  } catch {
    return null;
  }
}

export async function setSessionCookie(token: string): Promise<void> {
  const store = await cookies();
  store.set(COOKIE, token, {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge: 60 * 60 * 24 * 30,
  });
}

export async function clearSessionCookie(): Promise<void> {
  const store = await cookies();
  store.set(COOKIE, "", { httpOnly: true, path: "/", maxAge: 0 });
}

/**
 * Resolve authenticated user id. If an `Authorization: Bearer` header is present, only that JWT
 * is accepted (invalid token => unauthenticated — no fallback to cookie). If absent, falls back
 * to the httpOnly session cookie (browser).
 */
export async function getAuthedUserId(request: Request): Promise<number | null> {
  const bearer = parseBearerToken(request);
  if (bearer != null) {
    const payload = await sessionFromJwtString(bearer);
    if (payload == null) {
      return null;
    }
    const id = Number.parseInt(payload.sub, 10);
    return Number.isFinite(id) ? id : null;
  }

  const fromCookie = await getSession();
  if (fromCookie == null) {
    return null;
  }
  const id = Number.parseInt(fromCookie.sub, 10);
  return Number.isFinite(id) ? id : null;
}
