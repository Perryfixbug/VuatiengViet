# Fix cho Persistent Connection - Vua Tiếng Việt

## Vấn đề
Client không nhận được updates từ server, lỗi "invalid type code: AC" xảy ra.

## Nguyên nhân
1. **Stream corruption**: `DelegatingController` tạo controller mới cho mỗi request
2. Mỗi controller con tạo ObjectOutputStream mới → ghi header mới → corrupt stream
3. Client đọc header thay vì object → lỗi "invalid type code: AC" (0xAC = STREAM_MAGIC)

## Giải pháp
### 1. Thêm method `setStreams()` vào ServerController
```java
public void setStreams(ObjectInputStream in, ObjectOutputStream out) {
    this.in = in;
    this.out = out;
}
```

### 2. Sửa DelegatingController để tái sử dụng streams
```java
case "ROOM":
    RoomController rc = new RoomController(clientSocket);
    rc.setStreams(this.in, this.out); // ← QUAN TRỌNG
    return rc.process(request);
```

### 3. Bỏ tạo ObjectOutputStream mới trong handleListen()
```java
// TRƯỚC (SAI):
ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());

// SAU (ĐÚNG):
RoomUpdateManager.getInstance().addListener(roomId, userId, this.out);
```

## Kiến trúc cuối cùng
```
Client                          Server
  |                               |
  |-- LISTEN request ------------>|
  |                               |- DelegatingController
  |                               |  - Tạo streams 1 lần
  |                               |  
  |                               |- RoomController (tái sử dụng streams)
  |                               |  - handleListen()
  |                               |  - Đăng ký listener
  |                               |
  |<-- Initial room state --------|
  |                               |
  | (connection giữ mở)           | (connection giữ mở)
  |                               |
  |                    [Player join]
  |                               |- RoomUpdateManager.broadcastUpdate()
  |<-- ROOM_UPDATED --------------|
  |                               |
  | (cập nhật UI)                 |
```

## Test
1. Khởi động server: `cd server && mvn clean javafx:run`
2. Mở client 1: `cd client && mvn clean javafx:run`
3. Mở client 2: `cd client && mvn clean javafx:run`
4. Client 1: Login → Tạo phòng
5. Client 2: Login → Vào phòng
6. ✅ Client 1 thấy Client 2 join ngay lập tức (không cần polling!)
7. ✅ Client 2 thấy thông tin phòng cập nhật realtime

## Files đã sửa
- `server/src/main/java/vuatiengvietpj/controller/ServerController.java` - Thêm setStreams()
- `server/src/main/java/vuatiengvietpj/ServerApp.java` - Gọi setStreams() cho tất cả controllers
- `server/src/main/java/vuatiengvietpj/controller/RoomController.java` - Bỏ tạo stream mới
