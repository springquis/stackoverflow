package com.nxq.perform.stackoverflow.service;

import com.nxq.perform.stackoverflow.entity.sql.Badges;
import com.nxq.perform.stackoverflow.repository.BadgesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BadgesService {

    @Autowired
    private BadgesRepository badgesRepository;

    public List<Badges> getAllBadges() {
        return badgesRepository.findAll();
    }

    public Optional<Badges> getBadgeById(Integer id) {
        return badgesRepository.findById(id);
    }

    public Badges createBadge(Badges badge) {
        return badgesRepository.save(badge);
    }

    public Badges updateBadge(Integer id, Badges badgeDetails) {
        return badgesRepository.findById(id).map(badge -> {
            badge.setName(badgeDetails.getName());
            badge.setUserId(badgeDetails.getUserId());
            badge.setDate(badgeDetails.getDate());
            return badgesRepository.save(badge);
        }).orElse(null);
    }

    public void deleteBadge(Integer id) {
        badgesRepository.deleteById(id);
    }
}
