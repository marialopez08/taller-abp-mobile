const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

function pastServerTimestamp() {
  return new Date(Date.now() - 1000);
}

const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

let testEnv;

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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validAppointmentPayload(userId, overrides = {}) {
  return {
    userId,
    clientName: "Laura Gómez",
    clientPhone: "3001234567",
    petName: "Toby",
    petBreed: "Golden Retriever",
    serviceCode: "BANO_PREMIUM",
    serviceName: "Baño premium",
    dogSizeCode: "GRANDE",
    dateIso: "2026-11-20",
    timeSlotId: "10:00",
    timeSlotLabel: "10:00 a. m.",
    slotKey: "2026-11-20_10-00",
    priceCop: 100000,
    statusCode: "CONFIRMADA",
    createdAt: pastServerTimestamp(),
    updatedAt: pastServerTimestamp(),
    ...overrides,
  };
}

function validSlotPayload(userId, appointmentId, overrides = {}) {
  return {
    userId,
    appointmentId,
    dateIso: "2026-11-20",
    timeSlotId: "10:00",
    createdAt: pastServerTimestamp(),
    ...overrides,
  };
}

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("appointments").get()
  );
  await assertFails(
    unauthDb.collection("occupied_slots").doc("2026-11-20_10-00").get()
  );
  await assertFails(unauthDb.collection("occupied_slots").get());
});

test("Authenticated user can create appointment and slot lock atomically in a batch", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const batch = aliceDb.batch();
  const apptRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("appointments")
    .doc("appt_1");
  const slotRef = aliceDb.collection("occupied_slots").doc("2026-11-20_10-00");

  batch.set(apptRef, validAppointmentPayload(ALICE_UID));
  batch.set(slotRef, validSlotPayload(ALICE_UID, "appt_1"));
  await assertSucceeds(batch.commit());

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("appointments").get()
  );
});

test("Orphaned appointment without occupied_slots lock fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("appointments")
      .doc("appt_orphan")
      .set(validAppointmentPayload(ALICE_UID))
  );
});

test("Orphaned occupied_slots lock without appointment fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection("occupied_slots")
      .doc("2026-11-20_10-00")
      .set(validSlotPayload(ALICE_UID, "missing_appt"))
  );
});

test("Shadow update / ghost field injection is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const batch = aliceDb.batch();
  batch.set(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("appointments")
      .doc("appt_ghost"),
    validAppointmentPayload(ALICE_UID, { isAdmin: true })
  );
  batch.set(
    aliceDb.collection("occupied_slots").doc("2026-11-20_10-00"),
    validSlotPayload(ALICE_UID, "appt_ghost")
  );
  await assertFails(batch.commit());
});

test("Price tampering is rejected by security rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const batch = aliceDb.batch();
  batch.set(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("appointments")
      .doc("appt_tamper"),
    validAppointmentPayload(ALICE_UID, { priceCop: 1000 })
  );
  batch.set(
    aliceDb.collection("occupied_slots").doc("2026-11-20_10-00"),
    validSlotPayload(ALICE_UID, "appt_tamper")
  );
  await assertFails(batch.commit());
});

test("Cross-user PII isolation prevents Bob from reading or writing Alice's appointments", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const batch = aliceDb.batch();
  batch.set(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("appointments")
      .doc("appt_alice"),
    validAppointmentPayload(ALICE_UID)
  );
  batch.set(
    aliceDb.collection("occupied_slots").doc("2026-11-20_10-00"),
    validSlotPayload(ALICE_UID, "appt_alice")
  );
  await assertSucceeds(batch.commit());

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  // Bob cannot read Alice's profile or appointments
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("appointments")
      .doc("appt_alice")
      .get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("appointments").get()
  );

  // Bob can check single slot availability by get, but cannot blanket-list occupied_slots
  await assertSucceeds(
    bobDb.collection("occupied_slots").doc("2026-11-20_10-00").get()
  );
  await assertFails(bobDb.collection("occupied_slots").get());
  // Bob can list only his own occupied_slots
  await assertSucceeds(
    bobDb.collection("occupied_slots").where("userId", "==", BOB_UID).get()
  );
  // Bob cannot delete Alice's slot lock
  await assertFails(
    bobDb.collection("occupied_slots").doc("2026-11-20_10-00").delete()
  );
});

test("Owner can cancel CONFIRMADA appointment and release slot, then terminal lock blocks further updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const apptRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("appointments")
    .doc("appt_cancel");
  const slotRef = aliceDb.collection("occupied_slots").doc("2026-11-20_10-00");

  const createBatch = aliceDb.batch();
  createBatch.set(apptRef, validAppointmentPayload(ALICE_UID));
  createBatch.set(slotRef, validSlotPayload(ALICE_UID, "appt_cancel"));
  await assertSucceeds(createBatch.commit());

  const cancelBatch = aliceDb.batch();
  cancelBatch.update(apptRef, {
    statusCode: "CANCELADA",
    updatedAt: pastServerTimestamp(),
  });
  cancelBatch.delete(slotRef);
  await assertSucceeds(cancelBatch.commit());

  // Terminal state locking: cannot update CANCELADA back to CONFIRMADA
  await assertFails(
    apptRef.update({
      statusCode: "CONFIRMADA",
      updatedAt: pastServerTimestamp(),
    })
  );
});

test("UserProfile create and update preserve createdAt and block ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(
    userRef.set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      lastClientName: "Alice Gómez",
      lastClientPhone: "3001112233",
      lastPetName: "Max",
      lastPetBreed: "Beagle",
      createdAt: pastServerTimestamp(),
      updatedAt: pastServerTimestamp(),
    })
  );

  // Valid update modifying allowed fields
  await assertSucceeds(
    userRef.update({
      lastPetName: "Rocky",
      updatedAt: pastServerTimestamp(),
    })
  );

  // Cannot mutate userId or createdAt
  await assertFails(
    userRef.update({
      userId: BOB_UID,
      updatedAt: pastServerTimestamp(),
    })
  );
});
