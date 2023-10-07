package kr.talanton.tproject;

import java.util.HashSet;
import java.util.Set;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AppMain extends Application {	// JavaFX를 위해 Application 상속
	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("root.fxml"));
		Parent root = loader.load();
		RootController controller = loader.getController();
		controller.setPrimaryStage(primaryStage);
		
		Scene scene = new Scene(root);
		
		primaryStage.setTitle("StockApp");
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	public static void main(String[] args) {	// 실행 매소드를 만들어야 함
		registerPeriodicJob();	// 주기적으로 주식 시세 정보를 가져오는 작업을 등록
		launch(args);	// JavaFX UI 구동
	}
	
	private static void registerPeriodicJob() {
        DatabaseProcessor dp = DatabaseProcessor.getInstance();	// 데이터베이스 처리 객체 가져오기
        ParameterVO parameter = dp.getParameter("period");		// 주기 정보 가져오기(default: 10분)
//        String cronPeriod = "0 0/" + ((parameter != null)? parameter.getValue() : "10") + " * ? * * *";
        String cronPeriod = "0 0/" + ((parameter != null)? parameter.getValue() : "10") + " 9-16 ? * 1-5 *";
        // 시간 제한 : 개장되는 시간만(9~16), 월요일~금요일만 : 1-5 -> 휴일은 현재 구분 못함
		
        // Scheduler 사용을 위한 인스턴스화
        SchedulerFactory schedulerFactory = new StdSchedulerFactory();
        Scheduler scheduler;
        AppManager appManager = AppManager.getInstance();
        try {
     	   scheduler = schedulerFactory.getScheduler();
     	   appManager.setScheduler(scheduler);
    	   JobDetail jobDetail = JobBuilder.newJob(PeriodicJobTask.class)	// 주기적인 job 등록
                        .withIdentity(Constants.JOB_ID_NAME, Constants.JOB_ID_GROUP).build();
    	   appManager.setJobDetail(jobDetail);
    	   
    	   // CronTrigger
           CronTrigger cronTrigger = appManager.makeCronTriggerWithCronPeriod(cronPeriod);
           appManager.setCronTrigger(cronTrigger);
                
           Set<Trigger> triggerSet = new HashSet<Trigger>();
           triggerSet.add(cronTrigger);

           scheduler.scheduleJob(jobDetail, triggerSet, false);
           scheduler.start();
        }
        catch(Exception e) {	e.printStackTrace();	}
    }
}