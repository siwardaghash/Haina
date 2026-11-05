LUDO • Code Quest - team HAINA
Software Engineering & SQA, University of Haifa, 2026
Team: Lady Shannan, Joelle Naoum, Siwar Daghash, Layan Abu Rabea
Repository: https://github.com/siwardaghash/Haina

HOW TO RUN
- Requires Java 19 (JDK/JRE 19, as in the course guide "מדריך JRE 19"); newer versions also work.
- Double-click Haina.jar, or from a terminal:   java -jar Haina.jar
- From Eclipse: run controller.Main (Run As > Java Application).

DATA FILES
- On the first start a "data" folder is created next to the JAR
  (when started from Eclipse: the "data" folder of the project).
- data/history.csv  - game history, in the course template's exact columns (SRS DC-2).
- data/questions.dat - question bank: the XML of SRS DC-7, stored encrypted (CR-1).
  A starting bank is packed inside the JAR and copied here on the first start.
- data/admin.xml    - administrator accounts for the question wizard (passwords stored as hashes).
  Not in GitHub. A team member adds an account with controller.CreateAdmin (run from Eclipse).

PACKAGES (MVC)
- model      - game rules and data: Board, Piece, Game, Question, QuestionBank, GameHistory, SysData...
               Game is the subject of the Observer pattern (GameObserver).
- view       - Swing screens: opening, How to Play, setup, board, end of game, history, question wizard.
               The board screen observes the Game and shows its state.
- controller - AppController (Singleton, switches screens), GameController, AdminController,
               HistoryController, Main (start of the program) and CreateAdmin (team tool).
  Every player action goes view -> controller -> model; the view only reads the model.

BUILDING THE JAR
- In a terminal, from the project folder:   sh build-jar.sh
  The result is dist/Haina.jar (Main-Class controller.Main, compiled for Java 19).
