package com.schemarepository.model;

import lombok.*;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Calendar;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "chat_id", unique = true, nullable = false)
    @NotNull
    private Long chatId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "first_name")
    @NotBlank
    private String firstName;

    @Column(name = "last_name")
    @NotBlank
    private String lastName;

    @Column(name = "v_card")
    private String vCard;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "created_time")
    private Calendar createdTime;

    @Column(name = "registered_time")
    private Calendar registeredTime;

    @Column(name = "status")
    private String status;

    public User(Long chatId, String userName, String firstName, String lastName, String vCard, String phoneNumber, Calendar createdTime, Calendar registeredTime, String status) {
        this.chatId = chatId;
        this.userName = userName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.vCard = vCard;
        this.phoneNumber = phoneNumber;
        this.createdTime = createdTime;
        this.registeredTime = registeredTime;
        this.status = status;
    }
}