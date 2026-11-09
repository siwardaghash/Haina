package model;

import java.util.*;

/** The questions of one game: random per level, no repeat until the level is used up (FR-27). */
public final class QuestionBank {
    private final Map<String,List<Question>> groups = new HashMap<>();
    private final Map<String,List<Question>> remaining = new HashMap<>();
    private final Random random;

    /** Loads the (encrypted) bank through SysData. */
    public QuestionBank(SysData data, Random random) throws Exception { this(data.loadQuestions(), random); }

    public QuestionBank(List<Question> questions, Random random) {
        this.random = Objects.requireNonNull(random);
        SysData.checkMinimum(questions);
        for (Question q : questions) groups.computeIfAbsent(q.getLevel(), k -> new ArrayList<>()).add(q);
    }

    public Question next(String level) {
        List<Question> available=remaining.computeIfAbsent(level,k->new ArrayList<>());
        if (available.isEmpty()) available.addAll(groups.get(level));
        return available.remove(random.nextInt(available.size()));
    }
}
