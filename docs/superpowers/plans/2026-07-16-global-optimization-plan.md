# Global Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task with verification checkpoints.

**Goal:** Giảm chi phí runtime của luồng cưỡi/điều khiển mob, dọn trạng thái theo người chơi và giữ tương thích permission cũ mà không thêm dependency hoặc persistence.

**Architecture:** Packet listener tạo snapshot input bất biến theo UUID; scheduler main-thread chỉ đọc snapshot và áp dụng movement cho rider hiện tại. Command state vẫn ở RAM, còn permission cũ được nối tới node mới trong `plugin.yml`.

**Tech Stack:** Java 21, Paper API 1.21.4, ProtocolLib 5.3.0, Gradle 8.8.

## Global Constraints

- Không thêm dependency.
- Không lưu toggle hoặc grant/deny ra đĩa.
- Không thay đổi config gameplay, ngoại trừ dismount khi toggle off.
- Không dùng framework test; smoke check chạy bằng Java/Gradle.
- Production code chỉ viết sau khi có smoke check đỏ tương ứng.

---

### Task 1: Input snapshot và cleanup

**Files:**
- Create: `src/main/java/me/BigBou/rideableMobs/listener/InputState.java`
- Modify: `src/main/java/me/BigBou/rideableMobs/listener/InputListener.java`
- Modify: `src/main/java/me/BigBou/rideableMobs/listener/PlayerListener.java`
- Test: `src/test/java/me/BigBou/rideableMobs/listener/InputStateSmokeTest.java`
- Modify: `build.gradle`

**Interfaces:**
- `InputState` exposes immutable boolean accessors: `forward()`, `backward()`, `left()`, `right()`, `jump()`, `sneak()`, `sprint()`.
- `InputListener.playerInputs` becomes a private `ConcurrentHashMap<UUID, InputState>` with package-level read/remove methods used by the scheduler and listeners.
- `InputListener.onPacketReceiving` stores one `InputState` snapshot per packet and does not expose ProtocolLib handle objects to the scheduler.

- [ ] **Step 1: Write the failing smoke check**

Create `InputStateSmokeTest` with a `main` method that constructs an all-true/all-false state and asserts every accessor, then asserts `InputState` is immutable by only exposing accessors.

- [ ] **Step 2: Run the smoke check and verify it fails**

Run `.\gradlew.bat smokeTest --offline --no-daemon --console=plain`.
Expected: FAIL because `InputState` and `smokeTest` do not exist.

- [ ] **Step 3: Add the minimal snapshot and smoke task**

Add a final class/record-like immutable `InputState` with seven final booleans and add a `smokeTest` `JavaExec` task using `sourceSets.test.runtimeClasspath + configurations.compileClasspath` so Paper's compile-only API is available to the smoke check without adding a dependency.

- [ ] **Step 4: Run the smoke check and verify it passes**

Run `.\gradlew.bat smokeTest --offline --no-daemon --console=plain`. Expected: `BUILD SUCCESSFUL` and no assertion failure.

- [ ] **Step 5: Replace runtime reflection state and add cleanup**

Read the packet handle once in `InputListener`, construct `InputState`, and store by player UUID. Add `removeInput(UUID)` and call it from `PlayerListener.onDismount`; add a `PlayerQuitEvent` handler to remove the player UUID. Ignore malformed packet handles after logging once.

- [ ] **Step 6: Run compile and smoke check**

Run `.\gradlew.bat compileJava smokeTest --offline --no-daemon --console=plain`. Expected: both tasks succeed.

- [ ] **Step 7: Commit**

`git add src/main/java src/test/java build.gradle && git commit -m "perf: snapshot ride input and clean player state"`

### Task 2: Scheduler and movement hot path

**Files:**
- Modify: `src/main/java/me/BigBou/rideableMobs/RideableMobs.java`
- Modify: `src/main/java/me/BigBou/rideableMobs/util/Util.java`

