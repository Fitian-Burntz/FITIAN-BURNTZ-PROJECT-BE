package com.fitian.burntz.domain.auth.oauth;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * 사용법(로컬 실행):
 *  - 필요한 값: TEAM_ID, CLIENT_ID(=Service ID), KEY_ID, PRIVATE_KEY_P8_PATH
 *  - 예: mvn -q exec:java -Dexec.mainClass="com.fitian.burntz.domain.auth.oauth2.AppleClientSecretGenerator" \
 *         -Dexec.args="TEAM_ID CLIENT_ID KEY_ID /path/to/AuthKey_ABCDEF1234.p8"
 *
 * 출력: client_secret (compact JWT) 를 stdout 로 출력합니다. (application.properties에 복붙하거나 CI에 주입)
 */
public class AppleClientSecretGenerator {

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: AppleClientSecretGenerator <TEAM_ID> <CLIENT_ID> <KEY_ID> <PRIVATE_KEY_P8_PATH>");
            System.exit(2);
        }
        String teamId = args[0];
        String clientId = args[1];
        String keyId = args[2];
        Path p8Path = Path.of(args[3]);

        String jwt = generateClientSecret(teamId, clientId, keyId, Files.readString(p8Path));
        System.out.println(jwt);
    }

    /**
     * @param teamId Apple Team ID
     * @param clientId Service ID (Client ID)
     * @param keyId  Key ID (from Apple dev portal)
     * @param privateKeyPemContent content of .p8 file (PKCS#8)
     * @return compact signed JWT (ES256)
     */
    public static String generateClientSecret(String teamId,
                                              String clientId,
                                              String keyId,
                                              String privateKeyPemContent) throws Exception {

        ECPrivateKey privateKey = loadECPrivateKeyFromPKCS8Pem(privateKeyPemContent);

        // header
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .keyID(keyId)
                .type(JOSEObjectType.JWT)
                .build();

        Instant now = Instant.now();
        // Apple: 만료기간은 최대 6개월(권장: 6개월 이하)
        Instant exp = now.plusSeconds(60L * 60L * 24L * 160L); // 예: 160일 (운영에서 적절히 조정)

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(teamId)                     // iss
                .issueTime(Date.from(now))          // iat
                .expirationTime(Date.from(exp))     // exp
                .audience("https://appleid.apple.com") // aud
                .subject(clientId)                  // sub (client id)
                .build();

        SignedJWT signedJWT = new SignedJWT(header, claims);
        JWSSigner signer = new ECDSASigner(privateKey);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    private static ECPrivateKey loadECPrivateKeyFromPKCS8Pem(String pem) throws GeneralSecurityException, IOException {
        // remove headers
        String normalized = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] der = Base64.getDecoder().decode(normalized);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(der);
        KeyFactory kf = KeyFactory.getInstance("EC");
        PrivateKey pk = kf.generatePrivate(keySpec);
        if (!(pk instanceof ECPrivateKey)) {
            throw new IllegalArgumentException("Provided key is not EC private key");
        }
        return (ECPrivateKey) pk;
    }
}
