package com.git_crawler.backend.repository;

import com.git_crawler.backend.entity.NotificationOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationOutboxRepository
        extends JpaRepository<NotificationOutbox, UUID> {

    List<NotificationOutbox> findTop50ByStatusOrderByCreatedAtAsc(
            String status
    );
}