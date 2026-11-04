
export type AssistantMode = "explain" | "chat";

export type SupportedLanguage = "en" | "ms";

export type AssistantStatus = "ok" | "declined" | "fallback";

export interface ChatMessage {
  role: "user" | "assistant";
  content: string;
}

export interface AskAssistantRequest {
  mode: AssistantMode;
  language: SupportedLanguage;
  alertId?: string;
  message?: string;
  history?: ChatMessage[];
}

export interface AssistantResponse {
  status: AssistantStatus;
  explanation: string;
  steps: string[];
  sources: string[];
  generatedBy: "ai" | "rules";
}
