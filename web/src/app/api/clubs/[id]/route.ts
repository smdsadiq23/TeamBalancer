import { NextResponse } from "next/server";
import { z } from "zod";
import { getAuthedUserId } from "@/lib/auth";
import { prisma } from "@/lib/prisma";
import type { Prisma } from "@prisma/client";

type Params = { params: Promise<{ id: string }> };

async function clubForUser(clubId: number, userId: number) {
  return prisma.club.findFirst({
    where: { id: clubId, userId },
    include: {
      players: {
        orderBy: { name: "asc" },
      },
    },
  });
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

  const club = await clubForUser(clubId, userId);
  if (!club) {
    return NextResponse.json({ error: "Not found" }, { status: 404 });
  }

  return NextResponse.json({ club });
}

const patchSchema = z.object({
  name: z.string().min(1).max(128).trim().optional(),
  teamNames: z.array(z.string()).optional(),
  history: z.array(z.unknown()).optional(),
});

export async function PATCH(request: Request, { params }: Params) {
  const userId = await getAuthedUserId(request);
  if (userId == null) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const { id: idStr } = await params;
  const clubId = Number.parseInt(idStr, 10);
  if (!Number.isFinite(clubId)) {
    return NextResponse.json({ error: "Invalid club id" }, { status: 400 });
  }

  const existing = await clubForUser(clubId, userId);
  if (!existing) {
    return NextResponse.json({ error: "Not found" }, { status: 404 });
  }

  let raw: unknown;
  try {
    raw = await request.json();
  } catch {
    return NextResponse.json({ error: "Invalid JSON body" }, { status: 400 });
  }

  const parsed = patchSchema.safeParse(raw);
  if (!parsed.success) {
    return NextResponse.json({ error: "Invalid input", detail: parsed.error.flatten() }, { status: 400 });
  }

  const data: Prisma.ClubUpdateInput = {};
  if (parsed.data.name !== undefined) {
    data.name = parsed.data.name;
  }
  if (parsed.data.teamNames !== undefined) {
    data.teamNames = parsed.data.teamNames as Prisma.InputJsonValue;
  }
  if (parsed.data.history !== undefined) {
    data.history = parsed.data.history as Prisma.InputJsonValue;
  }

  await prisma.club.update({
    where: { id: clubId },
    data,
  });

  const club = await clubForUser(clubId, userId);
  return NextResponse.json({ club });
}

export async function DELETE(request: Request, { params }: Params) {
  const userId = await getAuthedUserId(request);
  if (userId == null) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const { id: idStr } = await params;
  const clubId = Number.parseInt(idStr, 10);
  if (!Number.isFinite(clubId)) {
    return NextResponse.json({ error: "Invalid club id" }, { status: 400 });
  }

  const deleted = await prisma.club.deleteMany({
    where: { id: clubId, userId },
  });

  if (deleted.count === 0) {
    return NextResponse.json({ error: "Not found" }, { status: 404 });
  }

  return NextResponse.json({ ok: true });
}
