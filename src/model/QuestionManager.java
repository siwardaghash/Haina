package model;

import java.util.*;

/** The question wizard's operations (FR-35 to FR-39); every change is saved encrypted through SysData. */
public final class QuestionManager {
    private final SysData data;
    private final List<Question> entries = new ArrayList<>();

    public QuestionManager(SysData data) throws Exception {
        this.data = data;
        entries.addAll(data.loadQuestions());
    }

    public List<Question> getAll() { return Collections.unmodifiableList(new ArrayList<>(entries)); }

    private void persist(List<Question> candidates) throws Exception {
        data.saveQuestions(candidates);
        entries.clear();
        entries.addAll(candidates);
    }

    public void add(Question entry) throws Exception {
        SysData.validate(entry);
        for (Question existing: entries) if (existing.getId().equals(entry.getId()))
            throw new IllegalArgumentException("Question ID already exists");
        List<Question> next=new ArrayList<>(entries); next.add(entry); persist(next);
    }

    public void update(String originalId, Question entry) throws Exception {
        SysData.validate(entry); int pos=-1;
        for (int i=0;i<entries.size();i++) {
            Question old=entries.get(i);
            if (old.getId().equals(originalId)) pos=i;
            else if (old.getId().equals(entry.getId())) throw new IllegalArgumentException("Question ID already exists");
        }
        if (pos<0) throw new IllegalArgumentException("Question no longer exists");
        List<Question> next=new ArrayList<>(entries); next.set(pos,entry); persist(next);
    }

    public void delete(String id) throws Exception {
        List<Question> next=new ArrayList<>(entries);
        if (!next.removeIf(q->q.getId().equals(id))) throw new IllegalArgumentException("Question not found");
        persist(next);
    }
}
