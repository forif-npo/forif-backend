package org.forif_backend.application.user;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.user.UserRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;


}
