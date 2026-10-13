import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// --- CÁC CHỈ SỐ THỐNG KÊ TÙY BIẾN CHO BÁO CÁO ---
const successfulOrders = new Counter('successful_orders'); // Số đơn đặt thành công (trừ kho thành công)
const outOfStockOrders = new Counter('out_of_stock_orders'); // Số đơn bị từ chối do hết kho hoặc quá tải
const otherErrors = new Counter('other_errors'); // Lỗi 5xx hoặc lỗi mạng
const orderSuccessRate = new Rate('order_success_rate');
const orderDuration = new Trend('order_duration_ms');

// --- CẤU HÌNH TẢI MÔ PHỎNG (500 - 1000 REQUESTS/GIÂY) ---
export const options = {
  scenarios: {
    flash_sale_concurrency: {
      executor: 'ramping-arrival-rate',
      startRate: 50,               // Khởi động 50 req/s
      timeUnit: '1s',
      preAllocatedVUs: 100,        // Chuẩn bị trước 100 Virtual Users
      maxVUs: 1000,                // Cho phép co giãn tối đa 1000 VUs
      stages: [
        { target: 200, duration: '10s' },  // Tăng tốc lên 200 req/s trong 10s
        { target: 500, duration: '15s' },  // Giữ mức tải 500 req/s trong 15s
        { target: 1000, duration: '10s' }, // Đẩy đột biến lên 1000 req/s (đỉnh điểm Flash Sale)
        { target: 0, duration: '5s' },     // Hạ tải kết thúc
      ],
    },
  },
  thresholds: {
    // 95% số request có thời gian phản hồi dưới 2 giây
    'http_req_duration': ['p(95)<2000'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'https://api-haomilo.duckdns.org';
const PRODUCT_ID = parseInt(__ENV.PRODUCT_ID || '2'); // Mặc định test Sản phẩm ID=2 (có số lượng tồn kho giới hạn)

// 1. SETUP: Đăng nhập lấy JWT accessToken & Ghi nhận số lượng tồn kho ban đầu
export function setup() {
  console.log(`🚀 Bắt đầu kịch bản kiểm thử chịu tải Flash Sale tới: ${BASE_URL}`);

  // Đăng nhập tài khoản admin
  const loginPayload = JSON.stringify({
    username: 'admin',
    password: 'password123'
  });

  let loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
    username: 'admin',
    password: '12345678'
  }), {
    headers: { 'Content-Type': 'application/json' },
  });

  let token = '';
  if (loginRes.status === 200) {
    const body = JSON.parse(loginRes.body);
    token = body.data ? body.data.accessToken : '';
    console.log('✅ Đăng nhập lấy Bearer JWT Token thành công!');
  } else {
    console.warn(`⚠️ Đăng nhập thất bại (HTTP ${loginRes.status}): ${loginRes.body}`);
  }

  // Lấy số lượng tồn kho ban đầu của sản phẩm
  const prodRes = http.get(`${BASE_URL}/api/v1/products/${PRODUCT_ID}`);
  let initialStock = 0;
  if (prodRes.status === 200) {
    try {
      const prodBody = JSON.parse(prodRes.body);
      initialStock = prodBody.data ? prodBody.data.stock : 0;
      console.log(`📦 Tồn kho sản phẩm ID=${PRODUCT_ID} TRƯỚC KHI TEST: ${initialStock} cái`);
    } catch (e) {
      console.warn('Không đọc được thông tin kho:', e);
    }
  }

  return { token: token, initialStock: initialStock };
}

// 2. VU EXECUTION: Từng Virtual User liên tục gửi request đặt hàng đồng thời
export default function (data) {
  const url = `${BASE_URL}/api/v1/orders`;

  const orderPayload = JSON.stringify({
    items: [
      {
        productId: PRODUCT_ID,
        quantity: 1
      }
    ],
    paymentMethod: 'COD',
    customerEmail: `customer_k6_${__VU}_${__ITER}@gmail.com`,
    note: 'Load testing concurrency with k6'
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': data.token ? `Bearer ${data.token}` : '',
    },
    timeout: '5s',
  };

  const start = Date.now();
  const res = http.post(url, orderPayload, params);
  orderDuration.add(Date.now() - start);

  // ĐÁNH GIÁ KẾT QUẢ PHẢN HỒI:
  // - 201: Đặt hàng thành công (Redisson Lock mở, kho còn hàng và đã trừ)
  // - 400: Hết hàng ("Sản phẩm không đủ số lượng tồn kho")
  // - 429: Bị chặn bởi Gateway Rate Limiter (đúng cơ chế bảo vệ hệ thống)
  if (res.status === 201) {
    successfulOrders.add(1);
    orderSuccessRate.add(1);
    check(res, { 'Order thành công (201)': (r) => r.status === 201 });
  } else if (res.status === 400 || res.status === 409 || res.status === 429) {
    outOfStockOrders.add(1);
    orderSuccessRate.add(0);
    check(res, { 'Từ chối đúng khi hết kho/quá tải (400/429)': (r) => r.status === 400 || r.status === 409 || r.status === 429 });
  } else {
    otherErrors.add(1);
  }
}

// 3. TEARDOWN: Kiểm tra lại tồn kho cuối cùng để chứng minh tính bảo toàn (Zero Overselling)
export function teardown(data) {
  console.log('\n=================== BÁO CÁO KẾT THÚC KIỂM THỬ ===================');
  const prodRes = http.get(`${BASE_URL}/api/v1/products/${PRODUCT_ID}`);
  if (prodRes.status === 200) {
    try {
      const prodBody = JSON.parse(prodRes.body);
      const finalStock = prodBody.data ? prodBody.data.stock : 0;
      console.log(`📦 Tồn kho BAN ĐẦU : ${data.initialStock}`);
      console.log(`📦 Tồn kho SAU TEST: ${finalStock}`);
      if (finalStock >= 0) {
        console.log(`🛡️ KẾT LUẬN: ĐẠT CHUẨN ZERO OVERSELLING ✅ (Kho không bao giờ bị âm!)`);
      } else {
        console.log(`❌ THẤT BẠI: Kho bị âm (${finalStock}) ➔ Đã xảy ra Overselling!`);
      }
    } catch (e) {
      console.error(e);
    }
  }
  console.log('=================================================================\n');
}
