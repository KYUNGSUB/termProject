package kr.talanton.tproject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class PeriodicJobTask implements Job {
	private static final SimpleDateFormat TIMESTAMP_FMT = new SimpleDateFormat("yyyyMMddHHmm");

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
//		System.out.println("Job Executed [" + new Date(System.currentTimeMillis()) + "]"); 
		
		String currentDate = TIMESTAMP_FMT.format(new Date());
//      String triggerKey = context.getTrigger().getKey().toString(); // group1.trggerName
//      System.out.println(String.format("[%s][%s]", currentDate, triggerKey));
        
		DatabaseProcessor dp = DatabaseProcessor.getInstance();
		Long iid = dp.storeInfoReqTable(currentDate);
		NaverInterworkManager ni = NaverInterworkManager.getInstance();
		List<StockInfoVO> stockList = ni.getCurrentStockInfo(false);
		dp.storeStockListInfo(stockList, iid);
		dp.updateInfoReqTable(iid, stockList.size());
	}
}