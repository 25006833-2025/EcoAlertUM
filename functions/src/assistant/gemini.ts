
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

// Send a prompt to Gemini and return its text response.
export async function generateGeminiText(
  prompt: string
): Promise<string> {
  if (!prompt.trim()) {
    throw new Error("Prompt cannot be empty.");
  }

  const ai = getGeminiClient();

  const interaction = await ai.interactions.create({
    model: GEMINI_MODEL,
    input: prompt,
    store: false,
  });

  const responseText = interaction.output_text?.trim();

  if (!responseText) {
    throw new Error("Gemini returned an empty response.");
  }

  return responseText;
}
