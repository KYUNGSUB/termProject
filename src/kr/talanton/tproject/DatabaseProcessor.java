package kr.talanton.tproject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseProcessor {
	private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";   // MySQL Java JDBC 드라이버 클래스
	private static final String URL = "jdbc:mysql://localhost:3306/sboot?serverTimeZone=Asia/Seoul";
	private static final String USERNAME = "study";
	private static final String PASSWORD = "study";
	
	private static final String INSERT_SiseInfo = "insert into sise_info (thistime, cd, nm, nv, cv, cr, rf, aq, ms, iid)"
			+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String INSERT_InfoReq = "insert into info_req (req_time) values (?)";
	private static final String GET_LAST_IID = "select last_insert_id()";
	private static final String UPDATE_InfoReq = "update info_req set count = ? where iid = ?";
	private static final String SELECT_INFO_REQ_MAX_COUNT = "select count(*) from info_req";
	private static final String SELECT_InfoReqForPaing = "select * from info_req limit ?, ?";
	private static final String SELECT_STOCK_INFO_MAX_COUNT = "select count(*) from sise_info where thistime like ?";
	private static final String SELECT_STOCK_INFO_PAGE = "select * from sise_info where thistime like ? limit ?, ?";
	private static final String SELECT_PARAMETER_VALUE = "select * from parameter where name = ?";
	private static final String INSERT_PARAMETER_VALUE = "insert into parameter (name, value) values (?, ?)";
	private static final String UPDATE_PARAMETER_VALUE = "update parameter set value = ? where name = ?";
	
	static {
		// 1. JDBC 드라이버 클래스를 가져온다. 드라이버 객체를 메모리로 로딩
        try {
			Class.forName(DRIVER_CLASS);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private DatabaseProcessor() { }		// Singleton을 구현하는 부분 - 시작
	private static DatabaseProcessor instance;
	public static DatabaseProcessor getInstance() {
		if(instance == null) {
			instance = new DatabaseProcessor();
		}
		return instance;
	}									// Singleton을 구현하는 부분 - 끝
	
	public int getMaxCountInfoReq() {	// InfoReq 테이블의 최대 열의 수를 가져온다
		int result = 0;
		try (
			// 2. DBMS에 접속을 해서 Connection을 획득
			Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
			Statement stmt = conn.createStatement();						// Statement 획득
			ResultSet rs = stmt.executeQuery(SELECT_INFO_REQ_MAX_COUNT);	// 질의 수행
			){
			if(rs.next()) {
				result = rs.getInt(1);	// 열의 수 가져오기
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;	// 열의 수 반환
	}

	public List<InfoReqVO> getInfoReqListPage(Integer pageIndex) {	// 해당 페이지 정보 가져오기
		List<InfoReqVO> infoReqList = new ArrayList<InfoReqVO>();	// 리스트 초기화
		int fromIndex = pageIndex * Constants.PAGE_SIZE;			// 가져올 열의 인덱스 만들기
        ResultSet rs = null;
		try (
				// 2. DBMS에 접속을 해서 Connection을 획득
				Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
				PreparedStatement pstmt = conn.prepareStatement(SELECT_InfoReqForPaing);
			){
			pstmt.setInt(1, fromIndex);				// 가져올 열의 처음 인덱스
			pstmt.setInt(2, Constants.PAGE_SIZE);	// 가져올 열의 수
			rs = pstmt.executeQuery();				// 질의 실행
			while(rs.next()) {						// 모든 열에 대하여
				InfoReqVO vo = makeInfoReqVOfromResultSet(rs);	// 정보 가져오기
				infoReqList.add(vo);				// 리스트에 저장
			}
		} catch (SQLException e) {	// 예외 처리
			e.printStackTrace();
		} finally {					// 자원 반납
			if(rs != null) {
				try { rs.close(); } catch(Exception e) { }
			}
		}
		return infoReqList;
	}

	private InfoReqVO makeInfoReqVOfromResultSet(ResultSet rs) throws SQLException {
		InfoReqVO vo = new InfoReqVO();
		vo.setIid(rs.getLong("iid"));				// 아이디 정보 가져오기
		vo.setReq_time(rs.getString("req_time"));	// 시간 정보 가져오기
		vo.setCount(rs.getInt("count"));			// 데이터 갯수 가져오기
		return vo;
	}
	
	public Long storeInfoReqTable(String currentDate) {
		Statement stmt = null;
		ResultSet rs = null;
		Long lastid = 0L;
		try (
			// 2. DBMS에 접속을 해서 Connection을 획득
			Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
			// 3. PrepareStatement 객체를 얻어 온다.
			PreparedStatement pstmt = conn.prepareStatement(INSERT_InfoReq);
			){
			pstmt.setString(1, currentDate);
			int result = pstmt.executeUpdate();		// 데이터를 저장
			stmt = conn.createStatement();
			rs = stmt.executeQuery(GET_LAST_IID);	// 마지막으로 저장된 열의 ID를 가져온다
			if(rs.next()) {
				lastid = rs.getLong(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if(rs != null) {
				try { rs.close(); } catch(SQLException e) { }
			}
			if(stmt != null) {
				try { stmt.close(); } catch(SQLException e) { }
			}
		}
		return lastid;
	}
	
	public void updateInfoReqTable(Long iid, int totalCount) {
		try (
			// 2. DBMS에 접속을 해서 Connection을 획득
			Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
			// 3. PrepareStatement 객체를 얻어 온다.
			PreparedStatement pstmt = conn.prepareStatement(UPDATE_InfoReq);	
			){
			pstmt.setInt(1, totalCount);	// count 값을 변경
			pstmt.setLong(2, iid);			// 해당 ID를 가지는 열에 대하여
			int result = pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void storeStockListInfo(List<StockInfoVO> itemList, Long iid) {
		PreparedStatement pstmt = null;
		try (
			// 2. DBMS에 접속을 해서 Connection을 획득
			Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
			){
			for(StockInfoVO si : itemList) {
				// 3. PrepareStatement 객체를 얻어 온다.
				pstmt = conn.prepareStatement(INSERT_SiseInfo);
				pstmt.setString(1, si.getThistime());
				pstmt.setString(2, si.getCd());
				pstmt.setString(3, si.getNm());
				pstmt.setInt(4, si.getNv());
				pstmt.setInt(5, si.getCv());
				pstmt.setFloat(6, si.getCr());
				pstmt.setString(7, si.getRf());
				pstmt.setInt(8, si.getAa());
				pstmt.setString(9, si.getMs());
				pstmt.setLong(10, iid);
				int result = pstmt.executeUpdate();
				pstmt.close();
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if(pstmt != null) {
				try { pstmt.close(); } catch(SQLException e) { }
			}
		}
	}

	public int getMaxCountStockInfo(String date) {
		int result = 0;
		ResultSet rs = null;
		try (	// 데이터베이스 검색
			// 2. DBMS에 접속을 해서 Connection을 획득
			Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
			PreparedStatement pstmt = 		// 3. Statement 가져오기
					conn.prepareStatement(SELECT_STOCK_INFO_MAX_COUNT);
			) {
			pstmt.setString(1, date + "%");	// 시간 정보
			rs = pstmt.executeQuery();		// 질의 실행
			if(rs.next()) {
				result = rs.getInt(1);		// 열의 수 가져오기
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if(rs != null) {
				try { rs.close(); } catch(Exception e) { }
			}
		}
		return result;
	}
	
	// 측정 시간대의 특정 페이지에 대한 주식 시세정보를 가져오기
	public List<StockInfoVO> getStockInfoListPage(String req_time, Integer pageIndex) {
		int fromIndex = pageIndex * Constants.PAGE_SIZE;	// 시작 열의 인덱스
		List<StockInfoVO> stockList = new ArrayList<StockInfoVO>();
		ResultSet rs = null;
		try (	// 데이터베이스 검색
			// 2. DBMS에 접속을 해서 Connection을 획득
			Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);			
			PreparedStatement pstmt = 	// 3. Statement 가져오기
					conn.prepareStatement(SELECT_STOCK_INFO_PAGE);
			) {
			pstmt.setString(1, req_time + "%");		// 특정 시간 정보
			pstmt.setInt(2, fromIndex);				// 시작 열의 인덱스
			pstmt.setInt(3, Constants.PAGE_SIZE);	// 갯수
			rs = pstmt.executeQuery();
			while(rs.next()) {	// 모든 열에 대하여
				StockInfoVO si = makeStockInfoVOfromResultSet(rs);	// 주식 시세정보
				stockList.add(si);	// 목록에 추가
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if(rs != null) {
				try { rs.close(); } catch(Exception e) { }
			}
		}
		return stockList;
	}
	
	private StockInfoVO makeStockInfoVOfromResultSet(ResultSet rs) throws SQLException {
		StockInfoVO si = new StockInfoVO();
		si.setThistime(rs.getString("thistime"));	// 시간
		si.setCd(rs.getString("cd"));			// 종목 코드
		si.setNm(rs.getString("nm"));			// 종목 이름
		si.setNv(rs.getInt("nv"));				// 대비
		si.setCv(rs.getInt("cv"));				// 현재가
		si.setCr(rs.getFloat("cr"));			// 등락율
		si.setRf(rs.getString("rf"));			// 상승/하락
		si.setAq(rs.getInt("aq"));				// 대금
		si.setMs(rs.getString("ms"));			// 장 상태
		return si;
	}
	
	public ParameterVO getParameter(String name) {
		ParameterVO p = null;
		ResultSet rs = null;
		// 데이터베이스 검색
		try (
			// 2. DBMS에 접속을 해서 Connection을 획득
			Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
			// 3. Statement 가져오기
			PreparedStatement pstmt = conn.prepareStatement(SELECT_PARAMETER_VALUE);
			) {
			pstmt.setString(1, name);
			rs = pstmt.executeQuery();
			if(rs.next()) {
				p = new ParameterVO();
				p.setPid(rs.getLong("pid"));
				p.setName(rs.getString("name"));
				p.setValue(rs.getString("value"));
				p.setRegdate(rs.getTimestamp("regdate"));
				p.setModdate(rs.getTimestamp("moddate"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if(rs != null) {
				try { rs.close(); } catch(Exception e) { }
			}
		}
		return p;
	}
	
	public void insertParameter(String name, String value) {
		try (	// 데이터베이스 검색
			// 2. DBMS에 접속을 해서 Connection을 획득
			Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
			// 3. Statement 가져오기
			PreparedStatement pstmt = conn.prepareStatement(INSERT_PARAMETER_VALUE);
			) {
			pstmt.setString(1, name);
			pstmt.setString(2, value);
			int result = pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void updateParameter(String name, String value) {
		try (	// 데이터베이스 검색
			// 2. DBMS에 접속을 해서 Connection을 획득
			Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
			// 3. Statement 가져오기
			PreparedStatement pstmt = conn.prepareStatement(UPDATE_PARAMETER_VALUE);
			) {
			pstmt.setString(1, value);
			pstmt.setString(2, name);
			int result = pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}