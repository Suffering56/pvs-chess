package com.example.chess.repository;

import com.example.chess.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRepository  extends JpaRepository<Game, Long> {
}
