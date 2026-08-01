# GPS Logger optimization baseline

This document defines the repeatable baseline used before changing the tracking engine, database writes, background service, or UI update frequency.

## Goals

The baseline must answer four questions:

1. Does recording remain continuous when the screen is off and the app is in the background?
2. How much battery, CPU, and memory does each scenario consume?
3. How accurate and stable is the recorded track before filtering changes?
4. Does resource use remain bounded during long recordings and large exports?

## Test build

Use the debug application from the `develop` branch:

- application ID: `eu.basicairdata.graziano.gpslogger.dev`
- launcher label: `GPS Logger Dev`
- target SDK: 36

The development application has its own database, preferences, files, and FileProvider authority, so it can be installed beside the release application.

## Required metadata

Record these details for every test series:

- phone model and Android version;
- GPS mode and whether an external receiver is used;
- battery capacity, starting charge, and ending charge;
- screen brightness and screen-on time;
- mobile data, Wi-Fi, Bluetooth, and airplane-mode state;
- battery optimization state for GPS Logger Dev;
- location permission state;
- GPS Logger recording interval and distance settings;
- weather, route type, and approximate sky visibility;
- build commit SHA.

Do not compare two runs unless the material conditions are documented.

## Collector

The repository includes `scripts/collect-android-baseline.sh`.

Prepare a scenario:

```bash
./scripts/collect-android-baseline.sh --prepare --label walking-60m
```

This clears logcat and resets Android batterystats. Start recording in GPS Logger Dev and perform the scenario.

Collect the results:

```bash
./scripts/collect-android-baseline.sh --collect --label walking-60m
```

Results are written under `baseline-results/`. This directory must remain local and must not contain personal routes in commits or pull requests.

Use another package when necessary:

```bash
./scripts/collect-android-baseline.sh \
  --collect \
  --package eu.basicairdata.graziano.gpslogger \
  --label upstream-comparison
```

## Scenario matrix

### Stationary background

- duration: 30 minutes;
- phone stationary with reasonable sky visibility;
- recording active;
- screen off for the entire measured interval.

Measure false movement, number of recorded points, battery use, wake behavior, and whether memory settles.

### Walking

- duration: 45–60 minutes;
- mixed open sky and partial obstruction;
- screen off except for a short status check midway.

Measure route continuity, corner cutting, spikes, distance error, altitude stability, and battery use.

### Driving

- duration: 45–60 minutes;
- include stops, low speed, and normal road speed;
- keep the app in the background with the screen off.

Measure gaps, delayed fixes, implausible speed changes, point density, and service survival.

### Long background recording

- duration: at least 3 hours;
- screen mostly off;
- include stationary and moving periods.

Measure heap growth, process residency, foreground-service state, track gaps, and recovery after returning to the UI.

### Export stress

Use a large recorded track or a restored test database and export KML, GPX, and TXT together.

Measure export duration, peak memory, thread count, responsiveness, output validity, and failure recovery.

## Results table

| Field | Value |
|---|---|
| Commit | |
| Device / Android | |
| Scenario | |
| Duration | |
| Start / end battery | |
| Battery optimization | |
| GPS interval / distance | |
| Recorded points | |
| Reported distance | |
| Reference distance | |
| Track gaps | |
| Visible spikes | |
| Initial PSS | |
| Final PSS | |
| Peak PSS | |
| Average CPU | |
| Export duration | |
| Notes | |

## Initial acceptance criteria

Before optimization work is considered safe:

- debug and release applications can coexist;
- recording continues with the screen off;
- the location foreground service remains active while recording;
- no unexplained multi-minute gaps occur in normal conditions;
- process memory reaches a plateau rather than increasing continuously;
- stopping a recording releases location updates and unnecessary work;
- exported files open successfully in an independent viewer;
- existing tracks survive application restart and database backup/restore;
- CI build, unit tests, and lint remain green.

These criteria establish regressions, not final product targets. Numeric battery and accuracy targets will be set after at least two comparable runs per scenario.
