package model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Single access point for every data file (SRS class SysData; Singleton):
 * the game history (CSV, course schema), the question bank and the administrators.
 *
 * The question bank holds the correct answers, so it is protected at the data level:
 * it is stored encrypted with AES-GCM (Java's built-in javax.crypto, no external library).
 * Opening the file shows no questions or answers, and any change made outside the
 * question wizard is detected (the GCM tag no longer matches) and the file is rejected.
 * Inside the encryption the content is the XML of SRS DC-7.
 * Limit: the key is part of the program, so someone who takes the program's code apart
 * could recover it; the protection is aimed at players and casual editing of the file.
 */
public final class SysData {
    private static final SysData INSTANCE = new SysData(defaultDirectory());

    private static final byte[] MAGIC = "HAINA-QB1".getBytes(StandardCharsets.US_ASCII);
    private static final int IV_BYTES = 12, TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final File directory;

    private SysData(File directory) { this.directory = directory; }

    public static SysData get() { return INSTANCE; }

    /** For tests and tools: a SysData that works on another folder. */
    public static SysData at(File directory) { return new SysData(directory); }

    /**
     * Where the data files live (SRS 3.6.5: the same file runs anywhere):
     * "data" in the folder the game was started from (Eclipse, terminal), otherwise
     * "data" next to the JAR (double-click on the JAR, where the start folder can be anything).
     */
    private static File defaultDirectory() {
        File data = new File("data");
        if (data.isDirectory()) return data;
        File nested = new File("Haina-main/data");   // when started from the folder above the project
        if (nested.isDirectory()) return nested;
        File besideJar = besideProgram();
        return besideJar != null ? besideJar : data;
    }

    private static File besideProgram() {
        try {
            File location = new File(SysData.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File folder = location.isFile() ? location.getParentFile() : location;   // the JAR's folder
            return folder == null ? null : new File(folder, "data");
        } catch (Exception ex) {
            return null;
        }
    }

    /** First run of a new installation: the encrypted starting bank packed inside the JAR (SRS 2.x). */
    private void installBundledBank() throws IOException {
        try (java.io.InputStream in = SysData.class.getResourceAsStream("/defaults/questions.dat")) {
            if (in == null) return;
            writeAtomically(questionsFile().toPath(), in.readAllBytes());
        }
    }

    public File historyFile() { return new File(directory, "history.csv"); }
    public File questionsFile() { return new File(directory, "questions.dat"); }
    /** The old readable bank; read once, encrypted into questions.dat, then removed. */
    public File legacyQuestionsFile() { return new File(directory, "questions.xml"); }
    public File adminFile() { return new File(directory, "admin.xml"); }

    public GameHistory history() { return new GameHistory(historyFile().toPath()); }
    public AdminAuth admin() { return new AdminAuth(adminFile()); }

    // ------------------------------------------------------------------ question bank

    /** Reads, decrypts and validates all questions. */
    public synchronized List<Question> loadQuestions() throws Exception {
        if (!questionsFile().isFile() && legacyQuestionsFile().isFile()) migrateLegacyBank();
        if (!questionsFile().isFile()) installBundledBank();
        if (!questionsFile().isFile())
            throw new IOException("Question bank not found: " + questionsFile().getPath());
        byte[] xml = decrypt(Files.readAllBytes(questionsFile().toPath()));
        List<Question> questions = parse(AdminAuth.secureFactory().newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml)));
        checkMinimum(questions);
        return questions;
    }

    /** Validates, encrypts and saves the whole bank (used only by the question wizard). */
    public synchronized void saveQuestions(List<Question> questions) throws Exception {
        Set<String> ids = new HashSet<>();
        for (Question q : questions) {
            validate(q);
            if (!ids.add(q.getId())) throw new IllegalArgumentException("Question ID already exists: " + q.getId());
        }
        checkMinimum(questions);
        writeAtomically(questionsFile().toPath(), encrypt(toXml(questions)));
    }

    /** One-time step: turn the readable questions.xml into the encrypted questions.dat. */
    private void migrateLegacyBank() throws Exception {
        List<Question> questions = parse(AdminAuth.secureFactory().newDocumentBuilder().parse(legacyQuestionsFile()));
        saveQuestions(questions);
        if (!parseEncryptedMatches(questions)) throw new IOException("Encrypted question bank could not be verified");
        Files.delete(legacyQuestionsFile().toPath());
    }

    private boolean parseEncryptedMatches(List<Question> expected) throws Exception {
        byte[] xml = decrypt(Files.readAllBytes(questionsFile().toPath()));
        List<Question> back = parse(AdminAuth.secureFactory().newDocumentBuilder().parse(new ByteArrayInputStream(xml)));
        if (back.size() != expected.size()) return false;
        for (int i = 0; i < back.size(); i++) {
            Question a = back.get(i), b = expected.get(i);
            if (!a.getId().equals(b.getId()) || !a.getText().equals(b.getText()) || a.getCorrect() != b.getCorrect()
                    || !Arrays.equals(a.getAnswers(), b.getAnswers())) return false;
        }
        return true;
    }

    public static void validate(Question q) {
        nonEmpty(q.getId(), "ID");
        nonEmpty(q.getText(), "Question");
        if (!Arrays.asList("easy", "medium", "hard").contains(q.getLevel()))
            throw new IllegalArgumentException("Choose a valid difficulty");
        String[] answers = q.getAnswers();
        if (answers == null || answers.length != 4) throw new IllegalArgumentException("Exactly four answers are required");
        for (int i = 0; i < 4; i++) nonEmpty(answers[i], "Answer " + (i + 1));
        if (q.getCorrect() < 0 || q.getCorrect() > 3) throw new IllegalArgumentException("Correct answer must be 1-4");
    }

    /** SRS 3.7.1 / FR-39: at least 10 questions per level. */
    public static void checkMinimum(List<Question> questions) {
        for (String level : Arrays.asList("easy", "medium", "hard")) {
            long count = questions.stream().filter(q -> level.equals(q.getLevel())).count();
            if (count < 10) throw new IllegalArgumentException("At least 10 " + level + " questions must remain");
        }
    }

    private static void nonEmpty(String text, String field) {
        if (text == null || text.trim().isEmpty()) throw new IllegalArgumentException(field + " cannot be empty");
    }

    private static List<Question> parse(Document document) {
        NodeList nodes = document.getElementsByTagName("question");
        List<Question> questions = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element) nodes.item(i);
            String id = e.getAttribute("id").trim();
            String level = e.getAttribute("level").trim().toLowerCase(Locale.ROOT);
            NodeList options = e.getElementsByTagName("answer");
            if (options.getLength() != 4) throw new IllegalArgumentException("Four answers required: " + id);
            String[] answers = new String[4];
            for (int j = 0; j < 4; j++) answers[j] = options.item(j).getTextContent().trim();
            Question q = new Question(id, level, content(e, "text"), answers,
                    Integer.parseInt(content(e, "correct")) - 1);
            validate(q);
            if (!ids.add(id)) throw new IllegalArgumentException("Duplicate question id: " + id);
            questions.add(q);
        }
        return questions;
    }

    private static String content(Element parent, String tag) {
        NodeList elements = parent.getElementsByTagName(tag);
        if (elements.getLength() != 1) throw new IllegalArgumentException("Missing " + tag);
        return elements.item(0).getTextContent().trim();
    }

    private static byte[] toXml(List<Question> questions) throws Exception {
        Document doc = AdminAuth.secureFactory().newDocumentBuilder().newDocument();
        Element root = doc.createElement("questions");
        doc.appendChild(root);
        for (Question q : questions) {
            Element element = doc.createElement("question");
            root.appendChild(element);
            element.setAttribute("id", q.getId());
            element.setAttribute("level", q.getLevel());
            child(doc, element, "text", q.getText());
            for (String answer : q.getAnswers()) child(doc, element, "answer", answer);
            child(doc, element, "correct", Integer.toString(q.getCorrectNumber()));
        }
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(out));
        return out.toByteArray();
    }

    private static void child(Document doc, Element parent, String name, String value) {
        Element e = doc.createElement(name);
        e.setTextContent(value);
        parent.appendChild(e);
    }

    // ------------------------------------------------------------------ encryption (AES-GCM)

    private static SecretKeySpec key() throws GeneralSecurityException {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest("HAINA Ludo Code Quest - question bank key v1".getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(Arrays.copyOf(digest, 16), "AES");
    }

    /** File layout: MAGIC | 12-byte random IV | ciphertext with 16-byte tag. A new IV for every save. */
    static byte[] encrypt(byte[] plain) throws GeneralSecurityException {
        byte[] iv = new byte[IV_BYTES];
        RANDOM.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
        cipher.updateAAD(MAGIC);
        byte[] sealed = cipher.doFinal(plain);
        byte[] out = new byte[MAGIC.length + IV_BYTES + sealed.length];
        System.arraycopy(MAGIC, 0, out, 0, MAGIC.length);
        System.arraycopy(iv, 0, out, MAGIC.length, IV_BYTES);
        System.arraycopy(sealed, 0, out, MAGIC.length + IV_BYTES, sealed.length);
        return out;
    }

    static byte[] decrypt(byte[] file) throws IOException, GeneralSecurityException {
        if (file.length < MAGIC.length + IV_BYTES + TAG_BITS / 8
                || !Arrays.equals(Arrays.copyOf(file, MAGIC.length), MAGIC))
            throw new IOException("The question bank file is not a valid HAINA question bank");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(),
                new GCMParameterSpec(TAG_BITS, Arrays.copyOfRange(file, MAGIC.length, MAGIC.length + IV_BYTES)));
        cipher.updateAAD(MAGIC);
        try {
            return cipher.doFinal(file, MAGIC.length + IV_BYTES, file.length - MAGIC.length - IV_BYTES);
        } catch (AEADBadTagException ex) {
            throw new IOException("The question bank was changed outside the question wizard or is damaged");
        }
    }

    private static void writeAtomically(Path target, byte[] content) throws IOException {
        Path absolute = target.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        Path temp = Files.createTempFile(absolute.getParent(), "ludo-", ".tmp");
        try {
            Files.write(temp, content);
            try { Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING); }
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
