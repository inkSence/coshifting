package org.coshift.c_adapters.persistence.jpa;

import org.coshift.a_domain.person.Person;
import org.coshift.d_frameworks.db.PersonJpaEntity;

public final class PersonJpaMapper {

    private PersonJpaMapper() {
    }

    public static PersonJpaEntity toEntity(Person person) {
        PersonJpaEntity entity = new PersonJpaEntity();
        if (person.getId() > 0) {
            entity.setId(person.getId());
        }
        entity.setNickname(person.getNickname());
        entity.setPassword(person.getPassword());
        entity.setTimeAccountId(person.getTimeAccountId());
        entity.setRole(person.getRole());
        return entity;
    }

    public static Person toDomain(PersonJpaEntity entity) {
        long id = entity.getId() == null ? 0 : entity.getId();
        return new Person(
            id,
            entity.getNickname(),
            entity.getPassword(),
            entity.getTimeAccountId(),
            entity.getRole()
        );
    }
}