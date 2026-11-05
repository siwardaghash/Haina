package model;

/** One multiple-choice question of the bank (SRS 3.7.1), shared by the game and the wizard. */
public final class Question {
    private final String id, level, text;
    private final String[] answers;
    private final int correct;   // 0-based index of the correct answer

    /** @param correct index of the correct answer, 0-3 */
    public Question(String id, String level, String text, String[] answers, int correct) {
        this.id = id;
        this.level = level;
        this.text = text;
        this.answers = answers == null ? null : answers.clone();
        this.correct = correct;
    }

    public String getId() { return id; }
    public String getLevel() { return level; }
    public String getText() { return text; }
    public String[] getAnswers() { return answers == null ? null : answers.clone(); }
    /** 0-based index of the correct answer. */
    public int getCorrect() { return correct; }
    /** Number of the correct answer as shown to the administrator and stored in the file (1-4). */
    public int getCorrectNumber() { return correct + 1; }

    @Override public String toString() { return id + " [" + level + "] " + text; }
}
