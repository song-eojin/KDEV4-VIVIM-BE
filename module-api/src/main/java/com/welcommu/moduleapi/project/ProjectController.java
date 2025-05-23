package com.welcommu.moduleapi.project;

import com.welcommu.modulecommon.dto.ApiResponse;
import com.welcommu.moduledomain.auth.AuthUserDetailsImpl;
import com.welcommu.moduledomain.project.Project;
import com.welcommu.moduleservice.project.ProjectService;
import com.welcommu.moduleservice.project.dto.DashboardInspectionCountResponse;
import com.welcommu.moduleservice.project.dto.DashboardProgressCountResponse;
import com.welcommu.moduleservice.project.dto.DashboardProjectFeeResponse;
import com.welcommu.moduleservice.project.dto.ProjectAdminSummaryResponse;
import com.welcommu.moduleservice.project.dto.ProjectCompanyResponse;
import com.welcommu.moduleservice.project.dto.ProjectCreateRequest;
import com.welcommu.moduleservice.project.dto.ProjectDeleteRequest;
import com.welcommu.moduleservice.project.dto.ProjectModifyRequest;
import com.welcommu.moduleservice.project.dto.ProjectMonthlyStats;
import com.welcommu.moduleservice.project.dto.ProjectSnapshot;
import com.welcommu.moduleservice.project.dto.ProjectSummaryWithRoleDto;
import com.welcommu.moduleservice.project.dto.ProjectUserResponse;
import com.welcommu.moduleservice.project.dto.ProjectUserSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects")
@Tag(name = "프로젝트 API", description = "프로젝트를 생성, 수정, 삭제시킬 수 있습니다.")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @Operation(summary = "프로젝트 생성")
    public ResponseEntity<ApiResponse> createProject(
        @AuthenticationPrincipal AuthUserDetailsImpl userDetails,
        @RequestBody ProjectCreateRequest dto
    ) {
        Long userId = userDetails.getUser().getId();
        projectService.createProject(dto, userId);

        return ResponseEntity.ok()
            .body(new ApiResponse(HttpStatus.CREATED.value(), "프로젝트가 생성되었습니다."));
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "프로젝트 개별 조회")
    public ResponseEntity<Project> readProject(@PathVariable Long projectId,
        @AuthenticationPrincipal AuthUserDetailsImpl userDetails
    ) {
        Project project = projectService.getProject(userDetails.getUser(), projectId);
        return ResponseEntity.ok(project);
    }

    @GetMapping("/projects/stats/monthly")
    @Operation(summary = "최근 6개월별 생성된 프로젝트 수, 완료된 프로젝트 수 리스트 반환")
    public List<ProjectMonthlyStats> getMonthlyProjectStats() {
        return projectService.getMonthlyProjectStats();
    }

    @GetMapping("/non-completed/sorted")
    @Operation(summary = "마감일 순으로 정렬된 프로젝트 리스트 반환")
    public List<ProjectSnapshot> getNonCompletedProjectsOrderedByEndDate() {
        return projectService.getNonCompletedProjectsOrderedByEndDate();
    }

    @PutMapping("/{projectId}")
    @Operation(summary = "프로젝트 수정")
    public ResponseEntity<ApiResponse> modifyProject(
        @PathVariable Long projectId,
        @AuthenticationPrincipal AuthUserDetailsImpl userDetails,
        @RequestBody ProjectModifyRequest dto
    ) {
        Long userId = userDetails.getUser().getId();
        projectService.modifyProject(projectId, dto, userId);
        return ResponseEntity.ok().body(new ApiResponse(HttpStatus.OK.value(), "프로젝트가 수정되었습니다."));
    }

    @GetMapping()
    @Operation(summary = "특정 유저 소속 프로젝트 조회")
    public ResponseEntity<List<ProjectUserSummaryResponse>> readProjects(
        @RequestParam Long userId) {
        List<ProjectUserSummaryResponse> projects = projectService.getProjectsByUser(userId);
        return ResponseEntity.ok(projects);
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "프로젝트 삭제")
    public ResponseEntity<ApiResponse> deleteProject(
        @PathVariable Long projectId,
        @AuthenticationPrincipal AuthUserDetailsImpl userDetails,
        @RequestBody ProjectDeleteRequest dto
    ) {
        Long userId = userDetails.getUser().getId();
        projectService.deleteProject(projectId, userId);
        return ResponseEntity.ok().body(new ApiResponse(HttpStatus.OK.value(), "프로젝트가 삭제되었습니다."));
    }

    @GetMapping("/all")
    @Operation(summary = "프로젝트 전체 조회")
    public ResponseEntity<List<ProjectAdminSummaryResponse>> readAllProjectsForAdmin() {
        List<ProjectAdminSummaryResponse> projects = projectService.getProjectList();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/search")
    @Operation(summary = "프로젝트 검색 (페이징)")
    public ResponseEntity<Page<ProjectAdminSummaryResponse>> searchProjects(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) Boolean isDeleted,
        @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ProjectAdminSummaryResponse> results = projectService.searchProjects(
            name, description, isDeleted, pageable
        );
        return ResponseEntity.ok(results);
    }

    @Operation(summary = "프로젝트 소속 유저 조회")
    @GetMapping("/{projectId}/users")
    public ResponseEntity<List<ProjectUserResponse>> readProjectUsers(
        @PathVariable Long projectId) {
        List<ProjectUserResponse> projects = projectService.getUserListByProject(projectId);
        return ResponseEntity.ok(projects);
    }

    // ProjectController.java
    @GetMapping("/company")
    @Operation(summary = "내 회사 소속 프로젝트 전체 조회")
    public ResponseEntity<List<ProjectSummaryWithRoleDto>> readAllMyCompanyProjects(
        @AuthenticationPrincipal AuthUserDetailsImpl userDetails) {
        Long companyId = userDetails.getUser().getCompany().getId();
        return ResponseEntity.ok(
            projectService.getCompanyProjectsWithMyRole(companyId, userDetails.getUser()
                .getId()));
    }

    @GetMapping("/{projectId}/companies")
    @Operation(summary = "프로젝트 소속 회사 목록 조회")
    public ResponseEntity<List<ProjectCompanyResponse>> getProjectCompanies(
        @PathVariable Long projectId
    ) {
        List<ProjectCompanyResponse> responses = projectService.getCompaniesByProjectId(projectId);
        return ResponseEntity.ok(responses);
    }


    @GetMapping("/dashboard/project_fee")
    @Operation(summary = "이번 달 매출 현황 데이터 조회")
    public ResponseEntity<DashboardProjectFeeResponse> getDashboardProjectFee() {
        return ResponseEntity.ok(projectService.getDashboardProjectFee());
    }

    @GetMapping("/dashboard/inspection_count")
    @Operation(summary = "계약, 검수 개수 조회")
    public ResponseEntity<DashboardInspectionCountResponse> getDashboardInspectionCount() {
        return ResponseEntity.ok(projectService.getDashboardInspectionCount());
    }

    @GetMapping("/dashboard/progress_count")
    @Operation(summary = "프로젝트 단계별 현황 조회")
    public ResponseEntity<DashboardProgressCountResponse> getDashboardProgressCount() {
        return ResponseEntity.ok(projectService.getDashboardProgressCount());
    }

    @PatchMapping("/{projectId}/progress/increase_current_progress")
    @Operation(summary = "프로젝트 단계 승급")
    public ResponseEntity<ApiResponse> increaseCurrentProgress(@PathVariable Long projectId) {
        projectService.increaseCurrentProgress(projectId);
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "프로젝트가 수정되었습니다."));
    }


}
