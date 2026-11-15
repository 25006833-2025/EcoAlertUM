import assert from "node:assert/strict";
import { test } from "node:test";
import { createFallbackResponse } from "./fallback";

test("English fallback does not expose raw Firestore JSON", () => {
  const rawAlert = JSON.stringify({
    hazardType: "rainfall",
    level: "advisory",
    area: "whole_campus",
    ruleId: "rain_rule_01",
  });

  const response = createFallbackResponse("en", rawAlert);

  assert.equal(response.status, "fallback");
  assert.equal(response.generatedBy, "rules");
  assert.ok(response.explanation.length > 0);
  assert.ok(!response.explanation.includes(rawAlert));
  assert.ok(!response.explanation.includes("ruleId"));
  assert.equal(response.steps.length, 3);
  assert.ok(response.sources.length > 0);
});

test("Malay fallback displays readable Malay guidance", () => {
  const response = createFallbackResponse(
    "ms",
    '{"hazardType":"rainfall"}'
  );

  assert.equal(response.status, "fallback");
  assert.equal(response.generatedBy, "rules");
  assert.ok(response.explanation.includes("Pembantu AI"));
  assert.ok(!response.explanation.includes("hazardType"));
  assert.equal(response.steps.length, 3);
});

test("Fallback works when hazard information is unavailable", () => {
  const response = createFallbackResponse("en", "");

  assert.equal(response.status, "fallback");
  assert.ok(response.explanation.length > 0);
  assert.equal(response.steps.length, 3);
});
