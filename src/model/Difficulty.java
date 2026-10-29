package model;


public enum Difficulty {
    EASY   ("Easy",   0,     4,      4,      0,        0,      false, false),
    MEDIUM ("Medium", 1,     8,      8,      6,        2,      true,  false),
    HARD   ("Hard",   2,    12,     12,      9,        3,      false, true);

    private final String label;
    private final int extraRings;
    private final int protectSquares;
    private final int arrowSquares;
    private final int questionSquares;
    private final int surpriseSquares;
    private final boolean boostDie;
    private final boolean ultraBoostDie;

    Difficulty(String label, int extraRings, int protectSquares, int arrowSquares,
               int questionSquares, int surpriseSquares, boolean boostDie, boolean ultraBoostDie) {
        this.label = label;
        this.extraRings = extraRings;
        this.protectSquares = protectSquares;
        this.arrowSquares = arrowSquares;
        this.questionSquares = questionSquares;
        this.surpriseSquares = surpriseSquares;
        this.boostDie = boostDie;
        this.ultraBoostDie = ultraBoostDie;
    }

    public String getLabel()          { return label; }
    public int getExtraRings()        { return extraRings; }
    public int getProtectSquares()    { return protectSquares; }
    public int getArrowSquares()      { return arrowSquares; }
    public int getQuestionSquares()   { return questionSquares; }
    public int getSurpriseSquares()   { return surpriseSquares; }
    public boolean hasBoostDie()      { return boostDie; }
    public boolean hasUltraBoostDie() { return ultraBoostDie; }
}