**Interfaces:**
- Scheduler reads `InputState` by UUID and calls `Util.applyMovement(Player, Entity, InputState, double)`.
- `Util.applyMovement` computes `canFly` and `canSwim` once and preserves current movement behavior.
- `Util.horizontalVelocity(float yaw, InputState input, double speed)` returns a normalized `Vector` and is package-visible for the smoke check.

- [ ] **Step 1: Extend the smoke check with movement vector edge cases**

Add assertions for `Util.horizontalVelocity` with no directional input producing zero horizontal velocity and diagonal input being normalized.

- [ ] **Step 2: Run smoke check and verify the new assertions fail**

Run `.\gradlew.bat smokeTest --offline --no-daemon --console=plain`. Expected: FAIL on the new movement assertions before the helper is changed.

- [ ] **Step 3: Implement the minimal hot-path changes**

Iterate only current online riders that have an input snapshot, read `entity-speed` once per tick, avoid repeated reflection/config access, and keep Bukkit API calls on the scheduler thread. Remove the `WrappedInput` parameter from `Util.applyMovement` in favor of `InputState`.

- [ ] **Step 4: Run compile and smoke check**

Run `.\gradlew.bat compileJava smokeTest --offline --no-daemon --console=plain`. Expected: success.

- [ ] **Step 5: Commit**

`git add src/main/java src/test/java && git commit -m "perf: simplify ride movement tick"`

### Task 3: Command compatibility and cleanup

**Files:**
- Modify: `src/main/java/me/BigBou/rideableMobs/command/RideableMobsCommand.java`
- Modify: `src/main/resources/plugin.yml`
- Modify: `src/main/java/me/BigBou/rideableMobs/RideableMobs.java`
- Delete: `src/main/java/me/BigBou/rideableMobs/listener/KeyboardListener.java`
- Modify: `src/main/java/me/BigBou/rideableMobs/listener/PacketListener.java`

**Interfaces:**
- Existing `/rm toggle`, `/rm permission`, and `/rm reload` behavior remains RAM-only.
- Legacy `rideablemobs.commands` and `rideablemobs.toggle` grant access to the corresponding new command nodes.

- [ ] **Step 1: Add a failing permission compatibility check**

Add a PowerShell assertion task that reads `src/main/resources/plugin.yml` and fails unless `rideablemobs.commands`, `rideablemobs.toggle`, and their child mappings are present.

- [ ] **Step 2: Run smoke check and verify it fails**

Run `.\gradlew.bat smokeTest --offline --no-daemon --console=plain`. Expected: FAIL against the current manifest because legacy child mappings are missing.

- [ ] **Step 3: Implement the smallest cleanup**

Use `List.of` for command names, make `plugin` final, centralize configured message translation, preserve legacy permission nodes as children, filter tab completion by command/permission, dismount on toggle-off, and remove the unused keyboard packet listener/registration.

- [ ] **Step 4: Run compile, smoke check, and whitespace check**

Run `.\gradlew.bat compileJava smokeTest --offline --no-daemon --console=plain` and `git diff --check`. Expected: all pass with no whitespace output.

- [ ] **Step 5: Commit**

`git add src/main/java src/main/resources src/test/java build.gradle && git commit -m "refactor: preserve command permissions and remove dead listener"`

### Task 4: Final verification

**Files:**
- Modify only files required by verification fixes.

- [ ] **Step 1: Run full verification**

Run `.\gradlew.bat clean test compileJava smokeTest --offline --no-daemon --console=plain`.

- [ ] **Step 2: Inspect the final diff**

Run `git diff c3f9017..HEAD --stat`, `git diff --check`, and `git status --short`; confirm no generated files are staged and no unrelated config behavior changed.

- [ ] **Step 3: Commit verification-only fixes if needed**

If verification exposes a real implementation defect, fix it, rerun the full command, and commit that fix as `chore: verify global optimization`; otherwise leave the implementation commits unchanged.
