package br.com.puc.identity_service.controllers;

import br.com.puc.identity_service.entities.UserEntity;
import br.com.puc.identity_service.services.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public record RegisterRequest(
            @NotBlank String username,
            @NotBlank @Email   String email,
            @NotBlank String password,
            @NotBlank String fullName
    ) {}

    public record UpdateRequest(
            @NotBlank String fullName,
            @NotBlank @Email String email
    ) {}

    /**
     * 1. Registrar novo usuário
     */
    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequest req) {
        UserEntity created = userService.registerNewUser(
                req.username(),
                req.email(),
                req.password(),
                req.fullName()
        );
        URI location = URI.create("/api/users/" + created.getId());
        return ResponseEntity.created(location).build();
    }

    /**
     * 2. Listar todos os usuários
     */
    @GetMapping
    public ResponseEntity<List<UserEntity>> listAll() {
        List<UserEntity> all = userService.findAll();
        return ResponseEntity.ok(all);
    }

    /**
     * 3. Buscar usuário por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserEntity> getById(@PathVariable UUID id) {
        UserEntity user = userService.findById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * 4. Atualizar perfil de usuário
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserEntity> update(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateRequest req
    ) {
        UserEntity updated = userService.updateProfile(
                id,
                req.fullName(),
                req.email()
        );
        return ResponseEntity.ok(updated);
    }

    /**
     * 5. Deletar usuário (local + Keycloak)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 6. Dados do usuário autenticado
     */
    @GetMapping("/me")
    public ResponseEntity<UserEntity> me(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        UserEntity me = userService.findByKeycloakId(keycloakId);
        return ResponseEntity.ok(me);
    }
}
