package com.tuckersoft.branchengine.user;

import com.tuckersoft.branchengine.exception.ApiException;
import com.tuckersoft.branchengine.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public UserResponse me() {
        return UserResponse.from(currentUserService.getCurrentUser());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAllByOrderByIdAsc().stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse changeRole(Long id, String roleName) {
        Role role = parseRole(roleName);
        User target = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("No existe el usuario " + id));
        User current = currentUserService.getCurrentUser();
        if (current.getId().equals(target.getId())) {
            throw ApiException.badRequest("Un administrador no puede cambiar su propio rol");
        }
        target.setRole(role);
        return UserResponse.from(userRepository.save(target));
    }

    private Role parseRole(String roleName) {
        return Arrays.stream(Role.values())
                .filter(r -> r.name().equals(roleName))
                .findFirst()
                .orElseThrow(() -> ApiException.badRequest("El rol debe ser ROLE_USER o ROLE_ADMIN"));
    }
}
