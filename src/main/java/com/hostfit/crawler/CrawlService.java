package com.hostfit.crawler;

import com.hostfit.domain.CrawlRun;
import com.hostfit.domain.CrawlSource;
import com.hostfit.domain.RunStatus;
import com.hostfit.repo.CrawlRunRepository;
import com.hostfit.repo.CrawlSourceRepository;
import com.hostfit.config.AppProperties;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Runs crawl sources: fetch (no database transaction held while on the network), parse, then apply in
 * one short transaction. Every run is recorded in {@link CrawlRun} so the admin can see what happened.
 */
@Service
public class CrawlService {

    private static final Logger log = LoggerFactory.getLogger(CrawlService.class);

    private final Map<CrawlSourceType, PriceParser> parsers = new EnumMap<>(CrawlSourceType.class);
    private final CrawlSourceRepository sources;
    private final CrawlRunRepository runs;
    private final PageFetcher fetcher;
    private final PlanUpsertService upsert;
    private final FxService fx;
    private final AppProperties props;
    private final AtomicBoolean allRunning = new AtomicBoolean(false);

    public CrawlService(List<PriceParser> parserList, CrawlSourceRepository sources, CrawlRunRepository runs,
                        PageFetcher fetcher, PlanUpsertService upsert, FxService fx, AppProperties props) {
        for (PriceParser p : parserList) {
            parsers.put(p.type(), p);
        }
        this.sources = sources;
        this.runs = runs;
        this.fetcher = fetcher;
        this.upsert = upsert;
        this.fx = fx;
        this.props = props;
    }

    /** Fetches and parses a source without saving anything; used by the admin "Test" button. */
    public List<ParsedPlan> preview(Long sourceId) throws Exception {
        CrawlSource source = sources.findWithProviderById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Crawl source not found"));
        return fetchAndParse(source);
    }

    public CrawlRun runSource(Long sourceId) {
        CrawlSource source = sources.findWithProviderById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Crawl source not found"));

        CrawlRun run = new CrawlRun();
        run.setSourceId(source.getId());
        run.setSourceName(source.getName());
        run.setStatus(RunStatus.RUNNING);
        run = runs.save(run);

        try {
            fx.refreshIfEnabled();
            List<ParsedPlan> parsed = fetchAndParse(source);
            PlanUpsertService.Outcome outcome = upsert.apply(source.getId(), parsed);

            run.setPlansFound(outcome.found());
            run.setPlansCreated(outcome.created());
            run.setPlansUpdated(outcome.updated());
            run.setPlansSkipped(outcome.skipped());
            boolean anythingApplied = outcome.created() + outcome.updated() + outcome.unchanged() > 0;
            run.setStatus(!anythingApplied ? RunStatus.FAILED : (outcome.skipped() > 0 ? RunStatus.PARTIAL : RunStatus.SUCCESS));
            run.setMessage(summary(outcome));
        } catch (Exception e) {
            log.warn("Crawl of '{}' failed: {}", source.getName(), e.getMessage());
            run.setStatus(RunStatus.FAILED);
            run.setMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        run.setFinishedAt(Instant.now());
        run = runs.save(run);

        source.setLastRunAt(run.getFinishedAt());
        source.setLastStatus(run.getStatus().name());
        sources.save(source);
        return run;
    }

    /** Runs every enabled source one after another with a pause between them; skips if a run is already in progress. */
    public List<CrawlRun> runAllEnabled() {
        if (!allRunning.compareAndSet(false, true)) {
            log.info("Crawl already in progress, skipping");
            return List.of();
        }
        try {
            List<CrawlRun> results = new ArrayList<>();
            List<CrawlSource> enabled = sources.findEnabledWithProvider();
            for (int i = 0; i < enabled.size(); i++) {
                results.add(runSource(enabled.get(i).getId()));
                if (i < enabled.size() - 1) {
                    pause();
                }
            }
            return results;
        } finally {
            allRunning.set(false);
        }
    }

    public boolean isRunningAll() {
        return allRunning.get();
    }

    private List<ParsedPlan> fetchAndParse(CrawlSource source) throws Exception {
        PriceParser parser = parsers.get(source.getType());
        if (parser == null) {
            throw new ParseException("No parser for source type " + source.getType());
        }
        String body = fetcher.fetch(source.getUrl(), !source.getType().isApi());
        return parser.parse(body, source.getConfig(), source.getUrl());
    }

    private void pause() {
        try {
            Thread.sleep(Math.max(0, props.crawler().delayMs()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String summary(PlanUpsertService.Outcome o) {
        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(o.found()).append(", created ").append(o.created())
                .append(", updated ").append(o.updated()).append(", unchanged ").append(o.unchanged())
                .append(", skipped ").append(o.skipped()).append('.');
        int shown = 0;
        for (String note : o.notes()) {
            if (shown++ == 8) {
                sb.append(" …");
                break;
            }
            sb.append(' ').append(note).append(';');
        }
        return sb.toString();
    }
}
