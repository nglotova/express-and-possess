package ca.glotov.expresspossess.groups;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {

    Optional<GroupMember> findByIdGroupIdAndIdUserId(Long groupId, Long userId);

    @Query("select new ca.glotov.expresspossess.groups.MemberView(u.id, u.name, u.email, m.role) "
            + "from GroupMember m, ca.glotov.expresspossess.auth.User u "
            + "where m.id.groupId = :groupId and u.id = m.id.userId order by m.role, u.name")
    List<MemberView> findMembers(@Param("groupId") Long groupId);

    List<GroupMember> findByIdGroupId(Long groupId);
}
