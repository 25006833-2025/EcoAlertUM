import { test } from "node:test";
import assert from "node:assert/strict";

import { parseExplanationResponse } from "./parseExplanation";

test("accepts a valid structured Gemini explanation", () => {
  const response = parseExplanationResponse(
    JSON.stringify({
      explanation: "Heavy rain has been reported near campus.",
      steps: [
        "Avoid flooded areas.",
        "Follow official announcements.",
      ],
    })
  );

  assert.equal(response.status, "ok");
  assert.equal(
    response.explanation,
    "Heavy rain has been reported near campus."
  );
  assert.deepEqual(response.steps, [
    "Avoid flooded areas.",
    "Follow official announcements.",
  ]);
  assert.equal(response.generatedBy, "ai");
});

test("rejects malformed JSON", () => {
  assert.throws(
    () => parseExplanationResponse("{invalid JSON"),
    SyntaxError
  );
});

test("rejects more than three safety steps", () => {
  assert.throws(
    () =>
      parseExplanationResponse(
        JSON.stringify({
          explanation: "Heavy rain warning.",
          steps: ["Step 1", "Step 2", "Step 3", "Step 4"],
        })
      ),
    /Invalid structured Gemini explanation response/
  );
});

test("rejects an empty explanation", () => {
  assert.throws(
    () =>
      parseExplanationResponse(
        JSON.stringify({
          explanation: "   ",
          steps: ["Avoid flooded areas."],
        })
      ),
    /Invalid structured Gemini explanation response/
  );
});

test("rejects invalid safety steps", () => {
  assert.throws(
    () =>
      parseExplanationResponse(
        JSON.stringify({
          explanation: "Heavy rain warning.",
          steps: ["Avoid flooded areas.", "", 123],
        })
      ),
    /Invalid structured Gemini explanation response/
  );
});

test("accepts a response with exactly three safety steps", () => {
  const response = parseExplanationResponse(
    JSON.stringify({
      explanation: "Heavy rain warning.",
      steps: ["Step 1", "Step 2", "Step 3"],
    })
  );

  assert.equal(response.steps.length, 3);
});