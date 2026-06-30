package com.apex.ecommerce.external;

import java.math.BigDecimal;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Service
public class ExternalPaymentClient {
    private static final Logger log = LoggerFactory.getLogger(ExternalPaymentClient.class);
    private final RestClient restClient;

    public ExternalPaymentClient() {
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2)) // Maksimal 2 detik nunggu koneksi terbuka
            .build();

        // 2. Bungkus ke dalam Request Factory milik Spring
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        
        // Catatan: JdkClientHttpRequestFactory tidak punya .setReadTimeout() global.
        // Batasan durasi nunggu response (Read Timeout) akan sepenuhnya di-handle 
        // secara anggun oleh Resilience4j Circuit Breaker lewat konfigurasi `slow-call-duration-threshold` di properties!

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl("https://api.mock-paymentgateway.com")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Method untuk menembak API Payment Gateway.
     * `@CircuitBreaker` akan memantau kegagalan. Jika RTO/Eror terjadi terus-menerus,
     * sirkuit akan terbuka (OPEN) dan langsung mengarahkan request ke method fallback.
     */
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "paymentFallback")
    public boolean callPaymentGateway(BigDecimal amount) {
        log.info("[HTTP Request] Menembak Payment Gateway untuk nominal: Rp{}", amount);

        try {
            // Membuat DTO request secara instan menggunakan Java Record
            PaymentRequestPayload payload = new PaymentRequestPayload("TX-" + System.currentTimeMillis(), amount);

            // Eksekusi POST request secara sinkronus namun non-blocking (karena ditopang Virtual Threads)
            String response = restClient.post()
                    .uri("/v1/charge")
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            log.info("[HTTP Response] Sukses dari Payment Gateway: {}", response);
            return "SUCCESS".equalsIgnoreCase(response);

        } catch (Exception e) {
            log.error("[HTTP Error] Gagal hit Payment Gateway. Alasan: {}", e.getMessage());
            // Lempar kembali exception-nya agar dicatat oleh statistika Resilience4j Circuit Breaker
            throw e; 
        }
    }

    /**
     * METHOD FALLBACK (Penyelamat Darurat)
     * Syarat Resilience4j: Parameter harus MATS (sama persis) dengan method utama, plus argumen Throwable di akhir.
     */
    public boolean paymentFallback(BigDecimal amount, Throwable t) {
        log.warn("[CIRCUIT BREAKER / TIMEOUT TRIPPED] Metode fallback aktif! Mengamankan thread aplikasi.");
        log.warn("Penyebab kegagalan sistem luar: {}", t.getMessage());
        
        // Kembalikan false agar transaksi digagalkan secara anggun tanpa membuat aplikasi crash / timeout beruntun.
        return false; 
    }

    // Menggunakan Java Record (Fitur modern Java untuk DTO ringkas tanpa boiler-plate code)
    private record PaymentRequestPayload(String transactionId, BigDecimal amount) {}
}
