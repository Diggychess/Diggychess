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
    email: "alice@example.com",
    preferredDifficulty: "medium",
    rating: 1320.5,
    ratedCount: 12,
    streak: 4,
    bestStreak: 9,
    winStreak: 2,
    puzzlesSolved: 25,
    dailyGoal: 5,
    dailySolvedToday: 3,
    dailySolvedDate: "2026-11-22",
    levelProgress: { "0": 10, "1": 6 },
    gamesPlayedBot: 3,
    gamesAnalysed: 2,
    ratingHistory: [{ t: 1700000000000, r: 1321 }],
    createdAt: tsNow(),
    updatedAt: tsNow(),
  };
}

function validFinishedGame(uid) {
  return {
    userId: uid,
    title: "Alice vs Stockfish (Medium)",
    opponent: "Stockfish (Medium)",
    result: "1-0",
    source: "bot",
    timestampMs: 1700000000000,
    plies: 4,
    moves: 2,
    pgn: "[White \"Alice\"]\n[Black \"Stockfish (Medium)\"]\n[Result \"1-0\"]\n\n1. e4 e5 2. Qh5 Nc6 3. Bc4 Nf6 4. Qxf7# 1-0",
    evals: [],
    bestMoves: [],
    whiteCounts: [],
    blackCounts: [],
    whitePhaseMistakes: [],
    blackPhaseMistakes: [],
    mistakes: [],
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

test("Unauthenticated user cannot read or write users or games", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("games").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("games").doc("play_1").set(validFinishedGame(ALICE_UID))
  );
});

test("Authenticated owner can create, read, and update their single UserProfile document; cross-user and invalid payloads fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      rating: 1355.0,
      streak: 5,
      bestStreak: 9,
      puzzlesSolved: 26,
      levelProgress: { "0": 10, "1": 7 },
      updatedAt: tsNow(),
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: tsNow(),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: tsNow(),
      updatedAt: tsNow(),
    })
  );

  // Reject out-of-range rating or future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      rating: 9999,
      updatedAt: tsNow(),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      streak: 6,
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Cross-user access and collection listing rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ streak: 0, updatedAt: tsNow() }));
  await assertFails(aliceDb.collection("users").get());
});

test("Authenticated owner can save finished game PGN, list ordered history, and delete; cross-user fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("games").doc("play_1700000000000").set(validFinishedGame(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("games").orderBy("timestampMs", "desc").limit(50).get()
  );

  // Invalid result rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("games").doc("play_bad").set({
      ...validFinishedGame(ALICE_UID),
      result: "invalid",
    })
  );

  // Cross-user denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("games").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("games").doc("play_1700000000000").delete());

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("games").doc("play_1700000000000").delete());
});

test("Authenticated owner can sync analysed game with accuracy and 7-category move counts; invalid accuracy rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const gameRef = aliceDb.collection("users").doc(ALICE_UID).collection("games").doc("g123abc-7");

  const analysedPayload = {
    ...validFinishedGame(ALICE_UID),
    source: "analysed",
    evals: [0.3, 0.25, 0.4, 0.5],
    bestMoves: ["", "e5", "Nf3", ""],
    whiteCounts: [0, 2, 0, 0, 0, 0, 0],
    blackCounts: [0, 1, 0, 0, 1, 0, 0],
    whitePhaseMistakes: [0, 0, 0],
    blackPhaseMistakes: [1, 0, 0],
    whiteAccuracy: 94.6,
    blackAccuracy: 71.2,
  };

  await assertSucceeds(gameRef.set(analysedPayload));
  await assertSucceeds(
    gameRef.update({
      whiteAccuracy: 95.1,
      blackAccuracy: 72.0,
      whiteCounts: [0, 2, 0, 0, 0, 0, 1],
      updatedAt: tsNow(),
    })
  );

  // Reject out-of-range accuracy (> 100 or < 0)
  await assertFails(
    gameRef.update({
      whiteAccuracy: 150.0,
      updatedAt: tsNow(),
    })
  );
  await assertFails(
    gameRef.update({
      blackAccuracy: -5.0,
      updatedAt: tsNow(),
    })
  );
});

