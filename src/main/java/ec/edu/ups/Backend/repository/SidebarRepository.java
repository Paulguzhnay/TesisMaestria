package ec.edu.ups.Backend.repository;

import ec.edu.ups.Backend.model.SidebarItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SidebarRepository extends JpaRepository<SidebarItem, Long> {
    List<SidebarItem> findAllByOrderByPositionAsc();
}