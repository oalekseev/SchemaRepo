package com.schemarepository.repository;

import com.schemarepository.model.FeedbackRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface JpaFeedbackRepository extends JpaRepository<FeedbackRecord, Integer> {

}
