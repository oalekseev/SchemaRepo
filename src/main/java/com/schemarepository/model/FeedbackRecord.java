package com.schemarepository.model;

import lombok.*;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.Calendar;

@Entity
@Table(name = "feedback")
@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
public class FeedbackRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "time")
    @NonNull
    private Calendar time;

    @Column(name = "message", nullable = false)
    @NotBlank
    @NonNull
    private String message;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    @NonNull
    private User user;

}
