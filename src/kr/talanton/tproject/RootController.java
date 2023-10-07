package kr.talanton.tproject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.net.ssl.HttpsURLConnection;

import org.quartz.CronTrigger;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;

import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Callback;

public class RootController implements Initializable {
	@FXML TabPane tabs;
	@FXML TableView<InfoReqVO> table1;				// 테이블의 데이터를 InfoReqVO와 매핑
	@FXML TableColumn<InfoReqVO, Long> t1id;		// 행을 InfoReqVO, Long 타입으로 매핑
	@FXML TableColumn<InfoReqVO, String> t1time;	// 행을 InfoReqVO, String 타입으로 매핑
	@FXML TableColumn<InfoReqVO, Integer> t1count;	// 행을 InfoReqVO, Integer 타입으로 매핑
	@FXML Pagination t1page;						// pagination을 가리키는 변수
	
	@FXML TableView<StockInfoVO> table2;
	@FXML TableColumn<StockInfoVO, String> t2cd;
	@FXML TableColumn<StockInfoVO, Float> t2cr;
	@FXML TableColumn<StockInfoVO, Integer> t2cv;
	@FXML TableColumn<StockInfoVO, Integer> t2nv;
	@FXML TableColumn<StockInfoVO, String> t2nm;
	@FXML Pagination t2page;
	
	@FXML Button startBtn;
	@FXML ProgressBar pBar;
	@FXML private Label workDone;
	@FXML TableView<StockInfoVO> table3;
	@FXML TableColumn<StockInfoVO, String> t3cd;
	@FXML TableColumn<StockInfoVO, Float> t3cr;
	@FXML TableColumn<StockInfoVO, Integer> t3cv;
	@FXML TableColumn<StockInfoVO, Integer> t3nv;
	@FXML TableColumn<StockInfoVO, String> t3nm;
	@FXML Pagination t3page;
	List<StockInfoVO> stockList;	// 주식 시세정보를 화면에 보여주기 위한 목록 데이터 저장
	private Task<Void> task;		// 백그라운드 작업으로 수행하기 위한 Task 클래스
	
	@FXML Button periodBtn;			// 데이터베이스에 저장 버튼
	@FXML TextField periodVal;		// 주기 값 입력창
	
