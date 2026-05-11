import { NextResponse } from "next/server";
import bcrypt from "bcryptjs";
import { z } from "zod";
import { prisma } from "@/lib/prisma";
import { setSessionCookie, signSession } from "@/lib/auth";
import { authRateLimitExceeded } from "@/lib/rate-limit";

const bodySchema = z.object({
  username: z.string().min(2).max(64).trim(),
  password: z.string().min(6).max(128),
});

export async function POST(request: Request) {
  if (authRateLimitExceeded(request)) {
    return NextResponse.json({ error: "Too many requests. Try again shortly." }, { status: 429 });
  }

  let raw: unknown;
  try {
    raw = await request.json();
  } catch {
    return NextResponse.json({ error: "Invalid JSON body" }, { status: 400 });
  }

  const parsed = bodySchema.safeParse(raw);
  if (!parsed.success) {
    return NextResponse.json({ error: "Invalid input", detail: parsed.error.flatten() }, { status: 400 });
  }

  const { username, password } = parsed.data;
  const existing = await prisma.user.findUnique({ where: { username } });
  if (existing) {
    return NextResponse.json({ error: "Username taken" }, { status: 409 });
  }

  const passwordHash = await bcrypt.hash(password, 11);
  const user = await prisma.user.create({
    data: { username, passwordHash },
    select: { id: true, username: true },
  });

  const token = await signSession(user.id, user.username);
  await setSessionCookie(token);

  return NextResponse.json({
    token,
    user: { id: user.id, username: user.username },
  });
}
