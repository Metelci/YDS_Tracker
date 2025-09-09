# TASK: Metrics & Instrumentation


## Events
- `app_open`, `today_open`, `session_start/complete/skip`, `mock_start/submit`, `reader_pref_change`.


## Implementation
- Thin wrapper `Analytics.track(name, props)`; no PII; log level OFF in release.


## Acceptance
- Events fire once per action; resilient to process death.


## Commit msg
`feat(metrics): lightweight analytics events`