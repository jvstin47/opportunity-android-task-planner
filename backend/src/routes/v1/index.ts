import { Router } from "express";
import { authRouter } from "./auth.routes.js";
import { syncRouter } from "./sync.routes.js";

export const v1Router = Router();

v1Router.use("/auth", authRouter);
v1Router.use("/sync", syncRouter);
