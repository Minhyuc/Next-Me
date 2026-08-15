package com.nm.characterservice.Repository;

import com.nm.characterservice.Entity.OwnedCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OwnedCharacterRepository extends JpaRepository<OwnedCharacter, Long> {
    List<OwnedCharacter> findAllByUserIdOrderByCharacterNumberAsc(Long userId);
}
