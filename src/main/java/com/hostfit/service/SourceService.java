package com.hostfit.service;

import com.hostfit.crawler.CrawlSourceType;
import com.hostfit.crawler.CssSelectorParser;
import com.hostfit.crawler.ParseException;
import com.hostfit.domain.CrawlRun;
import com.hostfit.domain.CrawlSource;
import com.hostfit.repo.CrawlRunRepository;
import com.hostfit.repo.CrawlSourceRepository;
import com.hostfit.repo.ProviderRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin operations on crawl sources and read access to run history. */
@Service
@Transactional(readOnly = true)
public class SourceService {

    private final CrawlSourceRepository sources;
    private final CrawlRunRepository runs;
    private final ProviderRepository providers;

    public SourceService(CrawlSourceRepository sources, CrawlRunRepository runs, ProviderRepository providers) {
        this.sources = sources;
        this.runs = runs;
        this.providers = providers;
    }

    public List<CrawlSource> findAll() {
        return sources.findAllWithProvider();
    }

    public CrawlSource get(Long id) {
        return sources.findWithProviderById(id).orElseThrow(() -> new NotFoundException("Crawl source " + id + " not found"));
    }

    public List<CrawlRun> recentRuns(int limit) {
        return runs.findRecent(PageRequest.of(0, limit));
    }

    public SourceForm toForm(CrawlSource s) {
        SourceForm f = new SourceForm();
        f.setProviderId(s.getProvider().getId());
        f.setName(s.getName());
        f.setType(s.getType());
        f.setUrl(s.getUrl());
        f.setConfig(s.getConfig());
        f.setEnabled(s.isEnabled());
        return f;
    }

    /** @throws IllegalArgumentException if a CSS source has missing or invalid selector configuration */
    @Transactional
    public CrawlSource create(SourceForm f) {
        CrawlSource s = new CrawlSource();
        apply(s, f);
        return sources.save(s);
    }

    @Transactional
    public CrawlSource update(Long id, SourceForm f) {
        CrawlSource s = get(id);
        apply(s, f);
        return sources.save(s);
    }

    @Transactional
    public void setEnabled(Long id, boolean enabled) {
        CrawlSource s = get(id);
        s.setEnabled(enabled);
        sources.save(s);
    }

    @Transactional
    public void delete(Long id) {
        sources.delete(get(id));
    }

    private void apply(CrawlSource s, SourceForm f) {
        if (f.getType() == CrawlSourceType.CSS_SCRAPER) {
            try {
                CssSelectorParser.readConfig(f.getConfig());
            } catch (ParseException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
        s.setProvider(providers.findById(f.getProviderId())
                .orElseThrow(() -> new NotFoundException("Provider " + f.getProviderId() + " not found")));
        s.setName(f.getName().trim());
        s.setType(f.getType());
        s.setUrl(f.getUrl().trim());
        s.setConfig(f.getType() == CrawlSourceType.CSS_SCRAPER ? f.getConfig().trim() : null);
        s.setEnabled(f.isEnabled());
    }
}
