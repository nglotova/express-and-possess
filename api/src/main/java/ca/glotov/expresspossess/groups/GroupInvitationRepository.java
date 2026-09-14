package ca.glotov.expresspossess.groups;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupInvitationRepository extends JpaRepository<GroupInvitation, Long> {

    Optional<GroupInvitation> findByTokenHash(String tokenHash);

    List<GroupInvitation> findByGroupIdAndAcceptedAtIsNull(Long groupId);

    Optional<GroupInvitation> findByGroupIdAndEmailIgnoreCaseAndAcceptedAtIsNull(Long groupId, String email);
}
