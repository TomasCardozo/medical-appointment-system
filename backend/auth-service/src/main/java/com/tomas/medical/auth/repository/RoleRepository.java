package com.tomas.medical.auth.repository;

import com.tomas.medical.auth.entity.Role;
import com.tomas.medical.auth.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
