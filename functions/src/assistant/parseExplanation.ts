import { AssistantResponse } from "../shared/types";

export function parseExplanationResponse(
  responseText: string
): AssistantResponse {
  const parsed: unknown = JSON.parse(responseText);

  if (
    typeof parsed !== "object" ||
    parsed === null ||
    !("explanation" in parsed) ||
    typeof parsed.explanation !== "string" ||
    !parsed.explanation.trim() ||
    !("steps" in parsed) ||
    !Array.isArray(parsed.steps) ||
    parsed.steps.length > 3 ||
    !parsed.steps.every(
      (step: unknown) =>
        typeof step === "string" && step.trim().length > 0
    )
  ) {
    throw new Error("Invalid structured Gemini explanation response.");
  }

  return {
    status: "ok",
    explanation: parsed.explanation.trim(),
    steps: parsed.steps,
    sources: ["EcoAlert UM hazard information"],
    generatedBy: "ai",
  };
}