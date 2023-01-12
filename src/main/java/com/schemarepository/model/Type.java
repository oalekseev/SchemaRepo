package com.schemarepository.model;

import lombok.*;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;

@Entity
@Table(name = "types")
@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
public class Type {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false)
    @NotBlank
    @NonNull
    private String name;
}
