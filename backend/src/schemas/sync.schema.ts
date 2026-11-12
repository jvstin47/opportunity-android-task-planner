import { z } from "zod";

export const opportunityPayloadSchema = z.object({
  clientId: z.string().min(1),
  title: z.string().min(1),
  description: z.string().default(""),
  type: z.string().min(1),
  status: z.string().min(1),
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

export const pushRequestSchema = z.object({
  opportunities: z.array(opportunityPayloadSchema).max(500)
});

export const pullQuerySchema = z.object({
  since: z.coerce.number().finite().nonnegative().default(0)
});

export type OpportunityPayload = z.infer<typeof opportunityPayloadSchema>;
