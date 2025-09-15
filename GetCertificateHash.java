import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import javax.net.ssl.HttpsURLConnection;

public class GetCertificateHash {
    public static void main(String[] args) {
        String[] domains = {
            "ais.osym.gov.tr",
            "www.osym.gov.tr",
            "cdn.jsdelivr.net",
            "fonts.googleapis.com",
            "fonts.gstatic.com"
        };
        
        for (String domain : domains) {
            try {
                System.out.println("=== Certificate for " + domain + " ===");
                
                URL url = new URL("https://" + domain);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                // Get the certificate
                X509Certificate[] certs = (X509Certificate[]) connection.getServerCertificates();
                if (certs.length > 0) {
                    X509Certificate cert = certs[0];
                    
                    // Calculate SHA-256 hash
                    MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                    byte[] certHash = sha256.digest(cert.getEncoded());
                    
                    // Convert to Base64
                    String base64Hash = Base64.getEncoder().encodeToString(certHash);
                    
                    System.out.println("SHA-256 Pin: " + base64Hash);
                    System.out.println("Subject: " + cert.getSubjectX500Principal());
                    System.out.println("Issuer: " + cert.getIssuerX500Principal());
                    System.out.println("Valid From: " + cert.getNotBefore());
                    System.out.println("Valid To: " + cert.getNotAfter());
                } else {
                    System.out.println("No certificates found");
                }
                
                connection.disconnect();
                System.out.println();
                
            } catch (MalformedURLException | NoSuchAlgorithmException | CertificateEncodingException e) {
                System.out.println("Error: " + e.getMessage());
                System.out.println();
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
                System.out.println();
            }
        }
    }
}