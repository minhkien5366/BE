package com.example.cinema.repository;

import com.example.cinema.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);

    // Tìm kiếm theo số điện thoại
    Optional<User> findByMobileNumber(String mobileNumber);
    boolean existsByMobileNumber(String mobileNumber);

    // Tìm kiếm nâng cao: Theo Tên, Email hoặc Số điện thoại (Hỗ trợ phân trang)
    @Query("SELECT u FROM User u WHERE " +
           "(:keyword IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
           "(:keyword IS NULL OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
           "(:keyword IS NULL OR u.email LIKE CONCAT('%', :keyword, '%')) OR " +
           "(:keyword IS NULL OR u.mobileNumber LIKE CONCAT('%', :keyword, '%'))")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);
}