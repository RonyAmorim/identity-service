package br.com.puc.identity_service.services;

import br.com.puc.identity_service.entities.UserEntity;
import br.com.puc.identity_service.exceptions.*;
import br.com.puc.identity_service.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final KeycloakAdminService keycloakAdminService;

    public UserService(UserRepository userRepository,
                       KeycloakAdminService keycloakAdminService) {
        this.userRepository       = userRepository;
        this.keycloakAdminService = keycloakAdminService;
    }

    @Transactional
    public UserEntity registerNewUser(String username,
                                      String email,
                                      String rawPassword,
                                      String fullName) {
        // 1) valida duplicatas locais
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("Username já está em uso: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("E-mail já registrado: " + email);
        }

        // 2) cria no Keycloak
        String keycloakId;
        try {
            keycloakId = keycloakAdminService.createUser(username, email, rawPassword);
        } catch (RuntimeException ex) {
            throw new UserRegistrationException("Falha ao criar usuário no Keycloak", ex);
        }

        // 3) persiste localmente
        UserEntity user = UserEntity.builder()
                .keycloakId(keycloakId)
                .username(username)
                .email(email)
                .fullName(fullName)
                .build();
        return userRepository.save(user);
    }

    public List<UserEntity> findAll() {
        return userRepository.findAll();
    }

    public UserEntity findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado: " + id));
    }

    @Transactional
    public UserEntity updateProfile(UUID id, String fullName, String email) {
        UserEntity user = findById(id);

        // 1) valida e-mail novo
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException("E-mail já está em uso: " + email);
        }

        user.setFullName(fullName);
        user.setEmail(email);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(UUID id) {
        UserEntity user = findById(id);

        // opcional: remover no Keycloak
        try {
            keycloakAdminService.deleteUser(user.getKeycloakId());
        } catch (RuntimeException ex) {
            throw new ExternalServiceException(
                    "Falha ao remover usuário no Keycloak: " + user.getKeycloakId(), ex);
        }

        userRepository.delete(user);
    }

    public UserEntity findByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() ->
                        new UserNotFoundException("Usuário não encontrado no Keycloak: " + keycloakId));
    }
}
