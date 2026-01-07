package com.nxq.perform.stackoverflow.service;

import com.nxq.perform.stackoverflow.entity.sql.Comment;
import com.nxq.perform.stackoverflow.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    public List<Comment> getAllComments() {
        return commentRepository.findAll();
    }

    public Optional<Comment> getCommentById(Integer id) {
        return commentRepository.findById(id);
    }

    public Comment createComment(Comment comment) {
        return commentRepository.save(comment);
    }

    public Comment updateComment(Integer id, Comment commentDetails) {
        return commentRepository.findById(id).map(comment -> {
            comment.setText(commentDetails.getText());
            comment.setScore(commentDetails.getScore());
            comment.setCreationDate(commentDetails.getCreationDate());
            comment.setPostId(commentDetails.getPostId());
            comment.setUserId(commentDetails.getUserId());
            return commentRepository.save(comment);
        }).orElse(null);
    }

    public void deleteComment(Integer id) {
        commentRepository.deleteById(id);
    }
}
