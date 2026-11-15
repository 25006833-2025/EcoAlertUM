import { readFileSync } from "node:fs";
import { join } from "node:path";

/** Real responses captured on 23 Oct 2026 (docs/samples). Compiled tests run from functions/lib/ingest/testing. */
export function sampleText(name: string): string {
  return readFileSync(join(__dirname, "..", "..", "..", "..", "docs", "samples", name), "utf8");
}

/** Builds an HttpGet that answers from the saved samples, so tests never touch the network. */
export function sampleGet(overrides: Record<string, () => string> = {}): (url: string) => Promise<string> {
  const routes: Array<[string, () => string]> = [
    ...Object.entries(overrides),
    ["aras-air-data", () => sampleText("infobanjir-water-level-kl.html")],
    ["searchresultrainfall", () => sampleText("infobanjir-rainfall-kl.html")],
    ["eqms.doe.gov.my", () => sampleText("doe-apims-current-reading.json")],
    ["/weather/warning/", () => sampleText("met-datagovmy-weather-warning.json")],
    ["/weather/forecast/", () => sampleText("met-datagovmy-forecast-kuala-lumpur.json")],
  ];
  return async (url) => {
    const route = routes.find(([part]) => url.includes(part));
    if (!route) throw new Error(`no sample for ${url}`);
    return route[1]();
  };
}
