package com.gt.codinnator.domain.room.repository;


import com.gt.codinnator.domain.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    // 기본 CRUD 메서드가 자동으로 생김
}