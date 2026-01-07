package com.nxq.perform.stackoverflow.controller;

import com.nxq.perform.stackoverflow.entity.sql.Badges;
import com.nxq.perform.stackoverflow.service.BadgesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/badges")
public class BadgesController {

    @Autowired
    private BadgesService badgesService;

    @GetMapping
    public List<Badges> getAllBadges() {
        return badgesService.getAllBadges();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Badges> getBadgeById(@PathVariable Integer id) {
        return badgesService.getBadgeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Badges createBadge(@RequestBody Badges badge) {
        return badgesService.createBadge(badge);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Badges> updateBadge(@PathVariable Integer id, @RequestBody Badges badge) {
        Badges updatedBadge = badgesService.updateBadge(id, badge);
        if (updatedBadge != null) {
            return ResponseEntity.ok(updatedBadge);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBadge(@PathVariable Integer id) {
        badgesService.deleteBadge(id);
        return ResponseEntity.ok().build();
    }
}
