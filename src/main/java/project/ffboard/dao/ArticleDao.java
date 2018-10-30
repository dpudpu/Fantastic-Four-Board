package project.ffboard.dao;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import project.ffboard.dto.Article;
import project.ffboard.dto.ArticleContent;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ArticleDao {
    private NamedParameterJdbcTemplate jdbc;
    private JdbcTemplate originJdbc;
    private SimpleJdbcInsert insertAction;

    public ArticleDao(DataSource dataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
        this.originJdbc = new JdbcTemplate(dataSource);
        this.insertAction = new SimpleJdbcInsert(dataSource).withTableName("article").usingGeneratedKeyColumns("id","is_deleted","regdate");

    }

    public int addArticle(Article article) {
        SqlParameterSource params = new BeanPropertySqlParameterSource(article);
        int result = insertAction.execute(params);
        String sql = "UPDATE article SET group_id=(SELECT LAST_INSERT_ID()) WHERE id=(SELECT LAST_INSERT_ID())";
        originJdbc.execute(sql);
        return result;
    }

    public int addArticleContent(ArticleContent articleContent) {
        SqlParameterSource params = new BeanPropertySqlParameterSource(articleContent);
        int result = insertAction.withTableName("article_content").execute(params);
        return result;
    }

    public int updateCount(Long id){
        String sql = "UPDATE article SET hit = hit + 1 WHERE id = :id";
        Map<String, Long> map = Collections.singletonMap("id", id);
        return jdbc.update(sql, map);
    }

    public int deleteArticle(Long id) {
        String sql = "UPDATE article SET is_deleted=TRUE WHERE id = :id";
        Map<String, Long> map = Collections.singletonMap("id",id);
        return jdbc.update(sql, map);
    }

    public int updateArticle(Article article) {
        String sql = "UPDATE article SET title = :title, upddate = :upddate " +
                "WHERE id = :id";
        SqlParameterSource params = new BeanPropertySqlParameterSource(article);
        return jdbc.update(sql, params);
    }

    public int updateArticleContent(ArticleContent articleContent) {
        String sql = "UPDATE article_content SET content = :content WHERE article_id = :article_id";
        SqlParameterSource params = new BeanPropertySqlParameterSource(articleContent);
        return jdbc.update(sql, params);
    }

    public Article getArticle(Long id) {
        String sql = "SELECT id,title,hit,nick_name,group_id,depth_level,group_seq,regdate,"
                +"upddate,category_id,ip_address,member_id,is_deleted "
                +"FROM article WHERE id=:id";
        try{
            RowMapper<Article> rowMapper = BeanPropertyRowMapper.newInstance(Article.class);
            Map<String, Long> params = Collections.singletonMap("id", id);
            return jdbc.queryForObject(sql, params, rowMapper);
        }catch(DataAccessException e){
            return null;
        }
    }

    public ArticleContent getArticleContent(Long id) {
        String sql = "SELECT article_id, content FROM article_content WHERE article_id=:articleId";

        try {
            RowMapper<ArticleContent> rowMapper = BeanPropertyRowMapper.newInstance(ArticleContent.class);
            Map<String, Long> params = Collections.singletonMap("articleId", id);
            return jdbc.queryForObject(sql, params, rowMapper);
        } catch (DataAccessException e) {
            return null;
        }
    }


//    public List<Article> getArticleList(int categoryId, int start, int limit){
//        String sql = "SELECT id, article_id, nick_name, content, group_id, depth_level, group_seq, " +
//                "regdate, upddate, ip_address, member_id, is_deleted FROM comment WHERE article_id=:articleId";
//        Map<String, Object> map = Collections.singletonMap("articleId", articleId);
//        RowMapper<Comment> rowMapper = BeanPropertyRowMapper.newInstance(Comment.class);
//        List<Comment> comments = jdbc.query(sql, map, rowMapper);
//
//        return comments;
//    }

    public List<Article> getArticleList(int categoryId, int start, int limit) {
        String sql = "SELECT id,title,hit,nick_name,group_id,depth_level,group_seq,regdate,"
                +"upddate,category_id,ip_address,member_id,is_deleted FROM article WHERE category_id=:categoryId "
                +"ORDER BY group_id DESC, group_seq ASC LIMIT :start , :limit";
        RowMapper<Article> rowMapper =  BeanPropertyRowMapper.newInstance(Article.class);

        Map<String, Integer> params = new HashMap();
        params.put("categoryId", Integer.valueOf(categoryId));
        params.put("start", Integer.valueOf(start));
        params.put("limit", Integer.valueOf(limit));
        try {
            return jdbc.query(sql,params,rowMapper);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @param categoryId 검색을 원하는 카테고리의 index id
     * @param start 검색을 시작할 인덱스
     * @param limit 검색 리스트의 한도 갯수
     * @param searchType 검색타입으로 "제목","내용","이름","제목+내용"만을 받는다.
     * @param searchWord 사용자가 입력한 검색어
     * @return
     */
    public List<Article> getArticleList(int categoryId, int start, int limit, String searchType, String searchWord) {
        searchWord = "%" + searchWord + "%";
        RowMapper<Article> rowMapper =  BeanPropertyRowMapper.newInstance(Article.class);
        String sql = "SELECT art.id,art.title, art.hit,art.nick_name, art.group_id, art.depth_level, art.group_seq, "
                +"art.regdate, art.upddate, art.category_id, art.ip_address, art.member_id, art.is_deleted, artcon.content "
                +"FROM article art LEFT OUTER JOIN article_content artcon ON art.id = artcon.article_id  WHERE art.category_id=:categoryId AND ";

        if (searchType.equals("제목")) {
            sql += "art.title LIKE :searchWord ";
        } else if (searchType.equals("내용")) {
            sql += "artcon.content LIKE :searchWord ";
        } else if (searchType.equals("이름")) {
            sql += "art.nick_name LIKE :searchWord ";
        } else if (searchType.equals("제목+내용")) {
            sql += "art.title LIKE :searchWord OR artcon.content LIKE :searchWord ";
        } else {
            return null;
        }

        sql+="ORDER BY art.group_id DESC, art.group_seq ASC LIMIT :start , :limit";

        Map<String, Object> params = new HashMap();
        params.put("categoryId", Integer.valueOf(categoryId));
        params.put("start", Integer.valueOf(start));
        params.put("limit", Integer.valueOf(limit));
        params.put("searchWord", searchWord);

        try {
            return jdbc.query(sql,params,rowMapper);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}