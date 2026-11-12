import type { Opportunity } from "@prisma/client";
import { prisma } from "../db/prisma.js";
import type { OpportunityPayload } from "../schemas/sync.schema.js";

/**
 * Reconciles incoming client items against persisted server state using last-write-wins
 * on `updatedAtEpochMillis`. Skips overwriting newer server edits so the next pull updates the client.
 */
export async function pushOpportunities(userId: string, opportunities: OpportunityPayload[]) {
  const synced: Record<string, string> = {};

  await prisma.$transaction(async (tx) => {
    for (const item of opportunities) {
      const existing = await tx.opportunity.findUnique({
        where: { userId_clientId: { userId, clientId: item.clientId } }
      });

      if (existing && existing.updatedAtEpochMillis > BigInt(item.updatedAtEpochMillis)) {
        continue;
      }

      const row = toPersistenceRow(item);
      const saved = await tx.opportunity.upsert({
        where: { userId_clientId: { userId, clientId: item.clientId } },
        update: row,
        create: { userId, clientId: item.clientId, ...row }
      });
      synced[item.clientId] = saved.id;
    }
  });

  return synced;
}

/** Returns all opportunities modified strictly after `sinceEpochMillis`. */
export async function pullOpportunities(userId: string, sinceEpochMillis: number) {
  const rows = await prisma.opportunity.findMany({
    where: { userId, updatedAtEpochMillis: { gt: BigInt(sinceEpochMillis) } },
    orderBy: { updatedAtEpochMillis: "asc" }
  });

  return {
    serverTimeEpochMillis: Date.now(),
    opportunities: rows.map(toDto)
  };
}

function toPersistenceRow(item: OpportunityPayload) {
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

function toDto(row: Opportunity) {
  return {
    ...row,
    deadlineEpochMillis: row.deadlineEpochMillis != null ? Number(row.deadlineEpochMillis) : null,
    lastPostponedEpochMillis: row.lastPostponedEpochMillis != null ? Number(row.lastPostponedEpochMillis) : null,
    createdAtEpochMillis: Number(row.createdAtEpochMillis),
    updatedAtEpochMillis: Number(row.updatedAtEpochMillis),
    completedAtEpochMillis: row.completedAtEpochMillis != null ? Number(row.completedAtEpochMillis) : null
  };
}
