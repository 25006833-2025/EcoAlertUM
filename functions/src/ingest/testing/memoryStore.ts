import { DocumentData } from "firebase-admin/firestore";

import { IngestStore, READINGS } from "../store";

/** In-memory IngestStore for tests. `docs` is "collection/id" -> data, so tests can inspect writes. */
export class MemoryStore implements IngestStore {
  readonly docs = new Map<string, DocumentData>();
  commits = 0;

  async getDoc(collection: string, id: string): Promise<DocumentData | undefined> {
    return this.docs.get(`${collection}/${id}`);
  }

  async setDoc(collection: string, id: string, data: DocumentData, merge = false): Promise<void> {
    const key = `${collection}/${id}`;
    this.docs.set(key, merge ? { ...this.docs.get(key), ...data } : { ...data });
  }

  async findReadings(source: string, type: string): Promise<Map<string, DocumentData>> {
    const found = new Map<string, DocumentData>();
    for (const [key, data] of this.docs) {
      if (key.startsWith(`${READINGS}/`) && data.source === source && data.type === type) {
        found.set(key.slice(READINGS.length + 1), data);
      }
    }
    return found;
  }

  async commitReadings(
    sets: { id: string; data: DocumentData }[],
    deletes: string[],
    touches: { id: string; data: DocumentData }[] = [],
  ): Promise<void> {
    this.commits++;
    for (const { id, data } of sets) this.docs.set(`${READINGS}/${id}`, { ...data });
    for (const { id, data } of touches) {
      // Like Firestore's update(): fails on a missing doc and only changes the given fields.
      const key = `${READINGS}/${id}`;
      if (!this.docs.has(key)) throw new Error(`update on missing doc ${id}`);
      this.docs.set(key, { ...this.docs.get(key), ...data });
    }
    for (const id of deletes) this.docs.delete(`${READINGS}/${id}`);
  }

  reading(id: string): DocumentData | undefined {
    return this.docs.get(`${READINGS}/${id}`);
  }

  status(source: string): DocumentData | undefined {
    return this.docs.get(`sourceStatus/${source}`);
  }
}
