import { NextResponse } from "next/server";
import { z } from "zod";
import { getAuthedUserId } from "@/lib/auth";
import { prisma } from "@/lib/prisma";

export async function GET(request: Request) {
  const userId = await getAuthedUserId(request);
  if (userId == null) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }
  const clubs = await prisma.club.findMany({
    where: { userId },
    orderBy: { name: "asc" },
    select: {
      id: true,
      name: true,
      updatedAt: true,
      _count: { select: { players: true } },
    },
  });
  return NextResponse.json({ clubs });
}

const postSchema = z.object({
  name: z.string().min(1).max(128).trim(),
});

export async function POST(request: Request) {
  const userId = await getAuthedUserId(request);
  if (userId == null) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  let raw: unknown;
  try {
    raw = await request.json();
  } catch {
    return NextResponse.json({ error: "Invalid JSON body" }, { status: 400 });
  }

  const parsed = postSchema.safeParse(raw);
  if (!parsed.success) {
    return NextResponse.json({ error: "Invalid input" }, { status: 400 });
  }

  const club = await prisma.club.create({
    data: {
      userId,
      name: parsed.data.name,
      teamNames: [],
      history: [],
    },
    select: { id: true, name: true },
  });

  return NextResponse.json({ club });
}
