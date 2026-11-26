package model;

/**
 * Bonus: the Code Quest trophies. Each one has a title, how to unlock it, and a rarity tier.
 * The {@link AchievementTracker} decides when a player earns one.
 */
public enum Achievement {
    HELLO_WORLD("Hello, World!", "Be the first to get a piece out of the castle", Tier.COMMON),
    INFINITE_LOOP("Infinite Loop", "Roll three 6s in a row", Tier.COMMON),
    MERGE_CONFLICT("Merge Conflict", "Get captured 3 times in one game", Tier.COMMON),
    FIREWALL("Firewall", "Build a Double Block with two of your pieces", Tier.COMMON),
    SENIOR_DEV("Senior Dev", "Answer a hard question correctly", Tier.RARE),
    BUG_SQUASHER("Bug Squasher", "Capture 3 opponent pieces in one game", Tier.RARE),
    DEPLOYED("Deployed!", "Be the first to bring a piece to FINISH", Tier.RARE),
    STACK_OVERFLOW("Stack Overflow", "Answer 3 questions correctly in a row", Tier.EPIC),
    LUCKY_COMMIT("Lucky Commit", "A surprise sends your piece straight to FINISH", Tier.EPIC),
    COMEBACK_KID("Comeback Kid", "Win after having 0 pieces home while an opponent had 2", Tier.LEGENDARY),
    FLAWLESS_BUILD("Flawless Build", "Win with no wrong answers and without being captured", Tier.LEGENDARY);

    /** How rare a trophy is; the view gives each tier its own colour. */
    public enum Tier { COMMON, RARE, EPIC, LEGENDARY }

    private final String title;
    private final String description;
    private final Tier tier;

    Achievement(String title, String description, Tier tier) {
        this.title = title;
        this.description = description;
        this.tier = tier;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Tier getTier() { return tier; }
}