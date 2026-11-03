# CropItAll — Architecture & Code Style

## Language & Stack
- Kotlin, Android, API 24+ (min), API 36 (target).
- XML layouts + ViewBinding (no Jetpack Compose).
- MVVM: ViewModel + StateFlow.
- DataStore Preferences for settings.
- Storage Access Framework (SAF) only.
- No Hilt, Room, Retrofit.

## Package
- `com.mahksim.CropItAll`

## Code Style
- Comments in Russian.
- Class/function/variable names in English, camelCase.
- Indent: 4 spaces. Max line: 120 chars.
- All public functions and classes — with KDoc.

## Critical Rules
- Never use `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE`. Only SAF.
- All file operations in `Dispatchers.IO`.
- All user-visible strings via `R.string`, never hardcoded.
- Portrait orientation only for `CropActivity` (`android:screenOrientation="portrait"`).
- All dangerous operations (read, write, delete) in `try-catch`.

## Project Structure
- `data/` — repositories (interfaces + implementations).
- `ui/main/` — MainActivity, MainViewModel.
- `ui/crop/` — CropActivity, CropViewModel, CropOverlayView, ImagePagerAdapter.
- `util/` — BitmapUtils, FileUtils.

## Documentation
Before modifying project structure, read:
- `docs/01-architecture.md`
- `docs/04-logic.md`
- 
## File Writing — CRITICAL
- NEVER use shell redirection or heredoc (cat >, echo >, @'...'@, <<EOF) to write files.
- The terminal is Windows PowerShell. Linux/Bash syntax does NOT work.
- ALWAYS use the write_to_file tool to create or edit files.
- If write_to_file fails — stop and ask the user. Do not fallback to shell.