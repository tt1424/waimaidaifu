const DB_KEY = "daifu_admin_mock_db_v1";

function nowText() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

function uid(prefix) {
  return `${prefix}_${Date.now()}_${Math.floor(Math.random() * 10000)}`;
}

function initDb() {
  return {
    staff: [
      { id: 1, username: "admin", name: "超级管理员", phone: "13800000000", role: "管理员", status: 1, password: "123456", createTime: nowText() }
    ],
    products: [
      { id: 1, name: "代付测试商品A", price: 16.8, status: 1, createTime: nowText() },
      { id: 2, name: "代付测试商品B", price: 29.9, status: 1, createTime: nowText() }
    ],
    orders: []
  };
}

function readDb() {
  const text = localStorage.getItem(DB_KEY);
  if (!text) {
    const db = initDb();
    localStorage.setItem(DB_KEY, JSON.stringify(db));
    return db;
  }
  try {
    return JSON.parse(text);
  } catch {
    const db = initDb();
    localStorage.setItem(DB_KEY, JSON.stringify(db));
    return db;
  }
}

function writeDb(db) {
  localStorage.setItem(DB_KEY, JSON.stringify(db));
}

function sleep(ms = 180) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function pageResult(records, pageNum = 1, pageSize = 10) {
  const total = records.length;
  const start = (pageNum - 1) * pageSize;
  const end = start + pageSize;
  return { total, records: records.slice(start, end) };
}

export async function mockLogin({ username, password }) {
  await sleep();
  const db = readDb();
  const user = db.staff.find((s) => s.username === username);
  if (!user) throw new Error("账号不存在");
  if (user.status !== 1) throw new Error("账号已禁用");
  if (user.password !== password) throw new Error("密码错误");
  return { token: uid("token"), expireInSeconds: 7200, username: user.username, role: user.role };
}

export async function mockListStaff(params = {}) {
  await sleep();
  const { pageNum = 1, pageSize = 10, keyword = "", status } = params;
  const db = readDb();
  let list = [...db.staff].sort((a, b) => b.id - a.id);
  if (keyword) {
    list = list.filter((x) => x.username.includes(keyword) || x.name.includes(keyword));
  }
  if (status !== undefined && status !== null && status !== "") {
    list = list.filter((x) => x.status === Number(status));
  }
  return pageResult(list, Number(pageNum), Number(pageSize));
}

export async function mockCreateStaff(data) {
  await sleep();
  const db = readDb();
  if (db.staff.some((x) => x.username === data.username)) {
    throw new Error("账号已存在");
  }
  const row = {
    id: db.staff.length ? Math.max(...db.staff.map((x) => x.id)) + 1 : 1,
    username: data.username,
    name: data.name,
    phone: data.phone || "",
    role: data.role || "运营",
    status: 1,
    password: data.password || "123456",
    createTime: nowText()
  };
  db.staff.push(row);
  writeDb(db);
  return row;
}

export async function mockUpdateStaff(id, data) {
  await sleep();
  const db = readDb();
  const row = db.staff.find((x) => x.id === Number(id));
  if (!row) throw new Error("人员不存在");
  row.name = data.name;
  row.phone = data.phone;
  row.role = data.role;
  writeDb(db);
  return row;
}

export async function mockUpdateStaffStatus(id, status) {
  await sleep();
  const db = readDb();
  const row = db.staff.find((x) => x.id === Number(id));
  if (!row) throw new Error("人员不存在");
  row.status = Number(status);
  writeDb(db);
  return true;
}

export async function mockResetStaffPassword(id, password) {
  await sleep();
  const db = readDb();
  const row = db.staff.find((x) => x.id === Number(id));
  if (!row) throw new Error("人员不存在");
  row.password = password || "123456";
  writeDb(db);
  return true;
}

export async function mockListProducts(params = {}) {
  await sleep();
  const { pageNum = 1, pageSize = 10, name = "", status } = params;
  const db = readDb();
  let list = [...db.products].sort((a, b) => b.id - a.id);
  if (name) list = list.filter((x) => x.name.includes(name));
  if (status !== undefined && status !== null && status !== "") {
    list = list.filter((x) => x.status === Number(status));
  }
  return pageResult(list, Number(pageNum), Number(pageSize));
}

export async function mockCreateProduct(data) {
  await sleep();
  const db = readDb();
  const row = {
    id: db.products.length ? Math.max(...db.products.map((x) => x.id)) + 1 : 1,
    name: data.name,
    price: Number(data.price),
    status: 1,
    createTime: nowText()
  };
  db.products.push(row);
  writeDb(db);
  return row;
}

export async function mockUpdateProduct(id, data) {
  await sleep();
  const db = readDb();
  const row = db.products.find((x) => x.id === Number(id));
  if (!row) throw new Error("商品不存在");
  row.name = data.name;
  row.price = Number(data.price);
  writeDb(db);
  return row;
}

export async function mockUpdateProductStatus(id, status) {
  await sleep();
  const db = readDb();
  const row = db.products.find((x) => x.id === Number(id));
  if (!row) throw new Error("商品不存在");
  row.status = Number(status);
  writeDb(db);
  return true;
}

export async function mockCreateOrder(data) {
  await sleep();
  const db = readDb();
  const orderNo = uid("OD").replace(/_/g, "");
  const row = {
    id: db.orders.length ? Math.max(...db.orders.map((x) => x.id)) + 1 : 1,
    orderNo,
    productId: Number(data.productId),
    productName: data.productName,
    amount: Number(data.amount),
    status: 0,
    payUrl: `https://your-domain/pay?orderNo=${orderNo}`,
    createTime: nowText(),
    expireTime: nowText(),
    payTime: ""
  };
  db.orders.push(row);
  writeDb(db);
  return row;
}

export async function mockListOrders(params = {}) {
  await sleep();
  const { pageNum = 1, pageSize = 10, orderNo = "", status } = params;
  const db = readDb();
  let list = [...db.orders].sort((a, b) => b.id - a.id);
  if (orderNo) list = list.filter((x) => x.orderNo.includes(orderNo));
  if (status !== undefined && status !== null && status !== "") {
    list = list.filter((x) => x.status === Number(status));
  }
  return pageResult(list, Number(pageNum), Number(pageSize));
}

export async function mockPayNotify(data) {
  await sleep();
  const db = readDb();
  const row = db.orders.find((x) => x.orderNo === data.orderNo);
  if (!row) throw new Error("订单不存在");
  row.status = 1;
  row.payTime = data.payTime || nowText();
  row.payAmount = Number(data.payAmount || row.amount);
  writeDb(db);
  return { orderNo: row.orderNo, notify: "accepted" };
}

export async function mockOverview() {
  await sleep();
  const db = readDb();
  const paid = db.orders.filter((x) => x.status === 1);
  const today = new Date().toISOString().slice(0, 10);
  const todayPaid = paid.filter((x) => (x.payTime || "").startsWith(today));
  const sum = (arr) => arr.reduce((acc, it) => acc + Number(it.payAmount || it.amount || 0), 0);
  return {
    totalPayAmount: sum(paid).toFixed(2),
    todayPayAmount: sum(todayPaid).toFixed(2),
    payCount: paid.length,
    unpaidCount: db.orders.filter((x) => x.status === 0).length
  };
}
