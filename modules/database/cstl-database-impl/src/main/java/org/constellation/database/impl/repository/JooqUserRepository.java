/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.constellation.database.impl.repository;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import static com.examind.database.api.jooq.Tables.CSTL_USER;
import static com.examind.database.api.jooq.Tables.USER_X_ROLE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.constellation.dto.UserWithRole;
import org.constellation.dto.CstlUser;

import com.examind.database.api.jooq.Tables;
import static com.examind.database.api.jooq.Tables.ROLE;
import com.examind.database.api.jooq.tables.records.CstlUserRecord;
import com.examind.database.api.jooq.tables.records.RoleRecord;
import com.examind.database.api.jooq.tables.records.UserXRoleRecord;
import org.constellation.repository.UserRepository;
import org.jooq.*;
import org.jooq.Record;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component("cstlUserRepository")
@DependsOn("database-initer")
public class JooqUserRepository extends
        AbstractJooqRespository<CstlUserRecord, com.examind.database.api.jooq.tables.pojos.CstlUser> implements
        UserRepository {

    private final static Logger LOGGER = Logger.getLogger("org.constellation.database.impl.repository");

    public JooqUserRepository() {
        super(com.examind.database.api.jooq.tables.pojos.CstlUser.class, CSTL_USER);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Integer create(UserWithRole user) {
        CstlUserRecord record = dsl.newRecord(CSTL_USER);
        // encode password
        String pwd = user.getPassword();
        user.setPassword(MD5encode(pwd));
        record.from(user);
        record.store();
        Integer uid =  record.getId();
        setUserRoles(uid, user.getRoles());
        return uid;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void update(UserWithRole user) {
        CstlUserRecord record = dsl.newRecord(CSTL_USER);
        //check password update
        String newPassword = user.getPassword();
        if (newPassword != null && !newPassword.isEmpty()) {
            user.setPassword(MD5encode(newPassword));
        }
        record.from(user);
        dsl.executeUpdate(record);
        setUserRoles(user.getId(), user.getRoles());
    }

    @Override
    public boolean existsById(Integer id) {
        return dsl.selectCount().from(CSTL_USER)
                .where(CSTL_USER.ID.eq(id))
                .fetchOne(0, Integer.class) > 0;
    }

    /**
     * This method remove current user roles and add new roles to user
     *
     * @param userId
     * @param roles
     */
    public void setUserRoles(Integer userId, List<String> roles) {
        //remove old roles
        dsl.delete(USER_X_ROLE).where(USER_X_ROLE.USER_ID.eq(userId)).execute();

        //set new roles
        for (String role : roles) {
            addRoleIfMissing(role);
            UserXRoleRecord record = dsl.newRecord(USER_X_ROLE);
            record.setUserId(userId);
            record.setRole(role);
            record.store();
        }
    }

    private void addRoleIfMissing(String role) {
        if (dsl.selectCount().from(ROLE).where(ROLE.NAME.eq(role)).fetchOneInto(Long.class) == 0) {
            RoleRecord r = dsl.newRecord(ROLE);
            r.setName(role);
            r.store();
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int delete(Integer userId) {
        int deleteRole = deleteRole(userId);
        LOGGER.log(Level.FINER, "Delete {0} role references", deleteRole);

        return dsl.delete(CSTL_USER).where(CSTL_USER.ID.eq(userId)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteAll() {
        dsl.delete(USER_X_ROLE).execute();
        return dsl.delete(CSTL_USER).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int desactivate(int userId) {
        return dsl.update(Tables.CSTL_USER).set(CSTL_USER.ACTIVE, false)
                .where(CSTL_USER.ID.eq(userId)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int activate(int userId) {
        return dsl.update(Tables.CSTL_USER).set(CSTL_USER.ACTIVE, true)
                .where(CSTL_USER.ID.eq(userId)).execute();
    }

    private int deleteRole(int userId) {
        return dsl.delete(USER_X_ROLE).where(USER_X_ROLE.USER_ID.eq(userId))
                .execute();

    }

    @Override
    public boolean isLastAdmin(int userId) {
        Record1<Integer> where = dsl
                .selectCount()
                .from(CSTL_USER)
                .join(USER_X_ROLE)
                .onKey()
                .where(USER_X_ROLE.ROLE.eq("cstl-admin").and(
                        CSTL_USER.ID.ne(userId))).fetchOne();
        return where.value1() == 0;
    }

    @Override
    public List<CstlUser> findAll() {
        return convertListToDto(dsl.select()
                                   .from(CSTL_USER)
                                   .fetchInto(com.examind.database.api.jooq.tables.pojos.CstlUser.class));
    }

    @Override
    public Optional<CstlUser> findOne(String login) {
        if (login == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(convertToDto(dsl.select()
                                                   .from(CSTL_USER)
                                                   .where(CSTL_USER.LOGIN.eq(login))
                                                   .fetchOneInto(com.examind.database.api.jooq.tables.pojos.CstlUser.class)));
    }

    @Override
    public Optional<CstlUser> findById(Integer id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(convertToDto(dsl.select()
                                                   .from(CSTL_USER)
                                                   .where(CSTL_USER.ID.eq(id))
                                                   .fetchOneInto(com.examind.database.api.jooq.tables.pojos.CstlUser.class)));
    }

    @Override
    public Optional<CstlUser> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(convertToDto(dsl.select()
                                                   .from(CSTL_USER)
                                                   .where(CSTL_USER.EMAIL.eq(email))
                                                   .fetchOneInto(com.examind.database.api.jooq.tables.pojos.CstlUser.class)));
    }

    @Override
    public Optional<UserWithRole> findByForgotPasswordUuid(String uuid) {
        Map<CstlUserRecord, Result<Record>> fetchGroups = dsl.select()
                .from(CSTL_USER).leftOuterJoin(Tables.USER_X_ROLE).onKey()
                .where(CSTL_USER.FORGOT_PASSWORD_UUID.eq(uuid)).fetchGroups(CSTL_USER);

        if (fetchGroups.isEmpty()) {
            return Optional.empty();
        }

        List<UserWithRole> users = mapUserWithRole(fetchGroups, true);
        return Optional.of(users.get(0));
    }

    @Override
    public List<String> getRoles(int userId) {
        return dsl.select().from(CSTL_USER).join(Tables.USER_X_ROLE).onKey()
                .where(USER_X_ROLE.USER_ID.eq(userId)).fetch(USER_X_ROLE.ROLE);
    }

    @Override
    public List<UserWithRole> findActivesWithRole() {
        Map<CstlUserRecord, Result<Record>> fetchGroups = dsl.select()
                .from(CSTL_USER).join(Tables.USER_X_ROLE).onKey().where(CSTL_USER.ACTIVE.isTrue())
                .fetchGroups(CSTL_USER);

        return mapUserWithRole(fetchGroups, true);
    }

    /**
     * search pageable on cstl_user
     *
     * @param search
     * @param size
     * @param page
     * @param sortFieldName fieldMapping[0] = table (ex: cstl_user),
     *                      fieldMapping[1] = attribute (e.g name)
     * @param order
     * @return
     */
    @Override
    public List<UserWithRole> search(String search, int size, int page, String sortFieldName, String order, List<String> fields) {
        //prepare sort
        
        List<Field<?>> tableFields;
        if (fields == null || fields.isEmpty()) {
            tableFields = new ArrayList<>(Arrays.asList(CSTL_USER.fields()));
            tableFields.add(Tables.USER_X_ROLE.ROLE);
        } else {
            tableFields = new ArrayList<>();
            for (String field : fields) {
                Field f = getFieldFromString(field);
                if (f != null) {
                    tableFields.add(f);
                }
            }
        }
        SelectConditionStep<Record> baseQuery = dsl.select(tableFields).from(CSTL_USER)
                .leftOuterJoin(USER_X_ROLE).on(CSTL_USER.ID.eq(USER_X_ROLE.USER_ID))
                .where(CSTL_USER.LOGIN.like(getLikePattern(search)));

        // JOOQ does not support orderBy(null)
        SelectLimitStep query = baseQuery;
        if (sortFieldName != null && !sortFieldName.isEmpty()) {
            Field<?> field = getFieldFromString(sortFieldName);
            if (field != null) {
                SortField<?> sortField = field.sort(SortOrder.valueOf(order));
                query = baseQuery.orderBy(sortField);
            }
        }
        Map<CstlUserRecord, Result<Record>> result =
                query.limit(size)
                     .offset((page - 1) * size)
                     .fetchGroups(CSTL_USER);

        return mapUserWithRole(result, tableFields.contains(Tables.USER_X_ROLE.ROLE));
    }

    private Field getFieldFromString(String fieldName) {
        Field<?> result = null;
        if (fieldName != null && !fieldName.isEmpty()) {
            String[] fieldMapping = fieldName.split("\\.");
            if (fieldMapping.length == 2) {
                String tableName = fieldMapping[0], attributeName = fieldMapping[1];
                if ("user_x_role".equals(tableName)) {
                    result = USER_X_ROLE.field(attributeName);
                } else if ("cstl_user".equals(tableName)) {
                    result = CSTL_USER.field(attributeName);
                }

                if (result == null) {
                    LOGGER.warning("unknown table field " + fieldName);
                }
            } else {
                LOGGER.warning("Wrong sort value : " + fieldName + ", expected 'TABLE.ATTRIBUTE'");
            }
        }
        return result;
    }

    @Override
    public long searchCount(String search) {
        return dsl.selectCount().from(CSTL_USER)
                .where(CSTL_USER.LOGIN.like(getLikePattern(search)))
                .fetchOne(0, Long.class);
    }

    @Override
    public Optional<UserWithRole> findOneWithRole(Integer id) {
        Map<CstlUserRecord, Result<Record>> fetchGroups = dsl.select()
                .from(CSTL_USER).leftOuterJoin(Tables.USER_X_ROLE).onKey()
                .where(CSTL_USER.ID.eq(id)).fetchGroups(CSTL_USER);

        if (fetchGroups.isEmpty()) {
            return Optional.empty();
        }

        List<UserWithRole> users = mapUserWithRole(fetchGroups, true);
        return Optional.of(users.get(0));

    }

    @Override
    public Optional<UserWithRole> findOneWithRole(String name) {
        Map<CstlUserRecord, Result<Record>> fetchGroups = dsl.select()
                .from(CSTL_USER).leftOuterJoin(Tables.USER_X_ROLE).onKey()
                .where(CSTL_USER.LOGIN.eq(name)).fetchGroups(CSTL_USER);

        if (fetchGroups.isEmpty()) {
            return Optional.empty();
        }

        List<UserWithRole> users = mapUserWithRole(fetchGroups, true);
        return Optional.of(users.get(0));

    }

    private List<UserWithRole> mapUserWithRole(Map<CstlUserRecord, Result<Record>> records, boolean fetchGroup) {

        List<UserWithRole> ret = new ArrayList<>();
        for (Map.Entry<CstlUserRecord, Result<Record>> e : records.entrySet()) {
            UserWithRole userWithRole = e.getKey().into(UserWithRole.class);
            // empty password
            userWithRole.setPassword(null);
            if (fetchGroup) {
                List<String> roles = e.getValue().getValues(Tables.USER_X_ROLE.ROLE);
                userWithRole.setRoles(roles);
            }
            ret.add(userWithRole);
        }
        return ret;
    }

    @Override
    public Optional<UserWithRole> findOneWithRoleByMail(String mail) {
        Map<CstlUserRecord, Result<Record>> fetchGroups = dsl.select()
                .from(CSTL_USER).leftOuterJoin(Tables.USER_X_ROLE).onKey()
                .where(CSTL_USER.EMAIL.eq(mail)).fetchGroups(CSTL_USER);

        if (fetchGroups.isEmpty()) {
            return Optional.empty();
        }
        List<UserWithRole> users = mapUserWithRole(fetchGroups, true);
        return Optional.of(users.get(0));
    }

    @Override
    public long countUser() {
        return dsl.selectCount().from(CSTL_USER).fetchOne(0, long.class);
    }

    @Override
    public boolean loginAvailable(String login) {
        return dsl.selectCount().from(CSTL_USER)
                .where(CSTL_USER.LOGIN.eq(login)).fetchOne().value1() == 0;
    }

    private String getLikePattern(String foo) {
        if (foo == null || foo.isEmpty()) {
            return "%";
        }
        return "%" + foo + "%";
    }

    private CstlUser convertToDto(com.examind.database.api.jooq.tables.pojos.CstlUser dao) {
        if (dao != null) {
            return new CstlUser(dao.getId(),
                    dao.getLogin(),
                    null, // hide pwd
            dao.getFirstname(),
            dao.getLastname(),
            dao.getEmail(),
            dao.getActive(),
            dao.getAvatar(),
            dao.getZip(),
            dao.getCity(),
            dao.getCountry(),
            dao.getPhone(),
            dao.getForgotPasswordUuid(),
            dao.getAddress(),
            dao.getAdditionalAddress(),
            dao.getCivility(),
            dao.getTitle(),
            dao.getLocale());
        }
        return null;
    }

    private List<CstlUser> convertListToDto(List<com.examind.database.api.jooq.tables.pojos.CstlUser> daos) {
        List<CstlUser> results = new ArrayList<>();
        for (com.examind.database.api.jooq.tables.pojos.CstlUser dao : daos) {
            results.add(convertToDto(dao));
        }
        return results;
    }

    /**
     * Encode the specified string with MD5 algorithm.
     *
     * @param key   the string to encode.
     * @return the value (string) hexadecimal on 32 bits
     */
    public static String MD5encode(final String key) {

        final byte[] uniqueKey = key.getBytes();
        byte[] hash = null;
        try {
            // we get an object allowing to crypt the string
            hash = MessageDigest.getInstance("MD5").digest(uniqueKey);

        } catch (NoSuchAlgorithmException e) {
            throw new Error("no MD5 support in this VM");
        }
        final StringBuffer hashString = new StringBuffer();
        for (int i = 0; i < hash.length; ++i) {
            final String hex = Integer.toHexString(hash[i]);
            if (hex.length() == 1) {
                hashString.append('0');
                hashString.append(hex.charAt(hex.length() - 1));
            } else {
                hashString.append(hex.substring(hex.length() - 2));
            }
        }
        return hashString.toString();
    }
}
