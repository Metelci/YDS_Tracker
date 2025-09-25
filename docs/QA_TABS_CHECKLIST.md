# QA Tabs Checklist

Use this checklist before every release to verify initial usability across all tabs and primary screens.

- Launch and stability
  - App launches within 3s on a mid‑tier device
  - No crashes, ANRs, or red Compose runtime errors
  - No blocking toasts/snackbars on first launch

- Navigation
  - Each bottom tab renders expected content and title
  - Re‑select active tab scrolls to top (if intended)
  - Rapid tab switching preserves state as designed
  - Back button pops correctly; from root exits app

- Core interactions
  - Primary CTA buttons enabled/disabled as expected
  - Forms validate and show inline errors
  - Dialogs/snackbars open and dismiss correctly
  - Pull‑to‑refresh (if present) completes without errors

- States
  - Loading state uses skeleton/placeholder without layout jump
  - Empty state has helpful illustration/message
  - Error state shows actionable message and retry works

- Data & lists
  - Items render without duplicates or missing images
  - Pagination/end‑of‑list works and shows final state

- Responsiveness & settings
  - Portrait/landscape rotation keeps state
  - Dark mode renders correctly; contrast acceptable
  - Font scale 1.3x/1.5x keeps layout intact, no clipping
  - Small phone and tablet basic layouts hold

- Permissions & offline
  - Permission prompts are contextual; denial handled gracefully
  - Airplane mode shows offline UI or cached data, no crash

- Performance
  - Smooth scrolling; no obvious jank
  - No infinite recompositions in Logcat

- Accessibility
  - Touch targets ≥ 48dp; content has semantics
  - Content labels for buttons/images are meaningful

- Security quick pass
  - No secrets in logs/UI
  - Network security config intact; no cleartext unless whitelisted

