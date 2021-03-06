package project.ffboard.service;

import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import project.ffboard.dao.ArticleDao;
import project.ffboard.dto.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@PropertySource("classpath:jdbc.properties")
public class ArticleServiceImpl implements ArticleService {
    private int limit = 10; //한페이지에 보여주는 최대 게시글의 갯수
    private ArticleDao articleDao;
    private Environment environment;

    public ArticleServiceImpl(ArticleDao articleDao, Environment environment) {
        this.articleDao = articleDao;
        this.environment = environment;
    }

    @Override
    @Transactional
    public int addArticle(Article article, ArticleContent articleContent, MultipartFile file, ArticleCounting articleCounting) {
        //지금 쓰는 글이 답글인경우 groupSeq를 알맞게 조정
        if (article.getGroupId() != null) {
            article.setDepthLevel(article.getDepthLevel() + 1);
            article.setGroupSeq(article.getGroupSeq() + 1);
            articleDao.arrangeGroupSeq(article.getGroupId(), article.getGroupSeq());
        }

        //article의 기본정보 삽입.
        Long articleId = articleDao.insertArticle(article);
        articleContent.setArticleId(articleId);

        //article이 원글일 경우 GroupId가 null이므로, 삽입해주는 과정.
        if (article.getGroupId() == null) {
            articleDao.insertGroupId();
        }

        //파일이 존재한다면 파일업로드를 진행
        if (!file.isEmpty()) {
            uploadFile(file, articleId);
        }

        //article_couning 해당 카테고리에 글이 없을 때 => count 0
        if (articleDao.getCategoryCount(articleCounting.getCategoryId()) == null) {
            articleCounting.setCount(1L);
            articleDao.insertArticleCount(articleCounting);
        } else {
            articleDao.updateArticleCount(articleCounting);
        }

        return articleDao.insertArticleContent(articleContent);
    }

    public int uploadFile(MultipartFile file, Long articleId) {
        UUID uuid = UUID.randomUUID();
        String uuidStr = uuid.toString();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd");
        String dataStr = simpleDateFormat.format(new Date());

        String baseDir = environment.getProperty("baseDir");
        String saveDir = baseDir + "/" + dataStr;
        String saveFile = saveDir + "/" + uuidStr;


        File fileObj = new File(saveDir);
        fileObj.mkdirs();

        InputStream in = null;
        OutputStream out = null;

        try {
            in = file.getInputStream();
            out = new FileOutputStream(saveFile);
            byte[] buffer = new byte[1024];
            int readCount = 0;
            while ((readCount = in.read(buffer)) != -1) {
                out.write(buffer, 0, readCount);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                }
            }
        }

        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put("article_id", articleId);
        fileInfo.put("origin_name", file.getOriginalFilename());
        fileInfo.put("stored_name", uuidStr);
        fileInfo.put("content_type", file.getContentType());
        fileInfo.put("size", file.getSize());
        fileInfo.put("path", saveDir);

        return articleDao.insertFileInfo(fileInfo);
    }

    public ArticleFile isExistFile(Long articleId) {
        return articleDao.getFileInfo(articleId);
    }

    public void downloadFile(HttpServletResponse response, Long articleId) {
        ArticleFile articleFile = articleDao.getFileInfo(articleId);

        response.setContentLengthLong(articleFile.getSize());
        response.setContentType(articleFile.getContentType());
        response.setContentType("application/x-msdownload");

        try {
            URLDecoder.decode(articleFile.getOriginName(), "ISO8859_1");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        response.setHeader("Content-disposition", "attachment; filename=" + articleFile.getOriginName());

        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(articleFile.getPath() + "/" + articleFile.getStoredName());
            out = response.getOutputStream();
            byte[] buffer = new byte[1024];
            int readCount = 0;
            while ((readCount = in.read(buffer)) != -1) {
                out.write(buffer, 0, readCount);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int getCount(int categoryId, int totalPage, int posts) {
        try {
            totalPage = articleDao.getCount(categoryId);
            totalPage = (totalPage - 1) / posts + 1;
            return totalPage;
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }

    }


    @Override
    @Transactional
    public int updateCount(Long id) {
        return articleDao.increaseHitCount(id);
    }

    @Override
    @Transactional
    public int deleteArticle(Long id) {
        return articleDao.deleteArticle(id);
    }

    @Override
    @Transactional
    public int updateArticle(Article article, ArticleContent articleContent, MultipartFile file) {
        articleContent.setArticleId(articleDao.updateArticle(article));

        if (!file.isEmpty()) {
            uploadFile(file, articleDao.insertArticle(article));
        }
        return articleDao.updateArticleContent(articleContent);
    }

    @Override
    @Transactional
    public Article getArticle(Long id) {
        //조회수 증가 시키기
        articleDao.increaseHitCount(id);

        //article 정보 가져오기
        return articleDao.getArticle(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleContent getArticleContent(Long id) {
        return articleDao.getArticleContent(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Article> getArticleList(int categoryId, int start, int posts) {
        List<Article> articleList = articleDao.getArticleList(categoryId, start, posts);
        return articleList;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Article> getArticleList(int categoryId, int start, String searchType, String searchWord) {
        return articleDao.getArticleList(categoryId, start, limit, searchType, searchWord);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Article> getArticleList(String orderType, int start) {
        return articleDao.getArticleList(orderType, start, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getCategoryList() {
        return articleDao.getCategoryList();
    }
}
