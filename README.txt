Ludo Code Quest - Step 6A: Questions

Updated: src/model/Game.java, src/screens/GameBoardScreen.java
New: src/model/QuestionBank.java, data/questions.xml

Copy these four files to the matching locations in the same GitHub clone.
IMPORTANT: Preserve the data/questions.xml directory in the repository.
Run the application with the project's root directory as working directory (the directory containing src and data).
If Eclipse uses the parent directory as working directory, GameBoardScreen also tries Haina-main/data/questions.xml.

What works:
- Multiple-choice questions from an XML question bank (10 easy, 10 medium, 10 hard).
- Random non-repeating selection per difficulty, reset after exhaustion.
- Dialog opens upon landing on a base-track question square.
- Correct answers move piece +1/+2/+3; incorrect answers -1/-2/-3; displays correct answer.
- Subsequent question-square landings can trigger another question.
- Capture check after question movement and protection rules continue to apply.

Current limitations:
- Questions on extra Medium/Hard tracks cannot trigger until their movement topology is implemented.
- Surprise squares are visually placed but not functional yet.
- The existing simplified base-track rules/graphics and Double Block rules are not full Ludo V2 for medium/hard.
- JDK 21 compilation tested; JDK 19 and graphical display not tested here.
