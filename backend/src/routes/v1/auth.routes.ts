import { Router } from "express";
import { asyncHandler } from "../../middleware/asyncHandler.js";
import { devLoginSchema } from "../../schemas/auth.schema.js";
import { devLogin } from "../../services/auth.service.js";

export const authRouter = Router();

/**
 * POST /dev-login
 * Exchanges an email for a signed JWT (dev/local-sync flow).
 */
authRouter.post(
  "/dev-login",
  asyncHandler(async (req, res) => {
    const { email } = devLoginSchema.parse(req.body);
    const session = await devLogin(email);
    res.json(session);
  })
);
