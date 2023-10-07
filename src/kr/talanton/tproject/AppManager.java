package kr.talanton.tproject;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

public class AppManager {
	// Singleton으로 만든다.
	private static AppManager instance = null;
	private AppManager() { }
	public static AppManager getInstance() {
		if(instance == null) {
			instance = new AppManager();
		}
		return instance;
	}
	
	// 필요한 정보를 저장한다.
	private Scheduler scheduler;
	private JobDetail jobDetail;
	private CronTrigger cronTrigger;
	public Scheduler getScheduler() {
		return scheduler;
	}
	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}
	public JobDetail getJobDetail() {
		return jobDetail;
	}
	public void setJobDetail(JobDetail jobDetail) {
		this.jobDetail = jobDetail;
	}
	public CronTrigger getCronTrigger() {
		return cronTrigger;
	}
	public void setCronTrigger(CronTrigger cronTrigger) {
		this.cronTrigger = cronTrigger;
	}
	public CronTrigger makeCronTriggerWithCronPeriod(String cronPeriod) {
		CronTrigger cronTrigger = (CronTrigger) TriggerBuilder.newTrigger()	// cron에 의한 작업 등록
                .withIdentity(Constants.CRON_TRIGGER_NAME, Constants.CRON_TRIGGER_GROUP)
                .withSchedule(CronScheduleBuilder.cronSchedule(cronPeriod)) // 주중 월~금 9-16시 10분마다
                .forJob(jobDetail).build();
		return cronTrigger;
	}
	public TriggerKey getTriggerKey() {
		return cronTrigger.getKey();
	}
}