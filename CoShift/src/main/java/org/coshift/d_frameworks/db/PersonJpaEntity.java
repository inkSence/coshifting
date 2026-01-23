package org.coshift.d_frameworks.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.coshift.a_domain.person.PersonRole;

/**
 * 1-zu-1-Abbild der DB-Tabelle "persons".
 * JPA-Annotationen sind hier lokalisiert,
 * Domain-Code bleibt davon unberührt.
 */
@Entity
@Table(name = "persons")
public class PersonJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column(nullable = false)
    private String password;

    @Column(name = "time_account_id")
    private Long timeAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PersonRole role;

    public PersonJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getTimeAccountId() {
        return timeAccountId;
    }

    public void setTimeAccountId(Long timeAccountId) {
        this.timeAccountId = timeAccountId;
    }

    public PersonRole getRole() {
        return role;
    }

    public void setRole(PersonRole role) {
        this.role = role;
    }
}