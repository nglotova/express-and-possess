package ca.glotov.expresspossess.contact;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    long countBySenderIdAndCreatedAtAfter(Long senderId, Instant since);

    List<ContactMessage> findTop100ByOrderByCreatedAtDesc();
}
