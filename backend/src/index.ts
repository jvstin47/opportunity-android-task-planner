import express from "express";
import { authRouter } from "./routes/auth.js";
import { syncRouter } from "./routes/sync.js";

const app = express();
app.use(express.json());

app.get("/health", (_req, res) => res.json({ ok: true }));
app.use("/auth", authRouter);
app.use("/sync", syncRouter);

const port = Number(process.env.PORT ?? 4000);
app.listen(port, () => {
  console.log(`Opportunity sync service listening on :${port}`);
});
