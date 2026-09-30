package com.ecommerce.identity.controller;

import com.ecommerce.identity.dto.ApiResponse;
import com.ecommerce.identity.dto.UserProfileResponse;
import com.ecommerce.identity.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * GET /api/v1/users/me - Lấy thông tin profile.
     * Header X-User-Id được inject bởi API Gateway sau khi verify JWT.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {

        if (userIdHeader == null || userIdHeader.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "Missing X-User-Id header"));
        }

        Long userId = Long.parseLong(userIdHeader);
        UserProfileResponse profile = authService.getUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin thành công", profile));
    }
}
