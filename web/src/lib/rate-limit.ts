/**
 * Lightweight in-memory sliding-window limiter per key.
 * In serverless, each isolate has its own map; production should use Redis/Upstash.
 */
const buckets = new Map<string, number[]>();

export function consumeRateLimit(key: string, max: number, windowMs: number): boolean {
  const now = Date.now();
  let arr = buckets.get(key);
  if (!arr) {
    arr = [];
    buckets.set(key, arr);
  }
  const cutoff = now - windowMs;
  while (arr.length > 0 && arr[0]! < cutoff) {
    arr.shift();
  }
  if (arr.length >= max) {
    return false;
  }
  arr.push(now);
  return true;
}

export function getClientIp(request: Request): string {
  const xff = request.headers.get("x-forwarded-for");
  if (xff) {
    return xff.split(",")[0]!.trim() || "unknown";
  }
  const xr = request.headers.get("x-real-ip");
  if (xr) {
    return xr.trim();
  }
  return "unknown";
}

/** Too many attempts when true → respond 429 */
export function authRateLimitExceeded(request: Request): boolean {
  const ip = getClientIp(request);
  return !consumeRateLimit(`auth:${ip}`, 40, 15 * 60 * 1000);
}
