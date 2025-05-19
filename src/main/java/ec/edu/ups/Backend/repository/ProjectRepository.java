package ec.edu.ups.Backend.repository;

import ec.edu.ups.Backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ec.edu.ups.Backend.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByUser(User user);

}
