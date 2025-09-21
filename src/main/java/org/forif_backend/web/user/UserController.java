package org.forif_backend.web.user;


import lombok.RequiredArgsConstructor;
import org.forif_backend.application.user.UserService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;


}
