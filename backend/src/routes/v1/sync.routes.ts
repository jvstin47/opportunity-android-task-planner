import { Router } from "express";
import { asyncHandler } from "../../middleware/asyncHandler.js";
import { requireAuth, type AuthedRequest } from "../../middleware/auth.js";
import { pullQuerySchema, pushRequestSchema } from "../../schemas/sync.schema.js";
import { pullOpportunities, pushOpportunities } from "../../services/sync.service.js";

export const syncRouter = Router();
syncRouter.use(requireAuth);

syncRouter.post(
  "/push",
  asyncHandler(async (req: AuthedRequest, res) => {
    const { opportunities } = pushRequestSchema.parse(req.body);
    const synced = await pushOpportunities(req.userId!, opportunities);
    res.json({ synced });
  })
);

syncRouter.get(
  "/pull",
  asyncHandler(async (req: AuthedRequest, res) => {
    const { since } = pullQuerySchema.parse(req.query);
    const payload = await pullOpportunities(req.userId!, since);
    res.json(payload);
  })
);
