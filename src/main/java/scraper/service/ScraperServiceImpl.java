package scraper.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import scraper.model.Article;
import scraper.repository.ArticleRepository;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ScraperServiceImpl implements ScraperService {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Logger LOGGER = LoggerFactory.getLogger(ArticleServiceImpl.class);
    private static final Pattern ARTICLE_URL_PATTERN = Pattern.compile("https://www.scmp.com/(.*?)/article/(.[0-9]+?)/(.*?)");
    @Autowired
    private ArticleRepository articleRepository;
    @Value("${scraper.url}")
    private String SCRAPER_URL;

    @Override
    @Scheduled(fixedDelay = 10000)
    public void scrapeArticles() {
        LOGGER.info("Fixed Delay Task. Current Time: " + formatter.format(LocalDateTime.now()));
        articleRepository.saveArticles(getScrapedArticlesToAdd());
    }

    private Article scrapedArticle(SyndEntry entry) {
        String url = entry.getUri();
        String articleId = "";
        Matcher matcher = ARTICLE_URL_PATTERN.matcher(url);
        if (matcher.matches()) {
            articleId = matcher.group(2);
        } else {
            LOGGER.error("Error while parsing URL {}, not valid", url);
        }

        String title = entry.getTitle();
        String publishedDate = entry.getPublishedDate().toString();
        String description = entry.getDescription().getValue();
        String author = entry.getAuthor();

        return new Article(Integer.parseInt(articleId), url, title, publishedDate, description, author);
    }

    @Override
    public List<Article> getScrapedArticlesToAdd() {
        try {
            URL feedUrl = new URL(SCRAPER_URL);
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(feedUrl));
            List<SyndEntry> entries = feed.getEntries();
            return articleRepository.getArticlesToAdd(entries.stream().map(this::scrapedArticle).collect(Collectors.toList()));
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
            return Collections.emptyList();
        }

    }
}
