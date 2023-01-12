package com.schemarepository.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;

@Entity
@Table(name = "manuals")
@Getter
@Setter
@NoArgsConstructor
public class Manual {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_id")
    private Type type;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Column(name = "model_name", nullable = false)
    @NotBlank
    private String modelName;

    @Column(name = "filepath", nullable = false)
    @NotBlank
    private String filePath;

    public Manual(Type type) {
        this.type = type;
    }

    public Manual(Type type, Brand brand) {
        this.type = type;
        this.brand = brand;
    }
}
