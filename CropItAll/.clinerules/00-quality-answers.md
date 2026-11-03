# Response Quality Rules

## Structure
- Show reasoning steps, not just final answers.
- If multiple approaches exist — describe alternatives and justify the choice.
- Separate facts from interpretations.
- State when information is incomplete or uncertain.

## Reference
For complex explanations, use the `/quality-answers` skill.

## Self-Check Before Submitting
1. After every code change, run the MCP build tool:
   android-build__build(folder="C:/Users/Anna/AndroidStudioProjects/CropItAll")
2. If build fails — read the error, fix it, and re-run build.
3. Repeat until build returns "BUILD SUCCESSFUL".
4. Before final answer, verify:
   - All files compile without errors.
   - No hardcoded strings (use R.string).
   - No READ_EXTERNAL_STORAGE / WRITE_EXTERNAL_STORAGE.
5. If you cannot verify the result — state it explicitly.