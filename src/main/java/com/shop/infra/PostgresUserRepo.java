package com.shop.infra;

import com.shop.application.UserRepository;
import com.shop.domain.Role;
import com.shop.domain.User;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Repository
public class PostgresUserRepo implements UserRepository {
    private static final String SELECT_USER_BY_ID =
            "SELECT u.id, u.username, u.password_hash, ur.role_name " + //lấy id,username,pass,role tu bảng user và userrole
            "FROM users u " +
            "LEFT JOIN user_roles ur ON ur.user_id = u.id " +
            "WHERE u.id = :id " +
            "ORDER BY ur.role_name";

    private static final String SELECT_USER_BY_NAME =
            "SELECT u.id, u.username, u.password_hash, ur.role_name " +
            "FROM users u " +
            "LEFT JOIN user_roles ur ON ur.user_id = u.id " +
            "WHERE u.username = :username " +
            "ORDER BY ur.role_name";

    private static final String UPDATE_USER_SQL =
            "UPDATE users " +
            "SET username = :username, " +
            "    updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = :id";

    private static final String INSERT_USER_SQL =
            "INSERT INTO users (id, username, password_hash) " +
            "VALUES (:id, :username, :passwordHash)";

    private static final String UPDATE_PASSWORD_SQL =
            "UPDATE users " +
            "SET password_hash = :passwordHash, " +
            "    updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = :id";

    private static final String INSERT_ROLE_SQL =
            "INSERT INTO user_roles (user_id, role_name) " +
            "VALUES (:userId, :roleName) " +
            "ON CONFLICT (user_id, role_name) DO NOTHING";

    private final DatabaseClient databaseClient;

    public PostgresUserRepo(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Boolean> existsByID(String id) { // phương thư kiểm tra xem user có tồn tại ko
        if (!StringUtils.hasText(id)) { // ktra xem id có bị r
            return Mono.just(false);
        }

        return databaseClient.sql("SELECT EXISTS (SELECT 1 FROM users WHERE id = :id) AS user_exists")
                .bind("id", id)
                .map((row, metadata) -> Boolean.TRUE.equals(row.get("user_exists", Boolean.class)))
                .one()
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<User> getByID(String id) {
        if (!StringUtils.hasText(id)) {
            return Mono.empty();
        }

        return databaseClient.sql(SELECT_USER_BY_ID)
                .bind("id", id)
                .map((row, metadata) -> new UserRow(
                        row.get("id", String.class),
                        row.get("username", String.class),
                        row.get("password_hash", String.class),
                        row.get("role_name", String.class)
                ))
                .all()
                .collectList()
                .flatMap(this::mapUser);
    }

    @Override
    public Mono<User> getByName(String name) {
        if (!StringUtils.hasText(name)) {
            return Mono.empty();
        }

        return databaseClient.sql(SELECT_USER_BY_NAME)
                .bind("username", name)
                .map((row, metadata) -> new UserRow(
                        row.get("id", String.class),
                        row.get("username", String.class),
                        row.get("password_hash", String.class),
                        row.get("role_name", String.class)
                ))
                .all()
                .collectList()
                .flatMap(this::mapUser);
    }

    @Override
    @Transactional
    public Mono<Void> saveUser(User user) {
        if (user == null || !StringUtils.hasText(user.getId()) || !StringUtils.hasText(user.username)) {
            return Mono.error(new IllegalArgumentException("user is invalid"));
        }

        Mono<Void> updateUser = databaseClient.sql(UPDATE_USER_SQL)
                .bind("id", user.getId())
                .bind("username", user.username)
                .fetch()
                .rowsUpdated()
                .then();

        if (user.getRoles().isEmpty()) {
            return updateUser;
        }

        return updateUser.then(replaceRoles(user.getId(), user.getRoles()));
    }

    @Override
    @Transactional
    public Mono<Void> deleteByID(String id) {
        if (!StringUtils.hasText(id)) {
            return Mono.empty();
        }

        return databaseClient.sql("DELETE FROM users WHERE id = :id")
                .bind("id", id)
                .fetch()
                .rowsUpdated()
                .then();
    }

    @Override
    @Transactional
    public Mono<Void> newUser(User user, String password) {
        if (user == null || !StringUtils.hasText(user.getId()) || !StringUtils.hasText(user.username)) {
            return Mono.error(new IllegalArgumentException("user is invalid"));
        }
        if (!StringUtils.hasText(password)) {
            return Mono.error(new IllegalArgumentException("password hash is invalid"));
        }

        Set<Role> rolesToPersist = user.getRoles().isEmpty() ? Collections.singleton(Role.USER) : user.getRoles();
        if (user.getRoles().isEmpty()) {
            user.addRole(Role.USER);
        }

        return databaseClient.sql(INSERT_USER_SQL)
                .bind("id", user.getId())
                .bind("username", user.username)
                .bind("passwordHash", password)
                .fetch()
                .rowsUpdated()
                .then(insertRoles(user.getId(), rolesToPersist));
    }

    @Override
    @Transactional
    public Mono<Void> changePassword(String id, String password) {
        if (!StringUtils.hasText(id) || !StringUtils.hasText(password)) {
            return Mono.error(new IllegalArgumentException("id or password hash is invalid"));
        }

        return databaseClient.sql(UPDATE_PASSWORD_SQL)
                .bind("id", id)
                .bind("passwordHash", password)
                .fetch()
                .rowsUpdated()
                .then();
    }

    private Mono<User> mapUser(List<UserRow> rows) {
        if (rows.isEmpty()) {
            return Mono.empty();
        }

        UserRow firstRow = rows.get(0);
        User user = new User(firstRow.id(), firstRow.username());
        user.passwordHash = firstRow.passwordHash();

        for (UserRow row : rows) {
            if (row.roleName() != null) {
                user.addRole(Role.valueOf(row.roleName()));
            }
        }

        return Mono.just(user);
    }

    private Mono<Void> replaceRoles(String userId, Set<Role> roles) {
        return databaseClient.sql("DELETE FROM user_roles WHERE user_id = :userId")
                .bind("userId", userId)
                .fetch()
                .rowsUpdated()
                .then(insertRoles(userId, roles));
    }

    private Mono<Void> insertRoles(String userId, Set<Role> roles) {
        return Flux.fromIterable(roles)
                .concatMap(role -> databaseClient.sql(INSERT_ROLE_SQL)
                        .bind("userId", userId)
                        .bind("roleName", role.name())
                        .fetch()
                        .rowsUpdated())
                .then();
    }

    private static final class UserRow {
        private final String id;
        private final String username;
        private final String passwordHash;
        private final String roleName;

        private UserRow(String id, String username, String passwordHash, String roleName) {
            this.id = id;
            this.username = username;
            this.passwordHash = passwordHash;
            this.roleName = roleName;
        }

        private String id() {
            return id;
        }

        private String username() {
            return username;
        }

        private String passwordHash() {
            return passwordHash;
        }

        private String roleName() {
            return roleName;
        }
    }
}
