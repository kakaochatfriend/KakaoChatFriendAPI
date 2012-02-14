# Kakao Chat Friend API Emulator

- 설치 방법
 1. http://nodejs.org에서 nodejs를 설치합니다.
 2.첨부된 파일을 적당한 폴더에 풉니다.
 3. 'npm install mongodb'를 해서 모듈 설치를 합니다.
 4. emul.cfg를 열어서 테스트로 사용할 설정(id, pass, port)을 합니다.
 5. 'node emul.js' 로 실행합니다.

- 사용방법
 1. 먼저 에뮬레이터를 띄운 후 자신이 작성한 bot서버를 설정한 id,pass,port로 접속합니다.
 2. 접속이 완료되면 connected라고 나오고 프롬프트가 표시됩니다.
 3. 프롬프트는 카톡의 대화창이라고 생각하시면 됩니다. 메세지를 치고 엔터를 치시면 봇 서버로 프로토콜에 맞게 전송이 됩니다.

- 명령어
 1. /set [user_key | room_key | country_iso] value
      * 메세지에 보낼 파라메터 값들을 변경합니다.
      * /set user_key 10 - 전송할 유저키를 10으로 변경합니다.
      * /set room_key 20 - 전송할 채팅방 키를 20으로 변경합니다.
      * /set country_iso JP - 전송할 국가코드를 JP로 변경합니다.
  2. /show [user_key | room_key | country_iso | all]
      * 현제 설정된 파라메터 값들을 보여줍니다.
      * /show user_key - user_key값을 보여줍니다.
      * /show all  - 설정된 모든 값들을 보여줍니다.
  3. /event [add|block|leave]
      * 친구 추가 이벤트 및 차단 이벤트를 전송합니다.
      * /event add - 친구 추가 이벤트 메세지를 전송합니다.
  4. /exit
      * 프로그램을 종료합니다.
