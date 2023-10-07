package kr.talanton.tproject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import com.fasterxml.jackson.databind.ObjectMapper;

public class NaverInterworkManager {	// Singleton으로 동작
	private NaverInterworkManager() { }
	private static NaverInterworkManager instance;
	public static NaverInterworkManager getInstance() {
		if(instance == null) {
			instance = new NaverInterworkManager();
		}
		return instance;
	}
	
	public List<StockInfoVO> getCurrentStockInfo(boolean flag) {
		List<StockInfoVO> stockList = new ArrayList<StockInfoVO>();
		for(int i = 1;;i++) {	// 20개씩 가져오기
			List<StockInfoVO> itemList = getStockSiseInfo(i);	// 20개씩 가져온다.
			if(flag)	System.out.print(".");
			stockList.addAll(itemList);		// 목록에 저장
			if(itemList.size() < 20) {		// 최종 데이터인지 체크
				break;						// 반복문 빠져나가기
			}
			
			try {
				Thread.sleep(2000);			// delay
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if(flag)	System.out.println();
		return stockList;
	}

	private List<StockInfoVO> getStockSiseInfo(int page) {
		List<StockInfoVO> itemList = null;
		HttpsURLConnection conn = null;
		String resultJSON = "";
		URL url;

		try {
		    StringBuffer params = new StringBuffer();		// API 파라미터 구성
		    params.append("menu=" + Constants.MENU);
		    params.append("&sosok=" + Constants.SOSOK);
		    params.append("&pageSize=" + Constants.PAGE_SIZE);	// 20개씩 가져오기
		    params.append("&page=" + page);			// 페이지 번호 : 순서대로 가져온다
				    
		    url = new URL(Constants.BASIC_URL + "?" + params.toString());	// 요청 url과 파라미터
				    
		    conn = (HttpsURLConnection) url.openConnection();
				        
		    if(conn != null) {
				// 헤더 정보 구성
		        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
		        conn.setRequestMethod("GET");
		        conn.setDefaultUseCaches(false);
		        conn.connect();
				
		    	// 응답 데이터 가져오기
		        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		        String input = null;
		        while ((input = br.readLine()) != null){
		            resultJSON += input;
		        }
		        br.close();
		        
		        ObjectMapper objectMapper = new ObjectMapper();	// JSON문자열을 객체로 변환
				SiseResponseVO dto = objectMapper.readValue(resultJSON, SiseResponseVO.class);
				if(dto.getResultCode().equals("success")) {	// 응답이 성공이면
					SiseInfoVO result = dto.getResult();	// 결과를 가져온다.
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
}