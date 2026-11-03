# Localization

## Rules
- All user-visible strings MUST be in `R.string`.
- When adding a new string, add it to BOTH files:
    - `app/src/main/res/values/strings.xml` (English, default)
    - `app/src/main/res/values-ru/strings.xml` (Russian)
- String `name` attributes must match across files.
- Never hardcode Russian or English text in code.

## Fallback
- If phone language is Russian → use `values-ru/strings.xml`.
- If phone language is anything else → use `values/strings.xml` (English).

## Resource Folder Rules
- `values-ru/` contains ONLY `strings.xml`. No colors, no themes.
- Colors and themes go ONLY in:
    - `values/colors.xml` (light theme)
    - `values/themes.xml` (light theme)
    - `values-night/colors.xml` (dark theme, if needed)
    - `values-night/themes.xml` (dark theme)
- Reason: language qualifier (`-ru`) has higher priority than night qualifier (`-night`).
  If colors were in `values-ru/`, dark theme would be ignored for Russian phones.