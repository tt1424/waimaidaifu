# Cashier Payment Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Turn the current admin-only WeChat payment demo into a public cashier flow with mock/real payment modes.

**Architecture:** Keep the existing Spring Boot payment module as the source of truth for order/payment state, add a public cashier route in the existing Vue app, and switch payment creation through a configurable `mock` or `real` mode. Orders are created from cart selections first, then paid from a public cashier page instead of the admin page directly invoking WeChat.

**Tech Stack:** Spring Boot 3, MyBatis-Plus, JUnit 5, Mockito, Vue 3, Vite, Element Plus

---

### Task 1: Back-End Payment Contract

**Files:**
- Modify: `backend/src/main/java/com/daifu/manage/payment/dto/CheckoutCreateRequest.java`
- Modify: `backend/src/main/java/com/daifu/manage/payment/dto/WechatJsapiPayRequest.java`
- Modify: `backend/src/main/java/com/daifu/manage/payment/vo/CheckoutCreateVO.java`
- Modify: `backend/src/main/java/com/daifu/manage/payment/vo/PaymentOrderVO.java`
- Create: `backend/src/main/java/com/daifu/manage/payment/vo/PaymentOrderItemVO.java`

**Steps:**
1. Write a failing test for checkout creation without `openid`.
2. Run the test and confirm it fails because the current DTO/service still requires `openid`.
3. Update DTO/VO contracts to support cashier-style order creation and richer order detail output.
4. Run the targeted test again and confirm it still fails at service logic, not at compilation.

### Task 2: Back-End Payment Service

**Files:**
- Modify: `backend/src/main/java/com/daifu/manage/payment/service/PaymentService.java`
- Modify: `backend/src/main/java/com/daifu/manage/payment/config/WechatPayProperties.java`
- Modify: `backend/src/main/java/com/daifu/manage/payment/controller/PaymentController.java`
- Modify: `backend/src/main/java/com/daifu/manage/payment/mapper/PaymentOrderMapper.java`

**Steps:**
1. Write failing tests for:
   - creating a cashier order with no `openid`
   - creating mock JSAPI params that bind a mock `openid`
   - simulating payment success updates order, stock, and cart cleanup
   - expired orders are closed when queried/paid
2. Run the tests and confirm the failures match the missing behaviors.
3. Implement minimal service/controller/mapper changes:
   - `mock|real` payment mode
   - delayed `openid` binding
   - public order detail lookup
   - mock payment success endpoint
   - row-locked order update for paid transition
4. Run the targeted tests until green.

### Task 3: Public Cashier Front-End

**Files:**
- Modify: `frontend/admin-web/src/router/index.js`
- Modify: `frontend/admin-web/src/views/CartView.vue`
- Modify: `frontend/admin-web/src/api/pay.js`
- Create: `frontend/admin-web/src/views/CashierView.vue`

**Steps:**
1. Add a public cashier route excluded from login redirect.
2. Replace admin cart payment action with cashier order creation plus cashier link/open action.
3. Build the cashier page to show order details, status, expiry, and payment entry.
4. In mock mode, expose a controlled simulated payment path; in real mode, keep `WeixinJSBridge` integration.
5. Run `npm run build` and fix any front-end regressions.

### Task 4: SQL and Verification

**Files:**
- Create: `backend/src/main/resources/db/migration/V3__cashier_payment_upgrade.sql`
- Modify: `backend/src/main/resources/application.yml`
- Test: `backend/src/test/java/com/daifu/manage/payment/PaymentServiceTests.java`

**Steps:**
1. Add only the schema changes needed for delayed `openid` binding and safer notify dedupe.
2. Keep all SQL in a migration file for manual execution by the user.
3. Run targeted back-end tests, back-end compile, and front-end build.
4. Summarize verification results and list the SQL file for manual execution.
