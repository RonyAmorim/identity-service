package br.com.puc.identity_service.services;

import br.com.puc.identity_service.exceptions.ExternalServiceException;
import jakarta.ws.rs.core.Response;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KeycloakAdminService {

    private final UsersResource usersResource;

    public KeycloakAdminService(
            @Value("${keycloak.auth-server-url}") String serverUrl,
            @Value("${keycloak.admin-realm}")   String adminRealm,
            @Value("${keycloak.realm}")         String userRealm,
            @Value("${keycloak.resource}")      String clientId,
            @Value("${keycloak.credentials.secret}") String clientSecret
    ) {
        Keycloak kc = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(adminRealm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();

        this.usersResource = kc.realm(userRealm).users();
    }

    /**
     * Cria um usuário no Keycloak (ou retorna o ID se já existir) e seta a senha.
     */
    public String createUser(String username, String email, String rawPassword) {
        // 1) Se já existe, retorna o ID
        List<UserRepresentation> found = usersResource.search(username, 0, 1);
        if (!found.isEmpty()) {
            return found.get(0).getId();
        }

        UserRepresentation rep = new UserRepresentation();
        rep.setUsername(username);
        rep.setEmail(email);
        rep.setEnabled(true);

        // 2) Criação
        try (Response r = usersResource.create(rep)) {
            int status = r.getStatus();
            if (status != Response.Status.CREATED.getStatusCode()) {
                throw new ExternalServiceException(
                        "Falha ao criar usuário Keycloak: HTTP " + status,
                        null
                );
            }
            String location = r.getHeaderString("Location");
            String userId = location.substring(location.lastIndexOf('/') + 1);

            // 3) Seta senha
            CredentialRepresentation cred = new CredentialRepresentation();
            cred.setType(CredentialRepresentation.PASSWORD);
            cred.setValue(rawPassword);
            cred.setTemporary(false);
            usersResource.get(userId).resetPassword(cred);

            return userId;
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException(
                    "Erro ao chamar Keycloak para criar usuário", e
            );
        }
    }

    /**
     * Remove um usuário do Keycloak.
     */
    public void deleteUser(String userId) {
        try (Response r = usersResource.delete(userId)) {
            int status = r.getStatus();
            if (status != Response.Status.NO_CONTENT.getStatusCode()) {
                throw new ExternalServiceException(
                        "Falha ao deletar usuário Keycloak: HTTP " + status,
                        null
                );
            }
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException(
                    "Erro ao chamar Keycloak para deletar usuário", e
            );
        }
    }
}
