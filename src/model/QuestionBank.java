package model;

import java.io.File;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

/** Reads the course question bank from local XML; no third-party libraries. */
public final class QuestionBank {
    public static final class Question {
        private final String id, level, text;
        private final String[] answers;
        private final int correct;
        Question(String id, String level, String text, String[] answers, int correct) {
            this.id=id; this.level=level; this.text=text; this.answers=answers.clone(); this.correct=correct;
        }
        public String getId() { return id; }
        public String getLevel() { return level; }
        public String getText() { return text; }
        public String[] getAnswers() { return answers.clone(); }
        public int getCorrect() { return correct; } // 0-based
    }
    private final Map<String,List<Question>> groups = new HashMap<>();
    private final Map<String,List<Question>> remaining = new HashMap<>();
    private final Random random;
    public QuestionBank(File file, Random random) throws Exception {
        this.random = Objects.requireNonNull(random);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        Document document = factory.newDocumentBuilder().parse(file);
        NodeList nodes = document.getElementsByTagName("question");
        Set<String> ids = new HashSet<>();
        for (int i=0;i<nodes.getLength();i++) {
            Element e=(Element)nodes.item(i);
            String id=e.getAttribute("id").trim(), level=e.getAttribute("level").trim().toLowerCase(Locale.ROOT);
            if (!ids.add(id) || id.isEmpty() || !Arrays.asList("easy","medium","hard").contains(level))
                throw new IllegalArgumentException("Invalid or duplicate question id/level: " + id);
            String text=content(e,"text");
            NodeList options=e.getElementsByTagName("answer");
            if (options.getLength()!=4) throw new IllegalArgumentException("Four answers required: " + id);
            String[] answers=new String[4];
            for (int j=0;j<4;j++) answers[j]=options.item(j).getTextContent().trim();
            int correct=Integer.parseInt(content(e,"correct"))-1;
            if (correct<0 || correct>=4 || text.isEmpty()) throw new IllegalArgumentException("Invalid question: " + id);
            for (String answer: answers) if (answer.isEmpty()) throw new IllegalArgumentException("Empty answer: " + id);
            groups.computeIfAbsent(level,k->new ArrayList<>()).add(new Question(id,level,text,answers,correct));
        }
        for (String level : Arrays.asList("easy","medium","hard"))
            if (groups.getOrDefault(level,Collections.emptyList()).size()<10)
                throw new IllegalArgumentException("At least 10 questions required for " + level);
    }
    private static String content(Element parent,String tag) {
        NodeList elements=parent.getElementsByTagName(tag);
        if (elements.getLength()!=1) throw new IllegalArgumentException("Missing " + tag);
        return elements.item(0).getTextContent().trim();
    }
    public Question next(String level) {
        List<Question> available=remaining.computeIfAbsent(level,k->new ArrayList<>());
        if (available.isEmpty()) available.addAll(groups.get(level));
        return available.remove(random.nextInt(available.size()));
    }
}
