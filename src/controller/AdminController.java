package controller;

import java.util.List;
import model.AdminAuth;
import model.Question;
import model.QuestionManager;
import model.SysData;

/** Controller of the question wizard (FR-34 to FR-39): login first, then the question bank operations. */
public final class AdminController {
    private final AdminAuth auth = SysData.get().admin();
    private QuestionManager manager;

    /** True when an administrator account exists on this computer. */
    public boolean hasAdministrator() { return auth.isInitialized(); }

    /** FR-34: checks the login; on success the question bank is opened for management. */
    public boolean login(String username, char[] password) throws Exception {
        if (!auth.authenticate(username, password)) return false;
        manager = new QuestionManager(SysData.get());
        return true;
    }

    /** FR-35: all questions with level and correct answer. */
    public List<Question> questions() { return loggedIn().getAll(); }

    /** FR-36: adds a question; a missing field is rejected. */
    public void add(Question question) throws Exception { loggedIn().add(question); }

    /** FR-37: replaces the question with the given ID. */
    public void update(String originalId, Question question) throws Exception { loggedIn().update(originalId, question); }

    /** FR-38: deletes a question unless its level would drop below 10. */
    public void delete(String id) throws Exception { loggedIn().delete(id); }

    private QuestionManager loggedIn() {
        if (manager == null) throw new IllegalStateException("Log in first");
        return manager;
    }
}
