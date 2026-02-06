package com.gt.codinnator.domain.editor.repository;

import com.gt.codinnator.domain.editor.entity.FileNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<FileNode, Long> {
    List<FileNode> findAllByRoomIdAndParentIdIsNull(Long roomId);
    void deleteByRoomId(Long roomId);
}
