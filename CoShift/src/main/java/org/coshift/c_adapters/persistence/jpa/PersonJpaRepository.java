package org.coshift.c_adapters.persistence.jpa;

import org.coshift.a_domain.person.Person;
import org.coshift.b_application.ports.PersonRepository;
import org.coshift.d_frameworks.db.PersonJpaEntity;
import org.coshift.d_frameworks.db.PersonSpringRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Primary
public class PersonJpaRepository implements PersonRepository {

    private final PersonSpringRepository repo;

    public PersonJpaRepository(PersonSpringRepository repo) {
        this.repo = repo;
    }

    @Override
    public Person save(Person person) {
        PersonJpaEntity saved = repo.save(PersonJpaMapper.toEntity(person));
        return PersonJpaMapper.toDomain(saved);
    }

    @Override
    public Optional<Person> findById(Long id) {
        return repo.findById(id).map(PersonJpaMapper::toDomain);
    }

    @Override
    public Optional<Person> findByNickname(String nickname) {
        return repo.findByNicknameIgnoreCase(nickname).map(PersonJpaMapper::toDomain);
    }

    @Override
    public List<Person> findAll() {
        return repo.findAll().stream()
                .map(PersonJpaMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
    }
}