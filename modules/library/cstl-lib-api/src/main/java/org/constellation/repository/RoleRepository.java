package org.constellation.repository;

import org.constellation.dto.Role;

import java.util.List;

/**
 *
 */
public interface RoleRepository {

    List<Role> findAll();
}
