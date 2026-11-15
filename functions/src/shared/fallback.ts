
import {
  AssistantResponse,
  SupportedLanguage,
} from "./types";

export function createFallbackResponse(
  language: SupportedLanguage,
  _hazardDescription: string
): AssistantResponse {
  const isMalay = language === "ms";

  return {
    status: "fallback",

    explanation: isMalay
      ? "Pembantu AI tidak tersedia buat masa ini. Sila rujuk maklumat amaran yang dipaparkan dan ikuti pengumuman keselamatan rasmi universiti."
      : "The AI assistant is currently unavailable. Please refer to the selected hazard information above and follow official university safety announcements.",

    steps: isMalay
      ? [
          "Berhati-hati apabila bergerak di kawasan kampus.",
          "Elakkan kawasan yang dinaiki air dan air banjir yang mengalir.",
          "Ikuti pengumuman dan arahan rasmi universiti.",
        ]
      : [
          "Be cautious when moving around campus.",
          "Avoid flooded areas and flowing floodwater.",
          "Follow official university announcements and instructions.",
        ],

    sources: [
      "Approved EcoAlert UM safety guidance",
    ],

    generatedBy: "rules",
  };
}
