package talkProject;

/*
 * 서버 쓰레드 - 
 * roomcasting 필요 - 처음 로그인 할 때. 
 * 
 */
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

import com.network2.TalkServerThread;

public class ServerThread extends Thread {
	TalkServer ts = null;
	ObjectOutputStream oos = null;
	ObjectInputStream ois = null;
	
	String nickName = null;//사용자의 닉네임 담김
	String g_title	= null; //톡방명
	int g_current	= 0; //톡방 참여자수
	
	public ServerThread(TalkServer talkServer) {
		this.ts = talkServer;
		try {
			oos = new ObjectOutputStream(ts.client.getOutputStream());
			ois = new ObjectInputStream(ts.client.getInputStream());
			String msg = (String)ois.readObject();
			ts.jta_log.append(msg+"\n");
			ts.jta_log.setCaretPosition(ts.jta_log.getDocument().getLength());
			StringTokenizer st = null;
			if(msg!=null) {
				st = new StringTokenizer(msg,Protocol.seperator);
			}
			if(st.hasMoreTokens()) {
				st.nextToken();
				nickName = st.nextToken();
			}
			
		///////////////////////[톡방목록 처리]//////////////////////////
		/*
		 * 친구리스트는 Client에서 바로 Dao처리 - ?
		 * room에 있는 닉네임 = roomtitle
		 * 마지막 msg.
		 */
			for(int i=0;i<ts.roomList.size();i++) {
				Room room = ts.roomList.get(i);
				String title = room.title;
				//String nick  = room.rlist.get(i).get("nick").toString();
				g_title	= title;
				//nickName = nick;
				int current = 0;
				if(room.userList!=null || room.nameList.size() >0) {
					current = room.userList.size();
				}
				g_current = current;
				this.send(Protocol.ROOM_LIST 
						+ Protocol.seperator + g_title 
						+ Protocol.seperator + g_current);
			}
			//입장한 내 스레드 추가하기
			ts.globallist.add(this);
			JOptionPane.showMessageDialog(ts, "64 global list");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void broadCasting(String msg) {
		//동기화 처리 추가하기 - 절대 인터셉트 당하지 않음
		synchronized(this) {	
			for(ServerThread tst : ts.globallist) {
				tst.send(msg);
			}
		}
	}
	public void roomCasting(String msg) {
		for(int i=0;i<ts.roomList.size();i++) {   //톡방의 수만큼 반복이 일어남
			Room room = ts.roomList.get(i); //룸클래스를 하나씩 꺼냄(룸 주소번지) 
			if(g_title.equals(room.title)) { //같은 방인지 체크함.
				for(int j=0; j<room.userList.size(); j++) { //룸에 있는 사람수만큼 반복해서 메세지를 보냄.
					ServerThread sth = room.userList.get(j);
					try {
						sth.send(msg);
					} catch (Exception e) {
						room.userList.remove(j--);
					}
				}
				break;
			}
		}
	}
	public void send(String msg) {//반복문은 필요없다.
		try {
			oos.writeObject(msg);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	public void run() {
		boolean isStop = false;
		try {
			run_start:
			while(!isStop) {
				String msg = (String)ois.readObject();
				ts.jta_log.append(msg+"\n");//200|나신입|주말에 뭐해?
				ts.jta_log.setCaretPosition(ts.jta_log.getDocument().getLength());
				int protocol = 0;
				StringTokenizer st = null;
				if(msg!=null) {
					st = new StringTokenizer(msg,"|");
					protocol = Integer.parseInt(st.nextToken());
					}
				switch(protocol) {
				case Protocol.ROOM_CREATE:{
					//방생성됨과 동시에 리스트에 추가, roomin.
					//Dao연동해서 테이블에 INSERT
					
					JOptionPane.showMessageDialog(ts, "Room Create");
					String roomTitle  = st.nextToken();
					String currentNum = st.nextToken();
					Room room = new Room(roomTitle, Integer.parseInt(currentNum));
					ts.roomList.add(room);
					if(Integer.parseInt(currentNum) == 2) {
					/*
					 * roomtitle == nickname
					 * userlist의 nickname과 비교해서
					 * 나와 상대방 thread를 add하고
					 * broadcasting room_create, room in 한번
					 */
					
					//셀프톡
					}else {
						this.broadCasting(Protocol.ROOM_CREATE
								+ Protocol.seperator + roomTitle
								+ Protocol.seperator + currentNum);
					}
				}break;
				}
			}///while
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
		
