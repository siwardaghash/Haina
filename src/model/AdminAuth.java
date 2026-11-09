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

/** Administrator authentication (one account per team member) with PBKDF2 password hashing. */
public final class AdminAuth {
    private static final int ITERATIONS = 160000;
    private static final int KEY_BITS = 256;
    private final File file;
    public AdminAuth(File file) { this.file = file; }

    /** True when at least one administrator exists. */
    public boolean isInitialized() {
        try { return !entries(load()).isEmpty(); }
        catch (Exception ex) { return false; }
    }

    /**
     * Adds an administrator (one per team member). Only the team tool controller.CreateAdmin calls this;
     * the game itself never creates accounts.
     */
    public void addAdministrator(String username, char[] password) throws Exception {
        if (username == null || username.trim().isEmpty() || password == null || password.length < 8)
            throw new IllegalArgumentException("Username and password (at least 8 characters) required");
        Document document = file.isFile() ? load() : null;
        if (document == null || !"administrators".equals(document.getDocumentElement().getTagName())) {
            Document fresh = secureFactory().newDocumentBuilder().newDocument();
            Element root = fresh.createElement("administrators");
            fresh.appendChild(root);
            if (document != null)   // keep an administrator from the old single-account format
                for (Element old : entries(document)) root.appendChild(fresh.importNode(old, true));
            document = fresh;
        }
        for (Element entry : entries(document))
            if (entry.getAttribute("username").equals(username.trim()))
                throw new IllegalArgumentException("Administrator \"" + username.trim() + "\" already exists");
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] hash = derive(password, salt, ITERATIONS);
        Element admin = document.createElement("administrator");
        admin.setAttribute("username", username.trim());
        admin.setAttribute("algorithm", "PBKDF2WithHmacSHA256");
        admin.setAttribute("iterations", Integer.toString(ITERATIONS));
        admin.setAttribute("salt", Base64.getEncoder().encodeToString(salt));
        admin.setAttribute("hash", Base64.getEncoder().encodeToString(hash));
        document.getDocumentElement().appendChild(admin);
        saveDocument(document, file);
    }

    public boolean authenticate(String username, char[] password) throws Exception {
        if (!file.isFile() || username == null || password == null) return false;
        for (Element entry : entries(load())) {
            if (!"PBKDF2WithHmacSHA256".equals(entry.getAttribute("algorithm")) ||
                    !entry.getAttribute("username").equals(username.trim())) continue;
            int iterations = Integer.parseInt(entry.getAttribute("iterations"));
            if (iterations < 10000 || iterations > 1000000) return false;
            byte[] salt = Base64.getDecoder().decode(entry.getAttribute("salt"));
            byte[] expected = Base64.getDecoder().decode(entry.getAttribute("hash"));
            return MessageDigest.isEqual(expected, derive(password, salt, iterations));
        }
        return false;
    }

    private Document load() throws Exception { return secureFactory().newDocumentBuilder().parse(file); }

    /** Works with both the old file (one administrator element) and the new list. */
    private static java.util.List<Element> entries(Document document) {
        java.util.List<Element> list = new java.util.ArrayList<>();
        Element root = document.getDocumentElement();
        if ("administrator".equals(root.getTagName())) list.add(root);
        else {
            org.w3c.dom.NodeList nodes = root.getElementsByTagName("administrator");
            for (int i = 0; i < nodes.getLength(); i++) list.add((Element) nodes.item(i));
        }
        return list;
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
