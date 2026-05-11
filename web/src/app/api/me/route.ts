import { NextResponse } from "next/server";
import { getAuthedUserId } from "@/lib/auth";
import { prisma } from "@/lib/prisma";

export async function GET(request: Request) {
  const userId = await getAuthedUserId(request);
  if (userId == null) {
    return NextResponse.json({ user: null });
  }
  const user = await prisma.user.findUnique({
    where: { id: userId },
    select: { id: true, username: true },
  });
  if (!user) {
    return NextResponse.json({ user: null });
  }
  return NextResponse.json({ user });
}
