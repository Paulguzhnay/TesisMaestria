package ec.edu.ups.Backend.repository;

import ec.edu.ups.Backend.model.OdooInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OdooInstanceRepository extends JpaRepository<OdooInstance, Long> {
}