	private Stage primaryStage;	
	public void setPrimaryStage(Stage primaryStage) {
		this.primaryStage = primaryStage;
	}
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		tabs.getSelectionModel().selectedItemProperty().addListener(
			new ChangeListener<Tab>() {
				@Override
				public void changed(ObservableValue<? extends Tab> ov, Tab t, Tab t1) {
					System.out.println("Tab Selection changed : " + t.getId() + ", " + t1.getId());
					processTabChanged(t1);
				}
			}
		);
		startBtn.setOnAction(event -> handleStartBtn(event));
		periodBtn.setOnAction(event -> handlePeriodBtnClicked(event));
		show_overall_data();			// 데이터 현황 보여주기 부분을 호출
	}
	
	private void handlePeriodBtnClicked(ActionEvent event) {
		if(periodVal.getText() == null) {
			showMessage("입력값이 없습니다.");
			return;
		}
		
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("확인창");
		alert.setHeaderText("주기값의 변경을 요청하였습니다.");
		alert.setContentText("정말로 변경하시겠습니까?");

		Optional<ButtonType> result = alert.showAndWait();
		if (result.get() != ButtonType.OK){
			showMessage("취소 되었습니다.");
			return;
		}
		
		int value = Integer.valueOf(periodVal.getText());
		DatabaseProcessor dp = DatabaseProcessor.getInstance();
		ParameterVO period = dp.getParameter("period");

		if(value == Integer.valueOf(period.getValue())) {				// 이전에 저장된 값이 있으면 변경
			showMessage("같은 값으로 요청 되었습니다.");
			return;
		} else {
			dp.updateParameter("period", String.valueOf(value));	// DB 변경
			AppManager appManager = AppManager.getInstance();		// Job 재설정
			Scheduler scheduler = appManager.getScheduler();
			
			String cronPeriod = "0 0/" + value + " 9-16 ? * 1-5 *";	// 시간 재설정값
//			String cronPeriod = "0 0/" + value + " * ? * * *";
			CronTrigger cronTrigger = appManager.makeCronTriggerWithCronPeriod(cronPeriod);
			TriggerKey triggerKey = appManager.getTriggerKey();
			try {
				scheduler.rescheduleJob(triggerKey, cronTrigger);	// 재설정
			} catch (SchedulerException e) {
				e.printStackTrace();
			}
			showMessage("변경 되었습니다.");
		}
	}
	
	private void showMessage(String msg) {	// 메시지를 화면에 보여준다.
		Popup popup = new Popup();
		
		Parent parent;
		try {
			parent = FXMLLoader.load(getClass().getResource("popup.fxml"));
			ImageView imageView = (ImageView) parent.lookup("#imgMessage");
			imageView.setImage(new Image(getClass().getResource("images/dialog-info.png").toString()));
			imageView.setOnMouseClicked(event->popup.hide());	// 이미지를 클릭하면 사라진다.
			Label lblMessage = (Label)parent.lookup("#lblMessage");
			lblMessage.setText(msg);	// 표시할 내용
			
			popup.getContent().add(parent);
			popup.setAutoHide(true);	
			popup.show(primaryStage);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handleStartBtn(ActionEvent e) {
		System.out.println("handleStartBtn()");
		task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				stockList.clear();
				for(int i = 0;;i++) {	// 20개씩 가져오기
					if(isCancelled()) { 
						updateMessage("취소됨");
						break; 
					}
					List<StockInfoVO> itemList = getStockSiseInfo(i+1);
					updateProgress(i, 120);
					updateMessage(String.valueOf(i));
					stockList.addAll(itemList);
					if(itemList.size() < 20) {
						updateMessage("완료됨");
						break;
					}
					
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						if(isCancelled()) { 
							updateMessage("취소됨");
							break; 
						}
					}
				}
				
				Platform.runLater(()->{
					int maxCount = stockList.size();
					t3page.setPageCount(maxCount / Constants.PAGE_SIZE + 1);
					t3page.setPageFactory(new Callback<Integer, Node>() {
						@Override
						public Node call(Integer pageIndex) {
							return createRealtimePage(pageIndex);
						}
					});
				});
				return null;
			}
		};

		pBar.progressProperty().bind(task.progressProperty());
		workDone.textProperty().bind(task.messageProperty());
		Thread thread = new Thread(task);
		thread.setDaemon(true);
		thread.start();
	}
	
	private List<StockInfoVO> getStockSiseInfo(int page) {
		List<StockInfoVO> itemList = null;
		HttpsURLConnection conn = null;
		String resultJSON = "";
		URL url;

		try {
		    StringBuffer params = new StringBuffer();
		    params.append("menu=" + Constants.MENU);
		    params.append("&sosok=" + Constants.SOSOK);
		    params.append("&pageSize=" + Constants.PAGE_SIZE);
		    params.append("&page=" + page);
				    
		    url = new URL(Constants.BASIC_URL + "?" + params.toString());
				    
		    conn = (HttpsURLConnection) url.openConnection();
				        
		    if(conn != null) {              
		        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
		        conn.setRequestMethod("GET");
		        conn.setDefaultUseCaches(false);
	              
		        conn.connect();
				            
		        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		        String input = null;
		        while ((input = br.readLine()) != null){
		            resultJSON += input;
		        }
		        br.close();
		        
		        ObjectMapper objectMapper = new ObjectMapper();
				SiseResponseVO dto = objectMapper.readValue(resultJSON, SiseResponseVO.class);
				if(dto.getResultCode().equals("success")) {
					SiseInfoVO result = dto.getResult();
					itemList = result.getItemList();
				}
		    }
		} catch (MalformedURLException e) {
		    e.printStackTrace();
		} catch (IOException e) {
		    e.printStackTrace();
		}
		return itemList;
	}
	
	protected Node createRealtimePage(Integer pageIndex) {
		int fromIndex = pageIndex * Constants.PAGE_SIZE;
		int toIndex = Math.min(fromIndex + Constants.PAGE_SIZE, stockList.size());
		table3.setItems(FXCollections.observableArrayList(stockList.subList(fromIndex, toIndex)));
		return table3;
	}

	protected void processTabChanged(Tab t1) {
		switch(t1.getId()) {
		case "tab1":
			show_overall_data();
			System.out.println("tab1");
			break;
		case "tab2":
			tab2Prepare();
			System.out.println("tab2");
			break;
		case "tab3":
			settings();
			System.out.println("tab3");
			break;
		default:
			break;
		}
	}

	private void settings() {
		DatabaseProcessor dp = DatabaseProcessor.getInstance();
		ParameterVO period = dp.getParameter("period");
		if(period != null) {
			periodVal.setText(period.getValue());
		} else {
			periodVal.setText("0");
		}
	}

	private void tab2Prepare() {
		stockList = new ArrayList<StockInfoVO>();
		t3cd.setCellValueFactory(
                new PropertyValueFactory<StockInfoVO, String>("cd"));
		t3cd.setStyle("-fx-alignment: CENTER-RIGHT;");
		t3cr.setCellValueFactory(
                new PropertyValueFactory<StockInfoVO, Float>("cr"));
		t3cr.setStyle("-fx-alignment: CENTER-RIGHT;");
		t3cv.setCellValueFactory(
                new PropertyValueFactory<StockInfoVO, Integer>("cv"));
		t3cv.setStyle("-fx-alignment: CENTER-RIGHT;");
		t3nv.setCellValueFactory(
                new PropertyValueFactory<StockInfoVO, Integer>("nv"));
		t3nv.setStyle("-fx-alignment: CENTER-RIGHT;");
		t3nm.setCellValueFactory(
                new PropertyValueFactory<StockInfoVO, String>("nm"));
		t3nm.setStyle("-fx-alignment: CENTER-RIGHT;");
	}

	private void show_overall_data() {	// 데이터 현황 보여주기
		t1id.setCellValueFactory(		// TableColumn과 InfoReqVO의 iid와의 매핑 설정
                new PropertyValueFactory<InfoReqVO, Long>("iid"));
		t1id.setStyle("-fx-alignment: CENTER-RIGHT;");	// 우측 정렬
		t1time.setCellValueFactory(
                new PropertyValueFactory<InfoReqVO, String>("req_time"));
		t1time.setStyle("-fx-alignment: CENTER-RIGHT;");
		t1count.setCellValueFactory(
                new PropertyValueFactory<InfoReqVO, Integer>("count"));
		t1count.setStyle("-fx-alignment: CENTER-RIGHT;");

		DatabaseProcessor dp = DatabaseProcessor.getInstance();		// 데이터베이스 연동 객체
		int maxCount = dp.getMaxCountInfoReq();	// InfoReq테이블의 데이터 갯수를 가져온다.
		t1page.setPageCount(maxCount / Constants.PAGE_SIZE + 1);	// 전체 페이지 수를 설정
		t1page.setPageFactory(new Callback<Integer, Node>() {		// 페이징 처리 부분을 설정
			@Override
			public Node call(Integer pageIndex) {	// 페이지가 변경되면 호출됨(처음에는 1페이지가 호출됨)
				return createPage(pageIndex);		// 해당 페이지 내용을 출력
			}
		});
	}
	
	protected Node createPage(Integer pageIndex) {	// 해당 페이지 내용을 출력
		DatabaseProcessor dp = DatabaseProcessor.getInstance();		// 데이터베이스 연동 객체
		List<InfoReqVO> infoReqList = dp.getInfoReqListPage(pageIndex);	// 페이지 데이터 획득
		table1.setItems(FXCollections.observableArrayList(infoReqList));// UI 테이블에 설정
		table1.setOnMouseClicked(mouseEvent -> {
			if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
			    if (mouseEvent.getClickCount() == 1 &&
			    	table1.getSelectionModel().getSelectedItem()!=null) {			// 열을 선택하면
			    	InfoReqVO vo = table1.getSelectionModel().getSelectedItem();	// 열 정보 얻기
			    	search_stock_info(vo.getReq_time());   // 선택된 시간대의 주식시세 정보 보여주기
			    }
			}
		});
		return table1;	// 테이블 뷰 반환
	}
	
	private void search_stock_info(String req_time) {	// req_time : 특정 시간대 정보
		t2cd.setCellValueFactory(						// UI와 빈간 정보 매핑 : 종목 코드(cd)
                new PropertyValueFactory<StockInfoVO, String>("cd"));
		t2cd.setStyle("-fx-alignment: CENTER-RIGHT;");	// 우로 정렬
		t2cr.setCellValueFactory(
                new PropertyValueFactory<StockInfoVO, Float>("cr"));
		t2cr.setStyle("-fx-alignment: CENTER-RIGHT;");
		t2cv.setCellValueFactory(
                new PropertyValueFactory<StockInfoVO, Integer>("cv"));
		t2cv.setStyle("-fx-alignment: CENTER-RIGHT;");
		t2nv.setCellValueFactory(
                new PropertyValueFactory<StockInfoVO, Integer>("nv"));
		t2nv.setStyle("-fx-alignment: CENTER-RIGHT;");
		t2nm.setCellValueFactory(
                new PropertyValueFactory<StockInfoVO, String>("nm"));
		t2nm.setStyle("-fx-alignment: CENTER-RIGHT;");

		DatabaseProcessor dp = DatabaseProcessor.getInstance();
		int maxCount = dp.getMaxCountStockInfo(req_time);			// 주식 시세 정보 최대 열의 수
		t2page.setPageCount(maxCount / Constants.PAGE_SIZE + 1);	// 페이지 수 설정
		t2page.setPageFactory(new Callback<Integer, Node>() {		// 페이징에 따른 처리 설정
			@Override
			public Node call(Integer pageIndex) {
				return createDetailedPage(req_time, pageIndex);		// 페이징 처리 시 보여주기
			}
		});
	}
	
	protected Node createDetailedPage(String req_time, Integer pageIndex) {
		DatabaseProcessor dp = DatabaseProcessor.getInstance();
		List<StockInfoVO> stockInfoList = dp.getStockInfoListPage(req_time, pageIndex);
		table2.setItems(FXCollections.observableArrayList(stockInfoList));
		return table2;
	}
}