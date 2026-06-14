package com.bubbles.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartitionMapping {
    private Long id;
    private String tname;
    private String partitionTag;
    private LocalDateTime createdAt;
}
