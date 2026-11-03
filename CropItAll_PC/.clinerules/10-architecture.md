# CropItAll PC — Architecture & Rules

## Language & Stack
- Language: Python 3.10+
- UI Framework: CustomTkinter (ctk)
- Image Processing: Pillow (PIL)
- OS: Windows

## CRITICAL RULES (Для Cline)

1. **File Writing:** NEVER use shell redirection (cat >, echo >, heredoc). ALWAYS use the `write_to_file` tool. The terminal is Windows PowerShell.
2. **UI Thread:** NEVER block the main thread during image processing. Use `threading` or `after()` in Tkinter.
3. **Paths:** Use standard Windows paths or `os.path`.
4. **Structure:** All UI should be in `main.py`. Image processing logic should be in `image_utils.py`.

5. **API VERIFICATION (CRITICAL FOR PYTHON):**
   - The `customtkinter` and `Pillow` libraries are strict. Do NOT guess method names.
   - If you are unsure about a method (e.g., setting colors, themes, or image formatting), EXPLICITLY state that you are unsure, or use the standard documented approach.
   - Example: `ctk.set_default_color_theme()` accepts ONLY specific strings ("blue", "dark-blue", "green"). It does not accept hex codes or "dark". Hex codes must be passed directly to widgets (e.g., `fg_color="#0F9D8E"`).
   
6. **SCOPE & ORDER (Avoid UnboundLocalError):**
   - In Python, functions and variables must be defined BEFORE they are used.
   - Never reference a method that hasn't been defined yet in the class or script.
   
7. **SELF-CHECK BEFORE SUBMITTING:**
   - Before returning the final code, mentally trace the execution. 
   - Check: Are all imports present? Are all variables defined? Do the method signatures match the library documentation?