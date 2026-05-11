import { NextResponse } from "next/server";
import { z } from "zod";
import { getAuthedUserId } from "@/lib/auth";
import { prisma } from "@/lib/prisma";

type Params = { params: Promise<{ id: string }> };

const STYLE = ["BATSMAN", "BOWLER", "ALL_ROUNDER"] as const;
const CATEGORY = ["REGULAR", "PROMISING_TALENT", "VETERAN"] as const;

const patchBody = z.object({
  name: z.string().min(1).max(128).trim().optional(),
  style: z.enum(STYLE).optional(),
  category: z.enum(CATEGORY).optional(),
  battingRating: z.number().int().min(1).max(10).optional(),
  bowlingRating: z.number().int().min(1).max(10).optional(),
  fieldingRating: z.number().int().min(1).max(10).optional(),
  isCaptain: z.boolean().optional(),
  isAvailable: z.boolean().optional(),
});

async function playerForUser(playerId: number, userId: number) {
  return prisma.player.findFirst({
    where: {
      id: playerId,
      club: { userId },
    },
    include: { club: { select: { id: true } } },
  });
}

export async function PATCH(request: Request, { params }: Params) {
  const userId = await getAuthedUserId(request);
  if (userId == null) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const { id: idStr } = await params;
  const playerId = Number.parseInt(idStr, 10);
  if (!Number.isFinite(playerId)) {
    return NextResponse.json({ error: "Invalid player id" }, { status: 400 });
  }

  const existing = await playerForUser(playerId, userId);
  if (!existing) {
    return NextResponse.json({ error: "Not found" }, { status: 404 });
  }

  let raw: unknown;
  try {
    raw = await request.json();
  } catch {
    return NextResponse.json({ error: "Invalid JSON body" }, { status: 400 });
  }

  const parsed = patchBody.safeParse(raw);
  if (!parsed.success) {
    return NextResponse.json({ error: "Invalid input", detail: parsed.error.flatten() }, { status: 400 });
  }

  const player = await prisma.player.update({
    where: { id: playerId },
    data: parsed.data,
  });

  return NextResponse.json({ player });
}

export async function DELETE(request: Request, { params }: Params) {
  const userId = await getAuthedUserId(request);
  if (userId == null) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const { id: idStr } = await params;
  const playerId = Number.parseInt(idStr, 10);
  if (!Number.isFinite(playerId)) {
    return NextResponse.json({ error: "Invalid player id" }, { status: 400 });
  }

  const deleted = await prisma.player.deleteMany({
    where: {
      id: playerId,
      club: { userId },
    },
  });

  if (deleted.count === 0) {
    return NextResponse.json({ error: "Not found" }, { status: 404 });
  }

  return NextResponse.json({ ok: true });
}
