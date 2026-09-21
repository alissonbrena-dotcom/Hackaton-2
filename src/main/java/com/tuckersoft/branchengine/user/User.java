package com.tuckersoft.branchengine.user;

import com.tuckersoft.branchengine.playthrough.Playthrough;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    /** Hash BCrypt. Nunca sale en un response: los controllers solo devuelven DTOs. */
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 60)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "user")
    private List<Playthrough> playthroughs = new ArrayList<>();
}
