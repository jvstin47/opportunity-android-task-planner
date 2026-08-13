import { Router } from "express";
import jwt from "jsonwebtoken";
import { z } from "zod";
import { prisma } from "../db/prisma.js";

export const authRouter = Router();

const devLoginSchema = z.object({ email: z.string().email() });

/**
 * Dev-only auth stub: exchanges an email for a JWT with no password/OAuth check. This is
 * intentionally NOT production auth -- section 20 leaves backend auth unspecified, and a real
 * deployment should replace this with Sign in with Google / OAuth before going further than a
 * local sync prototype.
 */
authRouter.post("/dev-login", async (req, res) => {
  const parsed = devLoginSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }
  const JWT_SECRET = process.env.JWT_SECRET;
  if (!JWT_SECRET) {
    res.status(500).json({ error: "Server misconfigured: JWT_SECRET not set" });
    return;
  }

  const user = await prisma.user.upsert({
    where: { email: parsed.data.email },
    update: {},
    create: { email: parsed.data.email }
  });

  const token = jwt.sign({ sub: user.id }, JWT_SECRET, { expiresIn: "30d" });
  res.json({ token, userId: user.id });
});
