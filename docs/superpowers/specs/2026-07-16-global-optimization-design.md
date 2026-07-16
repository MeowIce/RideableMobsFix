# RideableMobs Global Optimization Design

## Goal

Giảm chi phí runtime và dọn các điểm rò trạng thái trong luồng điều khiển mob, đồng thời giữ nguyên hành vi lưu trạng thái trong RAM và tương thích quyền command cũ.

## Scope

- Không thêm dependency.
- Không lưu toggle hoặc grant/deny ra đĩa; restart server vẫn xoá trạng thái.
- Không thay đổi cấu hình gameplay ngoài việc toggle khi đang cưỡi sẽ tự xuống mob.
- Giữ API/config hiện có ở mức cần thiết cho plugin.

## Design

### Input and movement

`InputListener` chuyển packet thành một snapshot boolean bất biến (`forward`, `backward`, `left`, `right`, `jump`, `sneak`, `sprint`) ngay khi packet đến. Scheduler chỉ đọc snapshot, không gọi reflection trong mỗi tick. Trạng thái được lưu theo `UUID` trong map an toàn khi callback packet và scheduler có thể khác thread; trạng thái bị xoá khi dismount hoặc quit.

Scheduler lấy `entity-speed` một lần mỗi tick và chỉ áp dụng movement cho người chơi đang cưỡi entity không phải Bukkit `Vehicle`, thuộc world được bật, chưa toggle off và có snapshot input. Các giá trị `canFly`/`canSwim` dùng một lần trong phép tính movement.

### Commands and permissions

Giữ grant/deny và toggle trong RAM. Các permission node cũ `rideablemobs.commands` và `rideablemobs.toggle` tiếp tục hoạt động thông qua child/alias tới node mới. Tab-complete chỉ trả về nội dung phù hợp với command và quyền người gửi. Khi toggle off trong lúc đang cưỡi, người chơi được dismount để trạng thái “tắt” có hiệu lực ngay.

### Code organization

Xoá `KeyboardListener` vì không còn được đăng ký. Dọn collection raw type, double-brace initialization, import fully-qualified và các helper gửi message lặp. Giữ logic movement trong helper hiện có để tránh thêm abstraction mới.

## Error handling

- Packet không đọc được sẽ bị bỏ qua và không làm hỏng scheduler.
- Entity/player không còn hợp lệ được bỏ qua trong tick.
- Không dùng assert cho validation runtime.

## Verification

- `gradlew compileJava` phải thành công.
- Thêm một smoke check chạy được cho snapshot/input movement mà không cần framework mới.
- `git diff --check` không còn whitespace lỗi.
- Kiểm tra thủ công permission cũ/mới, toggle khi đang cưỡi, dismount/quit cleanup và reload config.
