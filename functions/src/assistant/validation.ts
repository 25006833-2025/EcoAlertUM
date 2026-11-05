
import {
  AskAssistantRequest,
  ChatMessage,
} from "../shared/types";

const MAX_MESSAGE_LENGTH = 1000;
const MAX_HISTORY_MESSAGES = 10;
const MAX_HISTORY_CONTENT_LENGTH = 2000;

export function validateAssistantRequest(
  data: unknown
): AskAssistantRequest {
  if (!data || typeof data !== "object") {
    throw new Error("Invalid request data.");
  }

  const request = data as Record<string, unknown>;

  if (request.mode !== "explain" && request.mode !== "chat") {
    throw new Error("Invalid assistant mode.");
  }

  if (request.language !== "en" && request.language !== "ms") {
    throw new Error("Unsupported language.");
  }

  if (request.mode === "explain") {
    if (
      typeof request.alertId !== "string" ||
      request.alertId.trim().length === 0
    ) {
      throw new Error("A valid alert ID is required.");
    }
  }

  if (request.mode === "chat") {
    if (
      typeof request.message !== "string" ||
      request.message.trim().length === 0 ||
      request.message.length > MAX_MESSAGE_LENGTH
    ) {
      throw new Error("Invalid message.");
    }
  }

  const history = request.history ?? [];

  if (!Array.isArray(history) || history.length > MAX_HISTORY_MESSAGES) {
    throw new Error("Invalid conversation history.");
  }

  const validatedHistory: ChatMessage[] = history.map((item) => {
    if (
      !item ||
      typeof item !== "object" ||
      !["user", "assistant"].includes(item.role) ||
      typeof item.content !== "string" ||
      item.content.trim().length === 0 ||
      item.content.length > MAX_HISTORY_CONTENT_LENGTH
    ) {
      throw new Error("Invalid conversation history message.");
    }

    return {
      role: item.role,
      content: item.content,
    };
  });

  return {
    mode: request.mode,
    language: request.language,
    alertId: request.mode === "explain"
      ? (request.alertId as string).trim()
      : undefined,
    message: request.mode === "chat"
      ? (request.message as string).trim()
      : undefined,
    history: validatedHistory,
  };
}
