
import { GoogleGenAI } from "@google/genai";
import { defineSecret } from "firebase-functions/params";

// Firebase Secret Manager secret.
// Locally, Firebase Emulator reads this from functions/.secret.local.
export const geminiApiKey = defineSecret("GEMINI_API_KEY");

// Gemini model used by EcoAlert UM.
const GEMINI_MODEL = "gemini-3.5-flash-lite";

// Create a Gemini client when needed.
function getGeminiClient(): GoogleGenAI {
  const apiKey = geminiApiKey.value();

  if (!apiKey) {
    throw new Error("GEMINI_API_KEY is not configured.");
  }

  return new GoogleGenAI({ apiKey });
}

// Generate text, optionally enforcing JSON output.
export async function generateGeminiText(
  prompt: string,
  jsonMode: boolean = false
): Promise<string> {
  if (!prompt.trim()) {
    throw new Error("Prompt cannot be empty.");
  }

  const ai = getGeminiClient();

  const response = await ai.models.generateContent({
    model: GEMINI_MODEL,
    contents: prompt,
    config: jsonMode
      ? {
          responseMimeType: "application/json",
        }
      : undefined,
  });

  const responseText = response.text?.trim();

  if (!responseText) {
    throw new Error("Gemini returned an empty response.");
  }

  return responseText;
}
