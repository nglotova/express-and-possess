package ca.glotov.expresspossess.groups;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

interface InvitationEmailRepository extends JpaRepository<InvitationEmail, Long> {

    long countBySenderIdAndSentAtAfter(Long senderId, Instant since);
}
