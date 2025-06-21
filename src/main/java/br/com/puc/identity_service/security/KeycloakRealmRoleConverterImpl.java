package br.com.puc.identity_service.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;

public class KeycloakRealmRoleConverterImpl implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS = "realm_access";
    private static final String RESOURCE_ACCESS = "resource_access";
    private static final String ROLES = "roles";
    private static final String GROUPS = "groups";
    private static final String PREFIX = "ROLE_";
    // o clientId exato que você configurou no Keycloak
    private static final String CLIENT = "identity-service";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // 1) Realm roles
        Map<?,?> realm = jwt.getClaimAsMap(REALM_ACCESS);
        if (realm != null && realm.containsKey(ROLES)) {
            authorities.addAll(extract((List<?>) realm.get(ROLES)));
        }

        // 2) Client roles
        Map<String, Object> resourceAccess = jwt.getClaimAsMap(RESOURCE_ACCESS);
        if (resourceAccess != null && resourceAccess.containsKey(CLIENT)) {
            var clientMap = (Map<?,?>) resourceAccess.get(CLIENT);
            if (clientMap.containsKey(ROLES)) {
                authorities.addAll(extract((List<?>) clientMap.get(ROLES)));
            }
        }

        // 3) Groups (opcional)
        Collection<String> groups = jwt.getClaimAsStringList(GROUPS);
        if (groups != null) {
            authorities.addAll(extract(groups));
        }

        return authorities;
    }

    private List<GrantedAuthority> extract(Collection<?> roles) {
        List<GrantedAuthority> list = new ArrayList<>();
        for (Object role : roles) {
            list.add(new SimpleGrantedAuthority(PREFIX + role.toString()));
        }
        return list;
    }
}
