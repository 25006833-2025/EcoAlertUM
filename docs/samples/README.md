# Real response samples

Every file here is a **real response** captured on **Fri 23 Oct 2026, 20:55–21:15 MYT** during source verification. Findings are in [../data-sources.md](../data-sources.md). No API keys or tokens were used, and none appear in these files.

| File | Exact request | Notes |
|---|---|---|
| `met-datagovmy-weather-warning.json` | `GET https://api.data.gov.my/weather/warning/` | Full response, 5 warnings. MET data republished by data.gov.my |
| `met-datagovmy-forecast-kuala-lumpur.json` | `GET https://api.data.gov.my/weather/forecast/?contains=Kuala Lumpur@location__location_name` | 7 days × 3 locations (St009, Ds058, Tn079) |
| `met-datagovmy-forecast-petaling.json` | `GET https://api.data.gov.my/weather/forecast/?contains=Petaling@location__location_name` | Ds057 Petaling, Tn076 Petaling Jaya |
| `datagovmy-flood-warning-kl-subset.json` | `GET https://api.data.gov.my/flood-warning/` | **Filtered** to the 58 WP Kuala Lumpur stations plus Sg. Klang di Sri Manja. The full response was 1,276 records (1.1 MB). The data is **frozen at Feb 2024**, so it is kept only as a station and coordinate list |
| `infobanjir-water-level-kl.html` | `GET https://publicinfobanjir.water.gov.my/index.php/aras-air/data-paras-air/aras-air-data/?state=WLH&district=Kuala Lumpur&station=ALL` | HTML fragment the public page loads. Live data, 32 stations |
| `infobanjir-rainfall-kl.html` | `GET https://publicinfobanjir.water.gov.my/wp-content/themes/shapely/agency/searchresultrainfall.php?state=WLH&district=Kuala Lumpur&station=ALL&loginStatus=0&language=1` | HTML fragment. Live data, 56 rain gauges, including "Universiti Malaya (F2)" |
| `doe-apims-current-reading.json` | `GET https://eqms.doe.gov.my/api3/publicmapproxy/PUBLIC_DISPLAY/CAQM_MCAQM_Current_Reading/MapServer/0/query?where=1=1&outFields=STATION_ID,DATETIME,API,CLASS,PARAM_SYMBOL,LONGITUDE,LATITUDE,STATION_LOCATION,STATE_NAME&returnGeometry=false&f=json` | All 68 stations nationwide (ArcGIS JSON). This is the same query the DOE map runs |
| `doe-apims-nearest-station-um.json` | `GET https://eqms.doe.gov.my/api3/publicportalapims/getstationidbylonglat?longitude=101.650307&latitude=3.127447` | DOE's own "nearest station" lookup for UM |

These are test fixtures, not live data. Do not ship them in the app.
