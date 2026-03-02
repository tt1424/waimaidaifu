const DB_KEY = "manage_admin_mock_db_v2";

function nowText() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

function todayText() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function initDb() {
  const now = nowText();
  return {
    users: [
      { id: 1, username: "admin", password: "123456", phone: "13800000000", role: 1, status: 1, createTime: now },
      { id: 2, username: "user1", password: "123456", phone: "13900000001", role: 2, status: 1, createTime: now },
      { id: 3, username: "user2", password: "123456", phone: "13900000002", role: 2, status: 1, createTime: now }
    ],
    stores: [
      { id: 1, name: "一号店铺", contactName: "张三", contactPhone: "13600000001", status: 1, createTime: now },
      { id: 2, name: "二号店铺", contactName: "李四", contactPhone: "13600000002", status: 1, createTime: now }
    ],
    products: [
      { id: 1, storeId: 1, name: "测试商品A", price: 16.8, stock: 100, description: "商品A描述", status: 1, createTime: now },
      { id: 2, storeId: 1, name: "测试商品B", price: 29.9, stock: 80, description: "商品B描述", status: 1, createTime: now },
      { id: 3, storeId: 2, name: "测试商品C", price: 45, stock: 60, description: "商品C描述", status: 1, createTime: now }
    ],
    cartItems: []
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

function sleep(ms = 150) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function pageResult(records, pageNum = 1, pageSize = 10) {
  const total = records.length;
  const start = (pageNum - 1) * pageSize;
  const end = start + pageSize;
  return { total, pageNum, pageSize, records: records.slice(start, end) };
}

function pickStoreName(db, storeId) {
  return db.stores.find((x) => x.id === storeId)?.name || "";
}

function inRange(timeText, startTime, endTime) {
  const t = new Date(timeText.replace(" ", "T")).getTime();
  if (startTime) {
    const s = new Date(startTime.replace(" ", "T")).getTime();
    if (t < s) return false;
  }
  if (endTime) {
    const e = new Date(endTime.replace(" ", "T")).getTime();
    if (t > e) return false;
  }
  return true;
}

export async function mockLogin({ username, password }) {
  await sleep();
  const db = readDb();
  const user = db.users.find((s) => s.username === username);
  if (!user) throw new Error("账号不存在");
  if (user.status !== 1) throw new Error("账号已禁用");
  if (user.password !== password) throw new Error("密码错误");
  return { token: `${Date.now()}_${user.id}`, expireInSeconds: 7200, userId: user.id, username: user.username, role: user.role };
}

export async function mockListUsers(params = {}) {
  await sleep();
  const { pageNum = 1, pageSize = 10, username = "", phone = "", role, status } = params;
  const db = readDb();
  let list = [...db.users].sort((a, b) => b.id - a.id);
  if (username) list = list.filter((x) => x.username.includes(username));
  if (phone) list = list.filter((x) => (x.phone || "").includes(phone));
  if (role !== undefined && role !== null && role !== "") list = list.filter((x) => x.role === Number(role));
  if (status !== undefined && status !== null && status !== "") list = list.filter((x) => x.status === Number(status));
  return pageResult(list, Number(pageNum), Number(pageSize));
}

export async function mockCreateUser(data) {
  await sleep();
  const db = readDb();
  if (db.users.some((x) => x.username === data.username)) throw new Error("用户名已存在");
  const row = {
    id: db.users.length ? Math.max(...db.users.map((x) => x.id)) + 1 : 1,
    username: data.username,
    password: data.password || "123456",
    phone: data.phone || "",
    role: Number(data.role || 2),
    status: Number(data.status ?? 1),
    createTime: nowText()
  };
  db.users.push(row);
  writeDb(db);
  return row;
}

export async function mockUpdateUser(id, data) {
  await sleep();
  const db = readDb();
  const row = db.users.find((x) => x.id === Number(id));
  if (!row) throw new Error("用户不存在");
  if (db.users.some((x) => x.id !== Number(id) && x.username === data.username)) throw new Error("用户名已存在");
  row.username = data.username;
  row.phone = data.phone || "";
  row.role = Number(data.role);
  row.status = Number(data.status);
  if (data.password) row.password = data.password;
  writeDb(db);
  return row;
}

export async function mockDeleteUser(id) {
  await sleep();
  const db = readDb();
  const idx = db.users.findIndex((x) => x.id === Number(id));
  if (idx < 0) throw new Error("用户不存在");
  db.users.splice(idx, 1);
  db.cartItems = db.cartItems.filter((x) => x.userId !== Number(id));
  writeDb(db);
  return true;
}

export async function mockListStores(params = {}) {
  await sleep();
  const { pageNum = 1, pageSize = 10, name = "", status } = params;
  const db = readDb();
  let list = [...db.stores].sort((a, b) => b.id - a.id);
  if (name) list = list.filter((x) => x.name.includes(name));
  if (status !== undefined && status !== null && status !== "") list = list.filter((x) => x.status === Number(status));
  return pageResult(list, Number(pageNum), Number(pageSize));
}

export async function mockAllStores() {
  await sleep();
  const db = readDb();
  return db.stores.filter((x) => x.status === 1).sort((a, b) => b.id - a.id);
}

export async function mockCreateStore(data) {
  await sleep();
  const db = readDb();
  const row = {
    id: db.stores.length ? Math.max(...db.stores.map((x) => x.id)) + 1 : 1,
    name: data.name,
    contactName: data.contactName || "",
    contactPhone: data.contactPhone || "",
    status: Number(data.status ?? 1),
    createTime: nowText()
  };
  db.stores.push(row);
  writeDb(db);
  return row;
}

export async function mockUpdateStore(id, data) {
  await sleep();
  const db = readDb();
  const row = db.stores.find((x) => x.id === Number(id));
  if (!row) throw new Error("店铺不存在");
  row.name = data.name;
  row.contactName = data.contactName || "";
  row.contactPhone = data.contactPhone || "";
  row.status = Number(data.status);
  writeDb(db);
  return row;
}

export async function mockDeleteStore(id) {
  await sleep();
  const db = readDb();
  const idx = db.stores.findIndex((x) => x.id === Number(id));
  if (idx < 0) throw new Error("店铺不存在");
  db.stores.splice(idx, 1);
  db.products = db.products.filter((x) => x.storeId !== Number(id));
  db.cartItems = db.cartItems.filter((x) => db.products.some((p) => p.id === x.productId));
  writeDb(db);
  return true;
}

export async function mockListProducts(params = {}) {
  await sleep();
  const { pageNum = 1, pageSize = 10, storeId, name = "", status } = params;
  const db = readDb();
  let list = [...db.products].sort((a, b) => b.id - a.id);
  if (storeId !== undefined && storeId !== null && storeId !== "") list = list.filter((x) => x.storeId === Number(storeId));
  if (name) list = list.filter((x) => x.name.includes(name));
  if (status !== undefined && status !== null && status !== "") list = list.filter((x) => x.status === Number(status));
  list = list.map((x) => ({ ...x, storeName: pickStoreName(db, x.storeId) }));
  return pageResult(list, Number(pageNum), Number(pageSize));
}

export async function mockAllProducts(params = {}) {
  await sleep();
  const db = readDb();
  const storeId = params?.storeId;
  let list = db.products.filter((x) => x.status === 1);
  if (storeId !== undefined && storeId !== null && storeId !== "") {
    list = list.filter((x) => x.storeId === Number(storeId));
  }
  return list.map((x) => ({ ...x, storeName: pickStoreName(db, x.storeId) }));
}

export async function mockCreateProduct(data) {
  await sleep();
  const db = readDb();
  const row = {
    id: db.products.length ? Math.max(...db.products.map((x) => x.id)) + 1 : 1,
    storeId: Number(data.storeId),
    name: data.name,
    price: Number(data.price),
    stock: Number(data.stock),
    description: data.description || "",
    status: 1,
    createTime: nowText()
  };
  db.products.push(row);
  writeDb(db);
  return { ...row, storeName: pickStoreName(db, row.storeId) };
}

export async function mockUpdateProduct(id, data) {
  await sleep();
  const db = readDb();
  const row = db.products.find((x) => x.id === Number(id));
  if (!row) throw new Error("商品不存在");
  row.storeId = Number(data.storeId);
  row.name = data.name;
  row.price = Number(data.price);
  row.stock = Number(data.stock);
  row.description = data.description || "";
  writeDb(db);
  return { ...row, storeName: pickStoreName(db, row.storeId) };
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

export async function mockListCartByUser(userId) {
  await sleep();
  const db = readDb();
  const uid = Number(userId);
  const items = db.cartItems
    .filter((x) => x.userId === uid)
    .sort((a, b) => b.id - a.id)
    .map((x) => {
      const p = db.products.find((it) => it.id === x.productId);
      return {
        ...x,
        productName: p?.name || "",
        storeId: p?.storeId || null
      };
    });
  const totalQuantity = items.reduce((sum, x) => sum + Number(x.quantity), 0);
  const totalAmount = items.reduce((sum, x) => sum + Number(x.totalAmount), 0);
  return {
    items,
    summary: {
      itemCount: items.length,
      totalQuantity,
      totalAmount: Number(totalAmount.toFixed(2))
    }
  };
}

export async function mockAddCartItem(data) {
  await sleep();
  const db = readDb();
  const userId = Number(data.userId);
  const productId = Number(data.productId);
  const quantity = Number(data.quantity);
  const product = db.products.find((x) => x.id === productId && x.status === 1);
  if (!product) throw new Error("商品不可用");
  if (quantity < 1) throw new Error("数量必须大于0");
  if (product.stock < quantity) throw new Error("库存不足");
  const existing = db.cartItems.find((x) => x.userId === userId && x.productId === productId);
  if (existing) {
    const next = existing.quantity + quantity;
    if (product.stock < next) throw new Error("库存不足");
    existing.quantity = next;
    existing.unitPrice = Number(product.price);
    existing.totalAmount = Number((existing.quantity * existing.unitPrice).toFixed(2));
    existing.updateTime = nowText();
    writeDb(db);
    return existing;
  }
  const row = {
    id: db.cartItems.length ? Math.max(...db.cartItems.map((x) => x.id)) + 1 : 1,
    userId,
    productId,
    quantity,
    unitPrice: Number(product.price),
    totalAmount: Number((quantity * Number(product.price)).toFixed(2)),
    createTime: nowText(),
    updateTime: nowText()
  };
  db.cartItems.push(row);
  writeDb(db);
  return row;
}

export async function mockUpdateCartItem(id, data) {
  await sleep();
  const db = readDb();
  const row = db.cartItems.find((x) => x.id === Number(id));
  if (!row) throw new Error("购物车项不存在");
  const product = db.products.find((x) => x.id === row.productId);
  if (!product) throw new Error("商品不存在");
  const quantity = Number(data.quantity);
  if (quantity < 1) throw new Error("数量必须大于0");
  if (product.stock < quantity) throw new Error("库存不足");
  row.quantity = quantity;
  row.unitPrice = Number(product.price);
  row.totalAmount = Number((quantity * Number(product.price)).toFixed(2));
  row.updateTime = nowText();
  writeDb(db);
  return row;
}

export async function mockDeleteCartItem(id) {
  await sleep();
  const db = readDb();
  const idx = db.cartItems.findIndex((x) => x.id === Number(id));
  if (idx < 0) throw new Error("购物车项不存在");
  db.cartItems.splice(idx, 1);
  writeDb(db);
  return true;
}

export async function mockUserStats(params = {}) {
  await sleep();
  const db = readDb();
  const { pageNum = 1, pageSize = 10, startTime, endTime } = params;
  const users = db.users.filter((x) => x.role === 2).sort((a, b) => b.id - a.id);
  const records = users.map((u) => {
    const items = db.cartItems.filter((c) => c.userId === u.id && inRange(c.createTime, startTime, endTime));
    const totalQuantity = items.reduce((sum, x) => sum + Number(x.quantity), 0);
    const totalAmount = items.reduce((sum, x) => sum + Number(x.totalAmount), 0);
    return {
      userId: u.id,
      username: u.username,
      totalQuantity,
      totalAmount: Number(totalAmount.toFixed(2))
    };
  });
  return pageResult(records, Number(pageNum), Number(pageSize));
}

export async function mockStatsSummary(params = {}) {
  await sleep();
  const db = readDb();
  const { startTime, endTime } = params;
  const rows = db.cartItems.filter((x) => inRange(x.createTime, startTime, endTime));
  const activeUserCount = new Set(rows.map((x) => x.userId)).size;
  const totalQuantity = rows.reduce((sum, x) => sum + Number(x.quantity), 0);
  const totalAmount = rows.reduce((sum, x) => sum + Number(x.totalAmount), 0);
  return {
    activeUserCount,
    totalQuantity,
    totalAmount: Number(totalAmount.toFixed(2))
  };
}

export async function mockGenerateStatsReport(params = {}) {
  await sleep();
  return {
    statDate: params?.statDate || todayText(),
    userCount: readDb().users.filter((x) => x.role === 2).length
  };
}
