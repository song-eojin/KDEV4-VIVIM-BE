package com.welcommu.moduleinfra.admininquiry;

import com.welcommu.moduledomain.admininquiry.AdminInquiry;
import com.welcommu.moduledomain.user.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminInquiryRepository extends JpaRepository<AdminInquiry, Long>,
    AdminInquiryRepositoryCustom {

    List<AdminInquiry> findAllByDeletedAtIsNull();

    List<AdminInquiry> findByCreatorAndDeletedAtIsNull(User user);
}
