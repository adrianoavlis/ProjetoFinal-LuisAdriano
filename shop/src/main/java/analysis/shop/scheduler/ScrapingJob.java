package analysis.shop.scheduler;

import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import analysis.shop.scraping.SuperMarketScraper;
import analysis.shop.model.Produto;

public class ScrapingJob implements Job {
    @Override
    public void execute(JobExecutionContext context) {
    	SuperMarketScraper scraper = new SuperMarketScraper();
        List<Produto> produtos ;
        // salvar no banco via service
    }
}
