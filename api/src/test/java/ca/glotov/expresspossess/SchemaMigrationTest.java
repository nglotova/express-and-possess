package ca.glotov.expresspossess;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfig.class)
class SchemaMigrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void migrationCreatesEveryTableFromTheSpec() {
        List<String> tables = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public' order by 1",
                String.class);

        assertThat(tables).contains(
                "users", "password_reset_tokens",
                "groups", "group_members", "group_invitations",
                "expressions", "comments",
                "notifications", "outbox_events");
    }

    @Test
    void anExpressionCannotBeInProcessWithoutAnImplementer() {
        long creator = insertUser("creator@example.com");
        long group = insertGroup(creator);

        assertThatThrownBy(() -> jdbc.update(
                "insert into expressions (group_id, creator_id, status, description) values (?, ?, 'IN_PROCESS', 'x')",
                group, creator))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theCreatorCannotBeTheImplementer() {
        long creator = insertUser("self@example.com");
        long group = insertGroup(creator);

        assertThatThrownBy(() -> jdbc.update(
                "insert into expressions (group_id, creator_id, implementer_id, status, description) "
                        + "values (?, ?, ?, 'IN_PROCESS', 'x')",
                group, creator, creator))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void emailIsUniqueRegardlessOfCase() {
        insertUser("Same@Example.com");

        assertThatThrownBy(() -> insertUser("same@example.com"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private long insertUser(String email) {
        return jdbc.queryForObject(
                "insert into users (email, password_hash, name) values (?, 'x', 'Test') returning id",
                Long.class, email);
    }

    private long insertGroup(long ownerId) {
        return jdbc.queryForObject(
                "insert into groups (name, owner_id) values ('Family', ?) returning id",
                Long.class, ownerId);
    }
}
