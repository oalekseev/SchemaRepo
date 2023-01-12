package com.schemarepository.model;

import lombok.*;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.Calendar;

@Entity
@Table(name = "audit")
@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
public class AuditRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "time")
    @NonNull
    private Calendar time;

    @Column(name = "action", nullable = false)
    @NotBlank
    @NonNull
    private String action;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    @NonNull
    private User user;

}
