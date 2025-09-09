//package com.fitian.burntz.domain.auth.oauth;
//
//import com.nimbusds.jose.JOSEObjectType;
//import com.nimbusds.jose.JWSAlgorithm;
//import com.nimbusds.jose.JWSHeader;
//import com.nimbusds.jose.JWSSigner;
//import com.nimbusds.jose.crypto.ECDSASigner;
//import com.nimbusds.jwt.JWTClaimsSet;
//import com.nimbusds.jwt.SignedJWT;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.*;
//import org.springframework.stereotype.Component;
//import org.springframework.util.LinkedMultiValueMap;
//import org.springframework.util.MultiValueMap;
//import org.springframework.web.client.RestTemplate;
//
//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.nio.charset.StandardCharsets;
//import java.security.KeyFactory;
//import java.security.PrivateKey;
//import java.security.interfaces.ECPrivateKey;
//import java.security.spec.PKCS8EncodedKeySpec;
//import java.time.Instant;
//import java.util.Date;
//import java.util.Map;
//
//@Component
//@RequiredArgsConstructor
//public class AppleTokenService {
//
//    @Value("${spring.security.oauth2.client.registration.apple.client-id}")
//    private String clientId;
//
//    @Value("${apple.team-id}")
//    private String teamId;
//
//    @Value("${apple.key-id}")
//    private String keyId;
//
//    @Value("${apple.private-key-path}")
//    private String privateKeyPath;
//
//    private static final String TOKEN_URL = "https://appleid.apple.com/auth/token";
//
//    public Map<String, Object> exchangeCodeForTokens(String code, String redirectUri) {
//        try {
//            String clientSecret = buildClientSecret();
//
//            RestTemplate rest = new RestTemplate();
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//
//            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//            params.add("grant_type", "authorization_code");
//            params.add("code", code);
//            params.add("redirect_uri", redirectUri);
//            params.add("client_id", clientId);
//            params.add("client_secret", clientSecret);
//
//            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
//
//            ResponseEntity<Map> resp = rest.exchange(TOKEN_URL, HttpMethod.POST, request, Map.class);
//            return resp.getBody();
//        } catch (Exception ex) {
//            throw new RuntimeException("Failed to exchange code for Apple tokens", ex);
//        }
//    }
//
//    private String readAll(String path) throws Exception {
//        try (BufferedReader br = new BufferedReader(new FileReader(path, StandardCharsets.UTF_8))) {
//            StringBuilder sb = new StringBuilder();
//            String line;
//            while ((line = br.readLine()) != null) sb.append(line).append('\n');
//            return sb.toString();
//        }
//    }
//
//    private String buildClientSecret() throws Exception {
//        // .p8 파일(PKCS#8) 읽어서 PrivateKey 생성
//        String pem = readAll(privateKeyPath);
//        String normalized = pem
//                .replace("-----BEGIN PRIVATE KEY-----", "")
//                .replace("-----END PRIVATE KEY-----", "")
//                .replaceAll("\\s", "");
//        byte[] pkcs8 = java.util.Base64.getDecoder().decode(normalized);
//
//        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pkcs8);
//        KeyFactory kf = KeyFactory.getInstance("EC");
//        PrivateKey priv = kf.generatePrivate(spec);
//        if (!(priv instanceof ECPrivateKey)) throw new IllegalArgumentException("Apple private key is not EC private key");
//        ECPrivateKey ecPrivateKey = (ECPrivateKey) priv;
//
//        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
//                .keyID(keyId)
//                .type(JOSEObjectType.JWT)
//                .build();
//
//        Instant now = Instant.now();
//        JWTClaimsSet claims = new JWTClaimsSet.Builder()
//                .issuer(teamId)
//                .issueTime(Date.from(now))
//                .expirationTime(Date.from(now.plusSeconds(60 * 60 * 24 * 180))) // 180일(적절히 조정)
//                .audience("https://appleid.apple.com")
//                .subject(clientId)
//                .build();
//
//        SignedJWT signedJWT = new SignedJWT(header, claims);
//        JWSSigner signer = new ECDSASigner(ecPrivateKey);
//        signedJWT.sign(signer);
//        return signedJWT.serialize();
//    }
//}