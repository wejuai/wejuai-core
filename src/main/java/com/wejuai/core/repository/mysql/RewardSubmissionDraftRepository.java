package com.wejuai.core.repository.mysql;

import com.wejuai.entity.mysql.RewardSubmissionDraft;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardSubmissionDraftRepository extends JpaRepository<RewardSubmissionDraft, String> {

    Page<RewardSubmissionDraft> findByUser_Id(String userId, Pageable pageable);
}
