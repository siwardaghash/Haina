package model;

import java.io.File;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.*;

/** Validates and persists edits to the game's existing question XML schema. */
public final class QuestionManager {
    public static final class Entry {
        public final String id, level, text;
        public final String[] answers;
        public final int correct; // 1..4, matching XML
        public Entry(String id, String level, String text, String[] answers, int correct) {
            this.id=id; this.level=level; this.text=text;
            this.answers=answers == null ? null : answers.clone(); this.correct=correct;
        }
        @Override public String toString() { return id + " [" + level + "] " + text; }
    }
    private final File file;
    private final List<Entry> entries = new ArrayList<>();
    public QuestionManager(File file) throws Exception { this.file=file; load(); }
    public List<Entry> getAll() { return Collections.unmodifiableList(new ArrayList<>(entries)); }

    private void load() throws Exception {
        Document doc = AdminAuth.secureFactory().newDocumentBuilder().parse(file);
        NodeList list = doc.getElementsByTagName("question");
        entries.clear();
        for (int i=0;i<list.getLength();i++) {
            Element node=(Element) list.item(i);
            NodeList choices=node.getElementsByTagName("answer");
            if (choices.getLength()!=4) throw new IllegalArgumentException("Invalid answer count");
            String[] a = new String[4];
            for (int j=0;j<4;j++) a[j]=choices.item(j).getTextContent().trim();
            entries.add(new Entry(node.getAttribute("id"),node.getAttribute("level"),
                    node.getElementsByTagName("text").item(0).getTextContent(),a,
                    Integer.parseInt(node.getElementsByTagName("correct").item(0).getTextContent().trim())));
        }
        for (Entry e: entries) validate(e);
        checkMinimum(entries);
    }
    private static String nonempty(String text, String field) {
        if (text==null || text.trim().isEmpty()) throw new IllegalArgumentException(field + " cannot be empty");
        return text.trim();
    }
    private static void validate(Entry entry) {
        nonempty(entry.id,"ID"); nonempty(entry.text,"Question");
        if (!Arrays.asList("easy","medium","hard").contains(entry.level))
            throw new IllegalArgumentException("Choose a valid difficulty");
        if (entry.answers==null || entry.answers.length!=4) throw new IllegalArgumentException("Exactly four answers are required");
        for (int i=0;i<4;i++) nonempty(entry.answers[i],"Answer " + (i+1));
        if (entry.correct<1 || entry.correct>4) throw new IllegalArgumentException("Correct answer must be 1-4");
    }
    private static void checkMinimum(List<Entry> items) {
        for (String level: Arrays.asList("easy","medium","hard")) {
            long count=items.stream().filter(q->level.equals(q.level)).count();
            if (count<10) throw new IllegalArgumentException("At least 10 " + level + " questions must remain");
        }
    }
    private void persist(List<Entry> candidates) throws Exception {
        checkMinimum(candidates);
        DocumentBuilder builder=AdminAuth.secureFactory().newDocumentBuilder();
        Document doc=builder.newDocument();
        Element root=doc.createElement("questions"); doc.appendChild(root);
        for (Entry q: candidates) {
            Element element=doc.createElement("question"); root.appendChild(element);
            element.setAttribute("id",q.id); element.setAttribute("level",q.level);
            child(doc,element,"text",q.text);
            for (String answer: q.answers) child(doc,element,"answer",answer);
            child(doc,element,"correct",Integer.toString(q.correct));
        }
        AdminAuth.saveDocument(doc,file);
        entries.clear(); entries.addAll(candidates);
    }
    private static void child(Document doc, Element parent, String name, String value) {
        Element e=doc.createElement(name); e.setTextContent(value); parent.appendChild(e);
    }
    public void add(Entry entry) throws Exception {
        validate(entry);
        for (Entry existing: entries) if (existing.id.equals(entry.id))
            throw new IllegalArgumentException("Question ID already exists");
        List<Entry> next=new ArrayList<>(entries); next.add(entry); persist(next);
    }
    public void update(String originalId, Entry entry) throws Exception {
        validate(entry); int pos=-1;
        for (int i=0;i<entries.size();i++) {
            Entry old=entries.get(i);
            if (old.id.equals(originalId)) pos=i;
            else if (old.id.equals(entry.id)) throw new IllegalArgumentException("Question ID already exists");
        }
        if (pos<0) throw new IllegalArgumentException("Question no longer exists");
        List<Entry> next=new ArrayList<>(entries); next.set(pos,entry); persist(next);
    }
    public void delete(String id) throws Exception {
        List<Entry> next=new ArrayList<>(entries);
        if (!next.removeIf(q->q.id.equals(id))) throw new IllegalArgumentException("Question not found");
        persist(next);
    }
}
