
import { onDocumentCreated } from "firebase-functions/v2/firestore";

import {
  geminiApiKey,
  generateGeminiText,
} from "../assistant/gemini";

export const onReportCreated = onDocumentCreated(
  {
    document: "reports/{reportId}",
    secrets: [geminiApiKey],
  },
  async (event) => {
    const snapshot = event.data;

    if (!snapshot) {
      return;
    }

    const report = snapshot.data();
    const reportId = event.params.reportId;

    const description = report.description;
    const location = report.location ?? "Unknown location";

    if (
      typeof description !== "string" ||
      description.trim().length === 0
    ) {
      await snapshot.ref.update({
        processingStatus: "failed",
        classificationError: "Missing report description.",
      });
      return;
    }

    const prompt = `
You are EcoAlert UM, a campus hazard report classifier.

Classify the following student-submitted hazard report.

Report description: ${description}
Reported location: ${location}

Rules:
- Do not assume the report is true or verified.
- Do not invent details that are not provided.
- If the information is insufficient, use "Unknown".
- Severity must be one of: low, moderate, high, unknown.
- Classification should be a short hazard category.
- Summary should briefly describe what the student reported.
- Return valid JSON only, without Markdown.

Required JSON format:
{
  "classification": "Water Accumulation",
  "severity": "unknown",
  "summary": "A student reported water accumulation near a walkway."
}
`;

    try {
      const result = await generateGeminiText(prompt);

      const cleanedResult = result
        .replace(/^```json\s*/i, "")
        .replace(/^```\s*/i, "")
        .replace(/\s*```$/, "")
        .trim();

      const parsed = JSON.parse(cleanedResult);

      const validSeverities = [
        "low",
        "moderate",
        "high",
        "unknown",
      ];

      const classification =
        typeof parsed.classification === "string"
          ? parsed.classification
          : "Unknown";

      const severity = validSeverities.includes(parsed.severity)
        ? parsed.severity
        : "unknown";

      const summary =
        typeof parsed.summary === "string"
          ? parsed.summary
          : "Unable to generate report summary.";

      await snapshot.ref.update({
        classification,
        severity,
        summary,
        processingStatus: "completed",
        verificationStatus: "unverified",
        processedAt: new Date(),
      });

      console.log(`Report ${reportId} classified successfully.`);
    } catch (error) {
      console.error(
        `Failed to classify report ${reportId}:`,
        error
      );

      await snapshot.ref.update({
        classification: "Unknown",
        severity: "unknown",
        processingStatus: "failed",
        verificationStatus: "unverified",
      });
    }
  }
);
