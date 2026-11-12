import { z } from "zod";

export const devLoginSchema = z.object({
  email: z.string().trim().email("Valid email is required")
});

export type DevLoginInput = z.infer<typeof devLoginSchema>;
