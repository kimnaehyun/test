package com.gt.codinnator.domain.editor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Table(name="file")
public class FileNode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fileId;

    @Column(nullable = false)
    @Setter
    private String fileName;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private FileNode parentId;

    @OneToMany(mappedBy = "parentId", cascade = CascadeType.ALL)
    private List<FileNode> childList = new ArrayList<>();

    @Column(nullable = false)
    private Long roomId;

    @Builder
    public FileNode(String fileName, String filePath, String type, FileNode parentId, Long roomId){
        this.fileName = fileName;
        this.filePath = filePath;
        this.type = type;
        this.parentId = parentId;
        this.roomId = roomId;
    }
}
