package com.welcommu.moduleinfra.projectpost;

import com.welcommu.moduledomain.projectpost.ProjectPostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectPostCommentRepository extends JpaRepository<ProjectPostComment, Long> {

    List<ProjectPostComment> findAllByProjectPostIdAndDeletedAtIsNull(Long projectPostId);
}
