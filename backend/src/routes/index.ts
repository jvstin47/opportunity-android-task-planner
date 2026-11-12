import { Router } from "express";
import { authRouter } from "./v1/auth.routes.js";
import { v1Router } from "./v1/index.js";
import { syncRouter } from "./v1/sync.routes.js";

export const apiRouter = Router();

apiRouter.get("/health", (_req, res) => {
  res.json({ ok: true, service: "opportunity-sync-service", apiVersion: "v1" });
});

// Scalable versioned prefix
apiRouter.use("/api/v1", v1Router);

// Backwards-compatible root aliases for existing mobile clients
apiRouter.use("/auth", authRouter);
apiRouter.use("/sync", syncRouter);
