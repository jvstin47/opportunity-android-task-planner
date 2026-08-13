import { Router } from "express";
import { z } from "zod";
import { prisma } from "../db/prisma.js";
import { requireAuth, type AuthedRequest } from "../middleware/auth.js";

export const syncRouter = Router();
syncRouter.use(requireAuth);

const opportunityPayload = z.object({
  clientId: z.string(),
  title: z.string(),
  description: z.string().default(""),
  type: z.string(),
  status: z.string(),
  priorityBucket: z.string().nullable().optional(),
  userPriority: z.number().int().nullable().optional(),
  importance: z.number().int().default(0),
  urgency: z.number().int().default(0),
  deadlineEpochMillis: z.number().nullable().optional(),
  preferredTimeMinuteOfDay: z.number().int().nullable().optional(),
  estimatedDurationMinutes: z.number().int().nullable().optional(),
  actualDurationMinutes: z.number().int().default(0),
  timerMode: z.string().default("COUNTDOWN"),
  sessionPlanJson: z.string().nullable().optional(),
  reminderJson: z.string().nullable().optional(),
  recurrenceJson: z.string().nullable().optional(),
  parentProjectId: z.string().nullable().optional(),
  goalId: z.string().nullable().optional(),
  category: z.string().nullable().optional(),
  locationHint: z.string().nullable().optional(),
  completionPercent: z.number().int().default(0),
  postponementCount: z.number().int().default(0),
  lastPostponedEpochMillis: z.number().nullable().optional(),
  sortOrderInParent: z.number().int().default(0),
  createdAtEpochMillis: z.number(),
  updatedAtEpochMillis: z.number(),
  completedAtEpochMillis: z.number().nullable().optional(),
  deletedLocally: z.boolean().default(false)
});

const pushSchema = z.object({ opportunities: z.array(opportunityPayload) });

/**
 * Section 21/23: sync is push-then-pull, last-write-wins on updatedAtEpochMillis. This is
 * intentionally simple -- the app is fully usable offline against Room, so sync only needs to
 * reconcile occasional multi-device edits, not support real-time collaboration.
 */
syncRouter.post("/push", async (req: AuthedRequest, res) => {
  const parsed = pushSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }
  const userId = req.userId!;
  const results: Record<string, string> = {};

  for (const item of parsed.data.opportunities) {
    const existing = await prisma.opportunity.findUnique({
      where: { userId_clientId: { userId, clientId: item.clientId } }
    });

    if (existing && existing.updatedAtEpochMillis > BigInt(item.updatedAtEpochMillis)) {
      // Server has a newer edit -- client will pick it up on the next pull.
      continue;
    }

    const saved = await prisma.opportunity.upsert({
      where: { userId_clientId: { userId, clientId: item.clientId } },
      update: toRow(item),
      create: { userId, clientId: item.clientId, ...toRow(item) }
    });
    results[item.clientId] = saved.id;
  }

  res.json({ synced: results });
});

syncRouter.get("/pull", async (req: AuthedRequest, res) => {
  const since = Number(req.query.since ?? 0);
  const rows = await prisma.opportunity.findMany({
    where: { userId: req.userId!, updatedAtEpochMillis: { gt: BigInt(since) } },
    orderBy: { updatedAtEpochMillis: "asc" }
  });

  res.json({
    serverTimeEpochMillis: Date.now(),
    opportunities: rows.map((row) => ({
      ...row,
      deadlineEpochMillis: row.deadlineEpochMillis ? Number(row.deadlineEpochMillis) : null,
      lastPostponedEpochMillis: row.lastPostponedEpochMillis ? Number(row.lastPostponedEpochMillis) : null,
      createdAtEpochMillis: Number(row.createdAtEpochMillis),
      updatedAtEpochMillis: Number(row.updatedAtEpochMillis),
      completedAtEpochMillis: row.completedAtEpochMillis ? Number(row.completedAtEpochMillis) : null
    }))
  });
});

function toRow(item: z.infer<typeof opportunityPayload>) {
  return {
    title: item.title,
    description: item.description,
    type: item.type,
    status: item.status,
    priorityBucket: item.priorityBucket ?? null,
    userPriority: item.userPriority ?? null,
    importance: item.importance,
    urgency: item.urgency,
    deadlineEpochMillis: item.deadlineEpochMillis != null ? BigInt(item.deadlineEpochMillis) : null,
    preferredTimeMinuteOfDay: item.preferredTimeMinuteOfDay ?? null,
    estimatedDurationMinutes: item.estimatedDurationMinutes ?? null,
    actualDurationMinutes: item.actualDurationMinutes,
    timerMode: item.timerMode,
    sessionPlanJson: item.sessionPlanJson ?? null,
    reminderJson: item.reminderJson ?? null,
    recurrenceJson: item.recurrenceJson ?? null,
    parentProjectId: item.parentProjectId ?? null,
    goalId: item.goalId ?? null,
    category: item.category ?? null,
    locationHint: item.locationHint ?? null,
    completionPercent: item.completionPercent,
    postponementCount: item.postponementCount,
    lastPostponedEpochMillis: item.lastPostponedEpochMillis != null ? BigInt(item.lastPostponedEpochMillis) : null,
    sortOrderInParent: item.sortOrderInParent,
    createdAtEpochMillis: BigInt(item.createdAtEpochMillis),
    updatedAtEpochMillis: BigInt(item.updatedAtEpochMillis),
    completedAtEpochMillis: item.completedAtEpochMillis != null ? BigInt(item.completedAtEpochMillis) : null,
    deletedLocally: item.deletedLocally
  };
}
