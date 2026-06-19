package com.library.library_backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "library_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibraryConfig {

    @Id
    @Column(name = "config_key", length = 100)
    private String configKey;

    @Column(name = "config_value", length = 500, nullable = false)
    private String configValue;

    @Column(name = "description")
    private String description;

    @Column(name = "updated_at", insertable = false)
    private OffsetDateTime updatedAt;
}
