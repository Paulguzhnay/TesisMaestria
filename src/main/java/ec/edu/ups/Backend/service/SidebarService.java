package ec.edu.ups.Backend.service;

import ec.edu.ups.Backend.model.SidebarItem;
import ec.edu.ups.Backend.repository.SidebarRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SidebarService {

    private final SidebarRepository sidebarRepository;

    public SidebarService(SidebarRepository sidebarRepository) {
        this.sidebarRepository = sidebarRepository;
    }

    public List<SidebarItem> getSidebarItems() {
        return sidebarRepository.findAllByOrderByPositionAsc();
    }

    public List<SidebarItem> saveAll(List<SidebarItem> items) {
        return sidebarRepository.saveAll(items);
    }

    public void saveSidebarItems(List<SidebarItem> items) {
        sidebarRepository.saveAll(items);
    }
}