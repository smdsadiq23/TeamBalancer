import { NextResponse } from "next/server";
import { z } from "zod";
import { getAuthedUserId } from "@/lib/auth";
import { prisma } from "@/lib/prisma";

type Params = { params: Promise<{ id: string }> };

const STYLE = ["BATSMAN", "BOWLER", "ALL_ROUNDER"] as const;
const CATEGORY = ["REGULAR", "PROMISING_TALENT", "VETERAN"] as const;

const playerBody = z.object({
  name: z.string().min(1).max(128).trim(),
  style: z.enum(STYLE),
  category: z.enum(CATEGORY),
  battingRating: z.number().int().min(1).max(10).optional(),
  bowlingRating: z.number().int().min(1).max(10).optional(),
  fieldingRating: z.number().int().min(1).max(10).optional(),
  isCaptain: z.boolean().optional(),
  isAvailable: z.boolean().optional(),
});

async function ownsClub(clubId: number, userId: number): Promise<boolean> {
  const c = await prisma.club.findFirst({
    where: { id: clubId, userId },
    select: { id: true },
  });
  return c != null;
}

export async function GET(request: Request, { params }: Params) {
  const userId = await getAuthedUserId(request);
  if (userId == null) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const { id: idStr } = await params;
  const clubId = Number.parseInt(idStr, 10);
  if (!Number.isFinite(clubId)) {
    return NextResponse.json({ error: "Invalid club id" }, { status: 400 });
  }

  if (!(await ownsClub(clubId, userId))) {
    return NextResponse.json({ error: "Not found" }, { status: 404 });
  }

  const players = await prisma.player.findMany({
    where: { clubId },
    orderBy: { name: "asc" },
  });
  return NextResponse.json({ players });
}

export async function POST(request: Request, { params }: Params) {
  const userId = await getAuthedUserId(request);
  if (userId == null) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const { id: idStr } = await params;
  const clubId = Number.parseInt(idStr, 10);
  if (!Number.isFinite(clubId)) {
    return NextResponse.json({ error: "Invalid club id" }, { status: 400 });
  }

  if (!(await ownsClub(clubId, userId))) {
    return NextResponse.json({ error: "Not found" }, { status: 404 });
  }

  let raw: unknown;
  try {
    raw = await request.json();
  } catch {
    return NextResponse.json({ error: "Invalid JSON body" }, { status: 400 });
  }

  const parsed = playerBody.safeParse(raw);
  if (!parsed.success) {
    return NextResponse.json({ error: "Invalid input", detail: parsed.error.flatten() }, { status: 400 });
  }

  const p = parsed.data;
  const player = await prisma.player.create({
    data: {
      clubId,
      name: p.name,
      style: p.style,
      category: p.category,
      battingRating: p.battingRating ?? 1,
      bowlingRating: p.bowlingRating ?? 1,
      fieldingRating: p.fieldingRating ?? 1,
      isCaptain: p.isCaptain ?? false,
      isAvailable: p.isAvailable ?? true,
    },
  });

  return NextResponse.json({ player });
}
