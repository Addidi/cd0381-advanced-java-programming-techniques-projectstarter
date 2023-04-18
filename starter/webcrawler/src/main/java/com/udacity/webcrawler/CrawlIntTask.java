package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
public final class CrawlIntTask extends RecursiveAction {

        private final String url;
        private final Instant deadline;
        private final int maxDepth;
        private final Map<String, Integer> counts;
        private final Set<String> visitedUrls;
        private final Clock clock;
        private final List<Pattern> ignoredUrls;
        private final PageParserFactory parserFactory;
        private final Lock lock = new ReentrantLock();

        public CrawlIntTask(String url,
                                 Instant deadline,
                                 int maxDepth,
                                 Map<String, Integer> counts,
                                 Set<String> visitedUrls,
                                 Clock clock,
                                 List<Pattern> ignoredUrls,
                                 PageParserFactory parserFactory)
        {
            this.url = url;
            this.deadline = deadline;
            this.maxDepth = maxDepth;
            this.counts = counts;
            this.visitedUrls = visitedUrls;
            this.clock = clock;
            this.ignoredUrls = ignoredUrls;
            this.parserFactory = parserFactory;

        }


        @Override
        protected void compute(){
            if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
                return;
            }
            for (Pattern pattern : ignoredUrls) {
                if (pattern.matcher(url).matches()) {
                    return;
                }
            }
       /* if (visitedUrls.contains(url)) {
            return;
        }
        visitedUrls.add(url);*/
            if(!visitedUrls.add(url)){
                return;
            }
            PageParser.Result result = parserFactory.get(url).parse();

            for (ConcurrentMap.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
                lock.lock();
                counts.compute(e.getKey(), (k, v) -> (v == null) ? e.getValue() : e.getValue() +v );
                lock.unlock();
            }

            List<CrawlIntTask> subtasks = new ArrayList<>();

            for (String link : result.getLinks()) {
                subtasks.add(new CrawlIntTask(link, deadline, maxDepth - 1, counts, visitedUrls, clock,ignoredUrls, parserFactory));
            }

            invokeAll(subtasks);
        }


    }
