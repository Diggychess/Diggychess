const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);
const tsNow = () => new Date(Date.now() - 2000);

function validProfile(uid) {
  return {
    userId: uid,
    displayName: "Alice",
    avatarUrl: "https://res.cloudinary.com/demo/image/upload/sample.jpg",
    preferredDifficulty: "medium",
    gamesPlayedBot: 1,
    gamesAnalysed: 2,
    puzzlesSolved: 3,
    winStreak: 1,
    createdAt: tsNow(),
    updatedAt: tsNow(),
  };
}

function validGame(uid) {
  return {
    userId: uid,
    title: "Alice vs KnightScope",
    timestampMs: 1700000000000,
    plies: 4,
    moves: 2,
    pgn: "1. e4 e5 2. Nf3 Nc6 *",
    evals: [0.2, 0.1, 0.3, 0.2],
    bestMoves: ["", "c5", "", "Nf6"],
    whiteCounts: [1, 1, 0, 0, 0, 0],
    blackCounts: [1, 1, 0, 0, 0, 0],
    whitePhaseMistakes: [0, 0, 0],
    blackPhaseMistakes: [0, 0, 0],
    mistakes: [],
    createdAt: tsNow(),
    updatedAt: tsNow(),
  };
}

function validProgress(uid) {
  return {
    userId: uid,
    rating: 1245.5,
    ratedCount: 4,
    dailyGoal: 5,
    ratingHistory: [{ t: 1700000000000, r: 1246 }],
    seenIds: ["ks-01", "ks-02"],
    recentAttempts: [{ id: "ks-01", ts: 1700000000000, ok: true, r: 1200 }],
    createdAt: tsNow(),
    updatedAt: tsNow(),
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated user cannot read or write users, games, or progress", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("games").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("progress").doc("puzzles").get());
});

test("Authenticated owner can create, read, and update UserProfile; shadow field and cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).update({
    displayName: "Grandmaster Alice",
    gamesPlayedBot: 2,
    updatedAt: tsNow(),
  }));

  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({
    isAdmin: true,
    updatedAt: tsNow(),
  }));

  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({
    preferredDifficulty: "impossible",
    updatedAt: tsNow(),
  }));

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hacked", updatedAt: tsNow() }));
  await assertFails(aliceDb.collection("users").get());
});

test("Authenticated owner can create, list, and delete CloudSavedGame; cross-user fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("games").doc("g123-4").set(validGame(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("games").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("games").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("games").doc("g123-4").delete());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("games").doc("g123-4").delete());
});

test("Authenticated owner can write and update PuzzleProgress; invalid dailyGoal fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("progress").doc("puzzles").set(validProgress(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("progress").doc("puzzles").update({
    rating: 1300,
    ratedCount: 5,
    updatedAt: tsNow(),
  }));
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).collection("progress").doc("puzzles").update({
    dailyGoal: 999,
    updatedAt: tsNow(),
  }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("progress").doc("puzzles").get());
});
