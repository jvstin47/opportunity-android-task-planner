import type { NextFunction, Request, Response } from "express";

/** Wraps async Express route handlers so thrown/rejected errors reach [errorHandler]. */
export function asyncHandler(
  fn: (req: Request, res: Response, next: NextFunction) => Promise<void>
) {
  return (req: Request, res: Response, next: NextFunction) => {
    Promise.resolve(fn(req, res, next)).catch(next);
  };
}
