package org.constellation.database.impl.repository;

import java.util.List;
import org.constellation.dto.Role;
import com.examind.database.api.jooq.tables.records.RoleRecord;
import org.constellation.repository.RoleRepository;
import org.springframework.stereotype.Component;


import static com.examind.database.api.jooq.Tables.ROLE;
import org.springframework.context.annotation.DependsOn;

/**
 * Created with IntelliJ IDEA.
 * User: laurent
 * Date: 07/05/15
 * Time: 14:25
 * Geomatys
 */
@Component("cstlRoleRepository")
@DependsOn("database-initer")
public class JooqRoleRepository extends AbstractJooqRespository<RoleRecord, Role> implements RoleRepository{

    public JooqRoleRepository() {
        super(Role.class, ROLE);
    }

    @Override
    public List<Role> findAll() {
        return dsl.select().from(ROLE).fetchInto(Role.class);
    }
}
