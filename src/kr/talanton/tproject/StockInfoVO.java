package kr.talanton.tproject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockInfoVO {
	private String thistime;	// 시간 정보. 예) 20230515142139
	private String cd;			// 종목 코드. 예) 005930
 	private String nm;			// 종목 이름. 예) 삼성전자
	private String mt;			// 
	private int nv;				// 현재 가격. 예) 64400
	private int cv;				// 전일 대비. 예) 300
	private float cr;			// 등락율. 예) 0.47%
	private String rf;			// 상승/하락. 예) 2 (상승)
	private int pcv;			// 전날 가격. 예) 64100
	private int mks;			// 거래량. 예) 3844540
	private int aq;				// 대금. 예) 6196250백만
	private int aa;				// 시총. 예) 387조820억
	private String ms;			// 장상태. 예) OPEN(개장중)
	private String tyn;			// N(?)
}