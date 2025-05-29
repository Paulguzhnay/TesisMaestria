package ec.edu.ups.Backend.repository;

import ec.edu.ups.Backend.model.OdooInstance;
import ec.edu.ups.Backend.model.User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OdooInstanceRepository extends JpaRepository<OdooInstance, Long> {
    List<OdooInstance> findByProjectId(Long projectId);
    List<OdooInstance> findAllByNameAndProjectId(String name, Long projectId);
    List<OdooInstance> findByProjectName(String projectName);
    List<OdooInstance> findByProjectUserUsername(String username);
    @Transactional
    void deleteByNameAndCategory(String name, String category);

    Optional<OdooInstance> findAllByNameAndCategory(String name, String category);
}
