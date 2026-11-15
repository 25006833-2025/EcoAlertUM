import { onCall, HttpsError } from "firebase-functions/v2/https";
import { getFirestore } from "firebase-admin/firestore";

import { geminiApiKey, generateGeminiText } from "./gemini";
import { buildAssistantPrompt } from "./prompts";
import { validateAssistantRequest } from "./validation";
import { createFallbackResponse } from "../shared/fallback";
import { parseExplanationResponse } from "./parseExplanation";

export const askAssistant = onCall(
  {
    secrets: [geminiApiKey],
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError(
        "unauthenticated",
        "Please sign in to use the AI assistant."
      );
    }

    const email = request.auth.token.email;
    const emailVerified = request.auth.token.email_verified;

    if (
      typeof email !== "string" ||
      !email.toLowerCase().endsWith("@siswa.um.edu.my") ||
      emailVerified !== true
    ) {
      throw new HttpsError(
        "permission-denied",
        "A verified Universiti Malaya student email is required."
      );
    }

    const validated = validateAssistantRequest(request.data);
    const db = getFirestore();

    let hazardContext = "";

    if (validated.mode === "explain") {
      const alertSnapshot = await db
        .collection("alerts")
        .doc(validated.alertId!)
        .get();

      if (!alertSnapshot.exists) {
        throw new HttpsError("not-found", "Alert not found.");
      }

      hazardContext = JSON.stringify(alertSnapshot.data());
    } else {
      hazardContext =
        "No specific alert was selected. Only answer using information provided in the conversation.";
    }

    const prompt = buildAssistantPrompt(
      validated.mode,
      validated.language,
      hazardContext,
      validated.message,
      validated.history
    );

    try {
      const responseText = await generateGeminiText(
        prompt,
        validated.mode === "explain"
      );

      if (validated.mode === "explain") {
        return parseExplanationResponse(responseText);
      }

      // Preserve plain-text responses for FR4.2 chat.
      return {
        status: "ok",
        explanation: responseText,
        steps: [],
        sources: ["EcoAlert UM hazard information"],
        generatedBy: "ai",
      };
    } catch (error) {
      console.error("Gemini request or response parsing failed:", error);

      return createFallbackResponse(
        validated.language,
        hazardContext || "No specific hazard information is available."
      );
    }
  }
);