import {
  AssistantResponse,
  SupportedLanguage,
} from "./types";

export function createFallbackResponse(
  language: SupportedLanguage,
  hazardDescription: string
): AssistantResponse {
  const isMalay = language === "ms";

  return {
    status: "fallback",
    explanation: isMalay
      ? `Maklumat bahaya yang tersedia: ${hazardDescription}. Pembantu AI tidak dapat memberikan penjelasan lanjut buat masa ini.`
      : `Available hazard information: ${hazardDescription}. The AI assistant is temporarily unable to provide further explanation.`,

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
