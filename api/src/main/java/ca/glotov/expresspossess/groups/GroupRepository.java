package ca.glotov.expresspossess.groups;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query("select g from Group g, GroupMember m where m.id.groupId = g.id and m.id.userId = :userId "
            + "and g.status <> ca.glotov.expresspossess.groups.GroupStatus.ARCHIVED order by g.name")
    List<Group> findVisibleTo(@Param("userId") Long userId);

    Optional<Group> findByShareToken(String shareToken);

    /** True once any expression has ever been created in the group: "Working" in the spec. */
    @Query(value = "select exists (select 1 from expressions where group_id = :groupId)", nativeQuery = true)
    boolean hasExpressions(@Param("groupId") Long groupId);
}
