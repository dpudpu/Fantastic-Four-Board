package project.ffboard.service;

import project.ffboard.dto.Comment;

import java.util.List;

public interface CommentService {
    public int addComment(Comment comment);
    public int deleteComment(Long id);
    public int updateComment(Comment comment);
    public List<Comment> getCommentList(Long articleId);
}