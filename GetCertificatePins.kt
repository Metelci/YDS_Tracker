package com.mtlc.studyplan.security

import android.util.Log
import java.net.URL
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection

/**
 * Utility to get real certificate pins for network security configuration
 * Run this in debug mode to get actual certificate hashes
 */
object CertificatePinRetriever {
    
    private const val TAG = "CertificatePinRetriever"
    
    /**
     * Get certificate pins for all configured domains
     */
    fun getCertificatePins() {
        val domains = listOf(
            "ais.osym.gov.tr",
            "www.osym.gov.tr", 
            "cdn.jsdelivr.net",
            "fonts.googleapis.com",
            "fonts.gstatic.com"
        )
        
        domains.forEach { domain ->
            try {
                getCertificatePinForDomain(domain)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get certificate for $domain: ${e.message}")
            }
        }
    }
    
    /**
     * Get certificate pin for a specific domain
     */
    private fun getCertificatePinForDomain(domain: String) {
        val url = URL("https://$domain")
        val connection = url.openConnection() as HttpsURLConnection
        
        try {
            connection.connect()
            
            val certs = connection.serverCertificates
            if (certs.isNotEmpty() && certs[0] is X509Certificate) {
                val cert = certs[0] as X509Certificate
                
                // Calculate SHA-256 hash of the certificate
                val sha256 = MessageDigest.getInstance("SHA-256")
                val certHash = sha256.digest(cert.encoded)
                val base64Hash = android.util.Base64.encodeToString(certHash, android.util.Base64.NO_WRAP)
                
                Log.i(TAG, "=== Certificate for $domain ===")
                Log.i(TAG, "SHA-256 Pin: $base64Hash")
                Log.i(TAG, "Subject: ${cert.subjectX500Principal}")
                Log.i(TAG, "Issuer: ${cert.issuerX500Principal}")
                Log.i(TAG, "Valid From: ${cert.notBefore}")
                Log.i(TAG, "Valid To: ${cert.notAfter}")
                Log.i(TAG, "")
                
                // Also log in a format that's easy to copy
                Log.w(TAG, "COPY THIS PIN FOR $domain:")
                Log.w(TAG, "\"sha256/$base64Hash\"")
                Log.w(TAG, "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting certificate for $domain", e)
        } finally {
            connection.disconnect()
        }
    }
    
    /**
     * Get just the pin for a specific domain (simplified version)
     */
    fun getPinForDomain(domain: String): String? {
        return try {
            val url = URL("https://$domain")
            val connection = url.openConnection() as HttpsURLConnection
            
            connection.connect()
            val certs = connection.serverCertificates
            
            if (certs.isNotEmpty() && certs[0] is X509Certificate) {
                val cert = certs[0] as X509Certificate
                val sha256 = MessageDigest.getInstance("SHA-256")
                val certHash = sha256.digest(cert.encoded)
                android.util.Base64.encodeToString(certHash, android.util.Base64.NO_WRAP)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get pin for $domain", e)
            null
        }
    }
}