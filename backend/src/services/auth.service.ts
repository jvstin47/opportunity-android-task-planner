import jwt from "jsonwebtoken";
import { env } from "../config/env.js";
import { prisma } from "../db/prisma.js";

/** Issues a signed JWT for the given email, creating the user record on first login. */
export async function devLogin(email: string) {
  const user = await prisma.user.upsert({
    where: { email },
    update: {},
    create: { email }
  });

  const token = jwt.sign({ sub: user.id }, env.JWT_SECRET, {
    expiresIn: env.JWT_EXPIRES_IN as jwt.SignOptions["expiresIn"]
  });

  return { token, userId: user.id };
}
