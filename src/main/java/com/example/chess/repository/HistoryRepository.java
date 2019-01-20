package com.example.chess.repository;

import com.example.chess.entity.History;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoryRepository extends CrudRepository<History, Long> {

	List<History> findByGameIdAndPositionLessThanEqualOrderByPositionAsc(long gameId, int position);
}
