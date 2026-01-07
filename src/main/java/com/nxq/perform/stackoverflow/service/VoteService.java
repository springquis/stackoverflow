package com.nxq.perform.stackoverflow.service;

import com.nxq.perform.stackoverflow.entity.sql.Vote;
import com.nxq.perform.stackoverflow.repository.VoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VoteService {

    @Autowired
    private VoteRepository voteRepository;

    public List<Vote> getAllVotes() {
        return voteRepository.findAll();
    }

    public Optional<Vote> getVoteById(Integer id) {
        return voteRepository.findById(id);
    }

    public Vote createVote(Vote vote) {
        return voteRepository.save(vote);
    }

    public Vote updateVote(Integer id, Vote voteDetails) {
        return voteRepository.findById(id).map(vote -> {
            vote.setPostId(voteDetails.getPostId());
            vote.setUserId(voteDetails.getUserId());
            vote.setBountyAmount(voteDetails.getBountyAmount());
            vote.setVoteTypeId(voteDetails.getVoteTypeId());
            vote.setCreationDate(voteDetails.getCreationDate());
            return voteRepository.save(vote);
        }).orElse(null);
    }

    public void deleteVote(Integer id) {
        voteRepository.deleteById(id);
    }
}
