package model;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Application-level administrator authentication with PBKDF2 password hashing. */
public final class AdminAuth {
    private static final int ITERATIONS = 160000;
    private static final int KEY_BITS = 256;
    private final File file;
    public AdminAuth(File file) { this.file = file; }

    public boolean isInitialized() { return file.isFile(); }

    public void initialize(String username, char[] password) throws Exception {
        if (isInitialized()) throw new IllegalStateException("Administrator already initialized");
        if (username == null || username.trim().isEmpty() || password == null || password.length < 8)
            throw new IllegalArgumentException("Username and password (at least 8 characters) required");
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] hash = derive(password, salt, ITERATIONS);
        Document document = secureFactory().newDocumentBuilder().newDocument();
        Element root = document.createElement("administrator");
        root.setAttribute("username", username.trim());
        root.setAttribute("algorithm", "PBKDF2WithHmacSHA256");
        root.setAttribute("iterations", Integer.toString(ITERATIONS));
        root.setAttribute("salt", Base64.getEncoder().encodeToString(salt));
        root.setAttribute("hash", Base64.getEncoder().encodeToString(hash));
        document.appendChild(root);
        saveDocument(document, file);
    }

    public boolean authenticate(String username, char[] password) throws Exception {
        if (!isInitialized() || username == null || password == null) return false;
        Document document = secureFactory().newDocumentBuilder().parse(file);
        Element root = document.getDocumentElement();
        if (!"administrator".equals(root.getTagName()) ||
                !"PBKDF2WithHmacSHA256".equals(root.getAttribute("algorithm")) ||
                !root.getAttribute("username").equals(username.trim())) return false;
        int iterations = Integer.parseInt(root.getAttribute("iterations"));
        if (iterations < 10000 || iterations > 1000000) return false;
        byte[] salt = Base64.getDecoder().decode(root.getAttribute("salt"));
        byte[] expected = Base64.getDecoder().decode(root.getAttribute("hash"));
        return MessageDigest.isEqual(expected, derive(password, salt, iterations));
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        finally { spec.clearPassword(); }
    }

    public static DocumentBuilderFactory secureFactory() throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setFeature("http://xml.org/sax/features/external-general-entities", false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        f.setXIncludeAware(false);
        f.setExpandEntityReferences(false);
        return f;
    }

    static void saveDocument(Document document, File destination) throws Exception {
        Path target = destination.toPath().toAbsolutePath();
        Files.createDirectories(target.getParent());
        Path temp = Files.createTempFile(target.getParent(), "ludo-", ".tmp");
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            try (java.io.OutputStream output = Files.newOutputStream(temp)) {
                transformer.transform(new DOMSource(document), new StreamResult(output));
            }
            try { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temp); }
    }
}
