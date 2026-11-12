import type { NextFunction, Request, Response } from "express";
import jwt from "jsonwebtoken";
import { env } from "../config/env.js";
import { AppError } from "./errorHandler.js";

export interface AuthedRequest extends Request {
  userId?: string;
}

/** Verifies Bearer JWT tokens and attaches `req.userId` for downstream handlers. */
export function requireAuth(req: AuthedRequest, _res: Response, next: NextFunction) {
  const header = req.headers.authorization;
  if (!header?.startsWith("Bearer ")) {
    next(new AppError(401, "Missing bearer token"));
    return;
  }
  try {
    const token = header.slice("Bearer ".length).trim();
    const payload = jwt.verify(token, env.JWT_SECRET) as { sub?: string };
    if (!payload.sub) {
      next(new AppError(401, "Invalid token subject"));
      return;
    }
    req.userId = payload.sub;
    next();
  } catch {
    next(new AppError(401, "Invalid or expired token"));
  }
}
