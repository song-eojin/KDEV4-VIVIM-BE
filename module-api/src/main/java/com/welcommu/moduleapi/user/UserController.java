package com.welcommu.moduleapi.user;

import com.welcommu.modulecommon.dto.ApiResponse;
import com.welcommu.moduledomain.auth.AuthUserDetailsImpl;
import com.welcommu.moduledomain.company.CompanyRole;
import com.welcommu.moduledomain.user.User;
import com.welcommu.moduleservice.user.PasswordResetServiceImpl;
import com.welcommu.moduleservice.user.UserService;
import com.welcommu.moduleservice.user.dto.UserModifyRequest;
import com.welcommu.moduleservice.user.dto.UserRequest;
import com.welcommu.moduleservice.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSendException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "유저 API", description = "유저를 생성, 수정, 삭제시킬 수 있습니다.")
public class UserController {

    private final UserService userService;
    private final PasswordResetServiceImpl resetService;

    @PostMapping
    @Operation(summary = "유저를 생성합니다")
    public ResponseEntity<ApiResponse> createUser(@RequestBody UserRequest userRequest, @AuthenticationPrincipal AuthUserDetailsImpl userDetails) {
        log.info("Received user: {}", userRequest);
        Long actorId = userDetails.getUser().getId();
        userService.createUser(userRequest, actorId);
        return ResponseEntity.ok().body(new ApiResponse(HttpStatus.OK.value(), "사용자 생성에 성공했습니다."));
    }

    @GetMapping
    @Operation(summary = "모든 유저를 조회합니다.")
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/search")
    @Operation(summary = "유저 검색 (페이징)")
    public ResponseEntity<Page<UserResponse>> searchUsers(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) String phone,
        @RequestParam(required = false) Long companyId,
        @RequestParam(required = false) CompanyRole companyRole,
        @RequestParam(required = false) Boolean isDeleted,
        @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<UserResponse> results = userService.searchUsers(
            name, email, phone, companyId, companyRole, isDeleted, pageable
        );
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    @Operation(summary = "id를 바탕으로 유저를 개별 조회합니다.")
    public ResponseEntity<ApiResponse> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.getUserById(id);
        if (user.isPresent()) {
            UserResponse response = UserResponse.from(user.get());
            return ResponseEntity.ok(new ApiResponse(200, "User found", response));
        } else {
            ApiResponse apiResponse = new ApiResponse(404, "User not found with id: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
        }
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "email를 바탕으로 유저를 개별 조회합니다.")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        Optional<User> user = userService.getUserByEmail(email);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }


    @GetMapping("/phone/{phone}")
    @Operation(summary = "핸드폰 번호를 바탕으로 유저를 개별 조회합니다.")
    public ResponseEntity<User> getUserByPhone(@PathVariable String phone) {
        Optional<User> user = userService.getUserByPhone(phone);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/resetpassword")
    @Operation(summary = "비밀번호를 잊었을 시 해당 email의 비밀번호를 초기화합니다.")
    public ResponseEntity<ApiResponse> resetPasswordByUserWithoutLogin(@RequestParam String email) {
        if (userService.resetPasswordWithoutLogin(email)) {
            return ResponseEntity.ok(new ApiResponse(200, "Password changed"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(404, "User not found with email: " + email));
        }
    }

    @PutMapping("/modifypassword/{id}")
    @Operation(summary = "id를 바탕으로 비밀번호를 수정합니다.")
    public ResponseEntity<ApiResponse> modifyPasswordByUserWithLogin(@PathVariable Long id,
        @RequestParam String password) {
        if (userService.modifyPassword(id, password)) {
            return ResponseEntity.ok(new ApiResponse(200, "Password changed"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(404, "User not found with id: " + id));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "사용자 정보를 수정합니다.")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
        @RequestBody UserModifyRequest updatedUserRequest,
        @AuthenticationPrincipal AuthUserDetailsImpl userDetails) {
        try {
            log.info("사용자 수정 요청 받음, id=" + id);
            Long actorId = userDetails.getUser().getId();;
            UserResponse response = userService.modifyUser(id, actorId, updatedUserRequest);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.info("사용자 수정 실패: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "유저를 삭제합니다. (Hard Delete")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Long id, @AuthenticationPrincipal AuthUserDetailsImpl userDetails) {
        Long actorId = userDetails.getUser().getId();
        userService.deleteUser(id, actorId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/soft/{id}")
    @Operation(summary = "유저를 삭제합니다. (Soft Delete")
    public ResponseEntity<ApiResponse> softDeleteUser(@PathVariable Long id) {
        userService.softDeleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/request-reset")
    public ResponseEntity<?> requestReset(@RequestParam String email){
        try {
            resetService.requestReset(email);
            return ResponseEntity.ok(Map.of("message", "인증번호 발송 완료"));
        } catch (UsernameNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
        } catch (MailSendException ex) {
            log.error("메일 발송 실패", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", ex.getMessage()));
        }
    }

    @PutMapping("/confirm-reset")
    public ResponseEntity<?> confirmReset(
        @RequestParam String email,
        @RequestParam String code,
        @RequestParam String newPassword
    ) {
        boolean ok = resetService.confirmReset(email, code, newPassword);
        if (ok) {
            return ResponseEntity.ok(Map.of("message", "비밀번호 변경 완료"));
        } else {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "인증번호가 올바르지 않거나 만료되었습니다."));
        }
    }
}
