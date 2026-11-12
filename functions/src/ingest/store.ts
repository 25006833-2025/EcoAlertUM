import { DocumentData, Firestore } from "firebase-admin/firestore";

export interface StoredDoc {
  id: string;
  data: DocumentData;
}

/**
 * The few database operations ingestion needs. Firestore in production, an in-memory copy in
 * tests (testing/memoryStore.ts), so adapters are tested without a database or network.
 */
export interface IngestStore {
  getDoc(collection: string, id: string): Promise<DocumentData | undefined>;
  setDoc(collection: string, id: string, data: DocumentData, merge?: boolean): Promise<void>;
  /** Existing `hazardReadings` for one source and type, by document ID. */
  findReadings(source: string, type: string): Promise<Map<string, DocumentData>>;
  /** Writes and deletes `hazardReadings` in one batch. */
  commitReadings(sets: StoredDoc[], deletes: string[]): Promise<void>;
}

export const READINGS = "hazardReadings";
export const SOURCE_STATUS = "sourceStatus";

export function firestoreStore(db: Firestore): IngestStore {
  return {
    async getDoc(collection, id) {
      return (await db.collection(collection).doc(id).get()).data();
    },
    async setDoc(collection, id, data, merge = false) {
      await db.collection(collection).doc(id).set(data, { merge });
    },
    async findReadings(source, type) {
      const snapshot = await db.collection(READINGS).where("source", "==", source).where("type", "==", type).get();
      return new Map(snapshot.docs.map((doc) => [doc.id, doc.data()]));
    },
    async commitReadings(sets, deletes) {
      const batch = db.batch();
      for (const { id, data } of sets) batch.set(db.collection(READINGS).doc(id), data);
      for (const id of deletes) batch.delete(db.collection(READINGS).doc(id));
      await batch.commit();
    },
  };
}
