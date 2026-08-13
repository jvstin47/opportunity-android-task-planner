-- CreateTable
CREATE TABLE "User" (
    "id" TEXT NOT NULL,
    "email" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "User_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Opportunity" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "clientId" TEXT NOT NULL,
    "title" TEXT NOT NULL,
    "description" TEXT NOT NULL DEFAULT '',
    "type" TEXT NOT NULL,
    "status" TEXT NOT NULL,
    "priorityBucket" TEXT,
    "userPriority" INTEGER,
    "importance" INTEGER NOT NULL DEFAULT 0,
    "urgency" INTEGER NOT NULL DEFAULT 0,
    "deadlineEpochMillis" BIGINT,
    "preferredTimeMinuteOfDay" INTEGER,
    "estimatedDurationMinutes" INTEGER,
    "actualDurationMinutes" INTEGER NOT NULL DEFAULT 0,
    "timerMode" TEXT NOT NULL DEFAULT 'COUNTDOWN',
    "sessionPlanJson" TEXT,
    "reminderJson" TEXT,
    "recurrenceJson" TEXT,
    "parentProjectId" TEXT,
    "goalId" TEXT,
    "category" TEXT,
    "locationHint" TEXT,
    "completionPercent" INTEGER NOT NULL DEFAULT 0,
    "postponementCount" INTEGER NOT NULL DEFAULT 0,
    "lastPostponedEpochMillis" BIGINT,
    "sortOrderInParent" INTEGER NOT NULL DEFAULT 0,
    "createdAtEpochMillis" BIGINT NOT NULL,
    "updatedAtEpochMillis" BIGINT NOT NULL,
    "completedAtEpochMillis" BIGINT,
    "deletedLocally" BOOLEAN NOT NULL DEFAULT false,

    CONSTRAINT "Opportunity_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "User_email_key" ON "User"("email");

-- CreateIndex
CREATE INDEX "Opportunity_userId_updatedAtEpochMillis_idx" ON "Opportunity"("userId", "updatedAtEpochMillis");

-- CreateIndex
CREATE UNIQUE INDEX "Opportunity_userId_clientId_key" ON "Opportunity"("userId", "clientId");

-- AddForeignKey
ALTER TABLE "Opportunity" ADD CONSTRAINT "Opportunity_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
