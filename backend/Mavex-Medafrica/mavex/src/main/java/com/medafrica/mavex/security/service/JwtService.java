    package com.medafrica.mavex.security.service;

    import com.medafrica.mavex.model.security.User;
    import io.jsonwebtoken.Claims;
    import io.jsonwebtoken.Jwts;
    import io.jsonwebtoken.SignatureAlgorithm;
    import io.jsonwebtoken.security.Keys;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.stereotype.Service;
    import javax.crypto.SecretKey;
    import java.security.Key;
    import java.time.LocalDateTime;
    import java.time.ZoneId;
    import java.util.Date;

    @Service
    public class JwtService {

        @Value("${jwt.secret}")
        private String secretKey;

        /** Durée access token : 15 minutes */
        @Value("${jwt.access-token-expiration:900000}")
        private long accessTokenExpiration;

        /** Génère un access token JWT signé */
        public String generateAccessToken(User user) {
            return Jwts.builder()
                    .setSubject(user.getEmail())
                    .claim("userId", user.getId())
                    .claim("role", user.getRole().name())
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                    .compact();
        }

        /** Extrait l'email (subject) du token */
        public String extractEmail(String token) {
            return extractClaims(token).getSubject();
        }

        /** Vérifie que le token est valide et appartient à cet utilisateur */
        public boolean isTokenValid(String token, User user) {
            try {
                String email = extractEmail(token);
                return email.equals(user.getEmail())
                        && !isTokenExpired(token)
                        && !isIssuedBeforePasswordChange(token, user);
            } catch (Exception e) {
                return false;
            }
        }

        private boolean isTokenExpired(String token) {
            return extractClaims(token).getExpiration().before(new Date());
        }

        /** Rejette un token émis avant le dernier changement de mot de passe (déconnexion forcée). */
        private boolean isIssuedBeforePasswordChange(String token, User user) {
            LocalDateTime passwordChangedAt = user.getPasswordChangedAt();
            if (passwordChangedAt == null) {
                return false; // jamais changé depuis la création → aucune restriction
            }
            Date issuedAt = extractIssuedAt(token);
            Date changedAt = Date.from(passwordChangedAt.atZone(ZoneId.systemDefault()).toInstant());
            return issuedAt.before(changedAt);
        }

        private Date extractIssuedAt(String token) {
            return extractClaims(token).getIssuedAt();
        }

        private Claims extractClaims(String token) {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        }

        private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }
    }
