package com.wakilfly.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "system_settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = "key")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "`key`", nullable = false, unique = true, length = 128)
    private String key;

    @Column(name = "value", nullable = false, length = 512)
    private String value;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
