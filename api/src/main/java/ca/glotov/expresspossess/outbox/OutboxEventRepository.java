package ca.glotov.expresspossess.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop100ByPublishedAtIsNullOrderById();

    @Modifying
    @Transactional
    @Query("update OutboxEvent e set e.publishedAt = :at where e.id = :id")
    void markPublished(@Param("id") Long id, @Param("at") Instant at);
}
