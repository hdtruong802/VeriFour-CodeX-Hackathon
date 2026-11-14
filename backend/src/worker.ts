type Role = "student" | "admin";
type AccountStatus = "active" | "locked" | "suspended";
type AccessOutcome = "success" | "failed" | "blocked";
type PointDestination = "fundraising" | "rewards";
type Session = { sub: string; role: Role; stationId?: string | null; tv?: number; exp: number };
type UserRow = {
  id: string;
  email: string;
  password_hash: string;
  password_salt: string;
  role: Role;
  name: string;
  student_id: string | null;
  faculty: string | null;
  station_id: string | null;
  points: number;
  total_kg: number;
  status?: AccountStatus;
  last_login_at?: number | null;
  token_version?: number;
  created_at?: number;
};
type TransactionRow = {
  id: string;
  station_id: string;
  admin_id: string;
  student_id: string | null;
  items_json: string;
  total_quantity: number;
  weight_kg: number;
  points: number;
  status: "pending" | "confirmed" | "expired" | "cancelled";
  created_at: number;
  expires_at: number;
  confirmed_at: number | null;
};
type TransactionViewRow = TransactionRow & {
  station_name: string;
  student_name: string | null;
  student_faculty: string | null;
};
type StationRow = {
  id: string;
  name: string;
  address: string;
  latitude: number | null;
  longitude: number | null;
  capacity_kg: number;
  is_active?: number;
  filled_kg: number;
};
type AccessLogRow = {
  id: string;
  user_id: string | null;
  email: string;
  role: string | null;
  event_type: string;
  outcome: AccessOutcome;
  ip_address: string | null;
  user_agent: string | null;
  detail: string | null;
  created_at: number;
};
type AdminInviteRow = {
  id: string;
  code_preview: string;
  label: string;
  station_id: string;
  station_name?: string;
  max_uses: number;
  used_count: number;
  expires_at: number;
  revoked_at: number | null;
  created_by: string | null;
  created_at: number;
};
type TransferRow = {
  id: string;
  station_id: string;
  station_name: string;
  admin_id: string;
  weight_kg: number;
  created_at: number;
};
type DemoCampaignMetric = { label: string; kg: number };
type DemoCampaignSummary = {
  registeredStudents: number;
  participantCount: number;
  transactionCount: number;
  totalKg: number;
  totalPoints: number;
  fundraisingPoints: number;
  fundraisingPercent: number;
  byType: DemoCampaignMetric[];
  byQuality: DemoCampaignMetric[];
};
type AuthThrottleRow = {
  throttle_key?: string;
  email_hint?: string | null;
  failure_count: number;
  window_started_at: number;
  locked_until: number;
  updated_at?: number;
};
type ItemInput = { plasticType: string; quantity: number; cleanliness: string };

const TOKEN_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
const PASSWORD_ITERATIONS = 16_384;
const LEGACY_PASSWORD_ITERATIONS = 310_000;
const AUTH_FAILURE_WINDOW_MS = 15 * 60_000;
const AUTH_MAX_FAILURES = 5;
const AUTH_LOCK_MS = 15 * 60_000;
const POINT_BASE: Record<string, number> = { PET: 10, HDPE: 10, PP: 8, LDPE: 2, PS: 1 };
const CLEAN_MULTIPLIER: Record<string, number> = { CLEAN: 1.5, NORMAL: 1, DIRTY: 0.5 };

class HttpError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
  }
}

function headers(request: Request, env: Env) {
  const requestOrigin = request.headers.get("origin");
  const allowedOrigins = env.ALLOWED_ORIGIN.split(",").map((origin) => origin.trim());
  const origin =
    requestOrigin && allowedOrigins.includes(requestOrigin) ? requestOrigin : allowedOrigins[0];
  return {
    "access-control-allow-origin": origin,
    "access-control-allow-headers": "authorization, content-type",
    "access-control-allow-methods": "GET, POST, OPTIONS",
    "content-type": "application/json; charset=utf-8",
    vary: "origin",
  };
}

function json(request: Request, env: Env, body: unknown, status = 200) {
  return Response.json(body, { status, headers: headers(request, env) });
}

function requireString(value: unknown, field: string) {
  if (typeof value !== "string" || !value.trim()) throw new HttpError(400, `${field} is required`);
  return value.trim();
}

function optionalString(value: unknown) {
  return typeof value === "string" && value.trim() ? value.trim() : null;
}

async function body(request: Request) {
  try {
    const result = (await request.json()) as unknown;
    if (!result || typeof result !== "object" || Array.isArray(result)) {
      throw new HttpError(400, "JSON object expected");
    }
    return result as Record<string, unknown>;
  } catch (error) {
    if (error instanceof HttpError) throw error;
    throw new HttpError(400, "Invalid JSON body");
  }
}

function bytesToBase64Url(bytes: Uint8Array) {
  let text = "";
  bytes.forEach((byte) => {
    text += String.fromCharCode(byte);
  });
  return btoa(text).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
}

function base64UrlToBytes(value: string) {
  const padded = value
    .replaceAll("-", "+")
    .replaceAll("_", "/")
    .padEnd(Math.ceil(value.length / 4) * 4, "=");
  const text = atob(padded);
  return Uint8Array.from(text, (character) => character.charCodeAt(0));
}

async function digest(value: string) {
  const bytes = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return bytesToBase64Url(new Uint8Array(bytes));
}

async function timingSafeDigestEqual(first: string, second: string) {
  const encoder = new TextEncoder();
  const [firstHash, secondHash] = await Promise.all([
    crypto.subtle.digest("SHA-256", encoder.encode(first)),
    crypto.subtle.digest("SHA-256", encoder.encode(second)),
  ]);
  return crypto.subtle.timingSafeEqual(firstHash, secondHash);
}

async function pepperPassword(password: string, pepper: string) {
  const pepperKey = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(pepper),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  return crypto.subtle.sign("HMAC", pepperKey, new TextEncoder().encode(password));
}

async function passwordHash(
  password: string,
  salt: Uint8Array,
  iterations: number,
  pepper?: string,
) {
  const material = pepper
    ? await pepperPassword(password, pepper)
    : new TextEncoder().encode(password);
  const passwordKey = await crypto.subtle.importKey("raw", material, "PBKDF2", false, [
    "deriveBits",
  ]);
  const derived = await crypto.subtle.deriveBits(
    { name: "PBKDF2", salt, iterations, hash: "SHA-256" },
    passwordKey,
    256,
  );
  return bytesToBase64Url(new Uint8Array(derived));
}

async function passwordRecord(
  password: string,
  pepper: string,
  salt = crypto.getRandomValues(new Uint8Array(16)),
) {
  const hash = await passwordHash(password, salt, PASSWORD_ITERATIONS, pepper);
  return { hash: `v2$${PASSWORD_ITERATIONS}$${hash}`, salt: bytesToBase64Url(salt) };
}

async function verifyPassword(password: string, storedHash: string, salt: string, pepper: string) {
  const saltBytes = base64UrlToBytes(salt);
  const versionedHash = /^v2\$(\d+)\$([A-Za-z0-9_-]+)$/.exec(storedHash);
  if (versionedHash && Number(versionedHash[1]) === PASSWORD_ITERATIONS) {
    const hash = await passwordHash(password, saltBytes, PASSWORD_ITERATIONS, pepper);
    return timingSafeDigestEqual(hash, versionedHash[2]);
  }

  // There are no production users before the Free-plan migration. This fallback only preserves
  // compatibility for an interrupted earlier deployment; its cost is too high for Free Workers.
  const legacyHash = await passwordHash(password, saltBytes, LEGACY_PASSWORD_ITERATIONS);
  return timingSafeDigestEqual(legacyHash, storedHash);
}

async function enforceAuthRateLimit(env: Env, action: "login" | "register", email: string) {
  const identityKey = await digest(`${action}:${email}`);
  const [globalAttempt, identityAttempt] = await Promise.all([
    env.AUTH_GLOBAL_LIMIT.limit({ key: action }),
    env.AUTH_IDENTITY_LIMIT.limit({ key: identityKey }),
  ]);
  if (!globalAttempt.success || !identityAttempt.success) {
    throw new HttpError(429, "Too many attempts. Please wait one minute and try again");
  }
}

async function loginThrottle(env: Env, email: string) {
  const throttleKey = await digest(`login:${email}`);
  const throttle = await env.DB.prepare(
    "SELECT failure_count, window_started_at, locked_until FROM auth_throttles WHERE throttle_key = ?",
  )
    .bind(throttleKey)
    .first<AuthThrottleRow>();
  if (throttle && throttle.locked_until > Date.now()) {
    const retryMinutes = Math.max(1, Math.ceil((throttle.locked_until - Date.now()) / 60_000));
    throw new HttpError(429, `Too many failed attempts. Try again in ${retryMinutes} minute(s)`);
  }
  return throttleKey;
}

async function recordLoginFailure(env: Env, throttleKey: string, email: string) {
  const now = Date.now();
  const windowExpiredAt = now - AUTH_FAILURE_WINDOW_MS;
  await env.DB.prepare(
    `INSERT INTO auth_throttles (throttle_key, email_hint, failure_count, window_started_at, locked_until, updated_at)
     VALUES (?, ?, 1, ?, 0, ?)
     ON CONFLICT(throttle_key) DO UPDATE SET
       email_hint = COALESCE(excluded.email_hint, auth_throttles.email_hint),
       failure_count = CASE
         WHEN auth_throttles.window_started_at <= ? THEN 1
         ELSE auth_throttles.failure_count + 1
       END,
       window_started_at = CASE
         WHEN auth_throttles.window_started_at <= ? THEN excluded.window_started_at
         ELSE auth_throttles.window_started_at
       END,
       locked_until = CASE
         WHEN auth_throttles.window_started_at <= ? THEN 0
         WHEN auth_throttles.failure_count + 1 >= ? THEN ?
         ELSE auth_throttles.locked_until
       END,
       updated_at = excluded.updated_at`,
  )
    .bind(
      throttleKey,
      email,
      now,
      now,
      windowExpiredAt,
      windowExpiredAt,
      windowExpiredAt,
      AUTH_MAX_FAILURES,
      now + AUTH_LOCK_MS,
    )
    .run();
}

async function clearLoginThrottle(env: Env, throttleKey: string) {
  await env.DB.prepare("DELETE FROM auth_throttles WHERE throttle_key = ?").bind(throttleKey).run();
}

async function signSession(session: Session, secret: string) {
  const payload = bytesToBase64Url(new TextEncoder().encode(JSON.stringify(session)));
  const signature = await digest(`${payload}.${secret}`);
  return `${payload}.${signature}`;
}

async function readSession(request: Request, env: Env): Promise<Session> {
  const value = request.headers.get("authorization");
  if (!value?.startsWith("Bearer ")) throw new HttpError(401, "Authentication required");
  const [payload, signature] = value.slice(7).split(".");
  if (
    !payload ||
    !signature ||
    !(await timingSafeDigestEqual(signature, await digest(`${payload}.${env.AUTH_SECRET}`)))
  ) {
    throw new HttpError(401, "Invalid session");
  }
  try {
    const session = JSON.parse(new TextDecoder().decode(base64UrlToBytes(payload))) as Session;
    if (!session.sub || !session.role || session.exp < Date.now()) throw new Error();
    return session;
  } catch {
    throw new HttpError(401, "Invalid session");
  }
}

async function currentUser(request: Request, env: Env) {
  const session = await readSession(request, env);
  const user = await env.DB.prepare(
    `SELECT id, email, password_hash, password_salt, role, name, student_id, faculty, station_id,
      points, total_kg, COALESCE(status, 'active') AS status, last_login_at,
      COALESCE(token_version, 1) AS token_version, created_at
     FROM users WHERE id = ?`,
  )
    .bind(session.sub)
    .first<UserRow>();
  if (!user) throw new HttpError(401, "Session user no longer exists");
  if ((user.status ?? "active") !== "active") {
    throw new HttpError(403, "Account is locked or suspended");
  }
  if ((session.tv ?? 1) !== (user.token_version ?? 1)) {
    throw new HttpError(401, "Session token has been revoked");
  }
  return { session, user };
}

async function logAccess(
  env: Env,
  params: {
    userId?: string | null;
    email: string;
    role?: string | null;
    eventType: string;
    outcome: AccessOutcome;
    request?: Request;
    detail?: string | null;
  },
) {
  const ip = params.request?.headers.get("cf-connecting-ip") ?? params.request?.headers.get("x-forwarded-for") ?? null;
  const ua = params.request?.headers.get("user-agent")?.slice(0, 140) ?? null;
  await env.DB.prepare(
    `INSERT INTO access_logs (id, user_id, email, role, event_type, outcome, ip_address, user_agent, detail, created_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
  )
    .bind(
      randomId(),
      params.userId ?? null,
      params.email,
      params.role ?? null,
      params.eventType,
      params.outcome,
      ip,
      ua,
      params.detail ?? null,
      Date.now(),
    )
    .run();
}

async function logAudit(
  env: Env,
  actorId: string | null,
  action: string,
  entityType: string,
  entityId: string,
  metadata?: Record<string, unknown>,
) {
  await env.DB.prepare(
    `INSERT INTO audit_events (id, actor_id, action, entity_type, entity_id, metadata_json, created_at)
     VALUES (?, ?, ?, ?, ?, ?, ?)`,
  )
    .bind(randomId(), actorId, action, entityType, entityId, metadata ? JSON.stringify(metadata) : null, Date.now())
    .run();
}

async function publicUser(env: Env, user: UserRow) {
  const allocation = await env.DB.prepare(
    `SELECT COALESCE(SUM(CAST(json_extract(metadata_json, '$.points') AS INTEGER)), 0) AS fund_points
     FROM audit_events
     WHERE actor_id = ?
       AND action = 'points_allocated'
       AND json_extract(metadata_json, '$.destination') = 'fundraising'`,
  )
    .bind(user.id)
    .first<{ fund_points: number }>();
  const fundPoints = Math.max(0, allocation?.fund_points ?? 0);
  return {
    id: user.id,
    email: user.email,
    role: user.role,
    name: user.name,
    studentId: user.student_id,
    faculty: user.faculty,
    stationId: user.station_id,
    points: user.points,
    rewardPoints: Math.max(0, user.points - fundPoints),
    fundPoints,
    totalKg: user.total_kg,
  };
}

function randomId() {
  return crypto.randomUUID();
}

function randomToken() {
  const bytes = crypto.getRandomValues(new Uint8Array(12));
  return Array.from(bytes, (byte) => TOKEN_ALPHABET[byte % TOKEN_ALPHABET.length]).join("");
}

function calculate(items: ItemInput[]) {
  if (!Array.isArray(items) || items.length === 0 || items.length > 5) {
    throw new HttpError(400, "A transaction needs 1–5 plastic lines");
  }
  const normalized = items.map((item) => {
    if (!POINT_BASE[item.plasticType] || !CLEAN_MULTIPLIER[item.cleanliness]) {
      throw new HttpError(400, "Unsupported plastic type or cleanliness");
    }
    const quantity = Math.round(item.quantity * 100) / 100;
    if (
      !Number.isFinite(item.quantity) ||
      item.quantity < 0.01 ||
      item.quantity > 200 ||
      Math.abs(item.quantity - quantity) > Number.EPSILON
    ) {
      throw new HttpError(400, "Weight must be a number from 0.01 to 200 kg with up to 2 decimals");
    }
    return {
      plasticType: item.plasticType,
      cleanliness: item.cleanliness,
      quantity,
      points: Math.round(
        quantity * POINT_BASE[item.plasticType] * CLEAN_MULTIPLIER[item.cleanliness],
      ),
    };
  });
  const totalQuantity = +normalized.reduce((sum, item) => sum + item.quantity, 0).toFixed(2);
  return {
    items: normalized,
    points: normalized.reduce((sum, item) => sum + item.points, 0),
    totalQuantity,
    weightKg: totalQuantity,
  };
}

async function stationData(env: Env, includeInactive = false) {
  const whereClause = includeInactive ? "" : "WHERE s.is_active = 1";
  const result = await env.DB.prepare(
    `SELECT
      s.id, s.name, s.address, s.latitude, s.longitude, s.capacity_kg, s.is_active,
      COALESCE((SELECT SUM(t.weight_kg) FROM transactions t WHERE t.station_id = s.id AND t.status = 'confirmed'), 0)
      - COALESCE((SELECT SUM(r.weight_kg) FROM station_transfers r WHERE r.station_id = s.id), 0) AS filled_kg
    FROM stations s
    ${whereClause}
    ORDER BY s.name`,
  ).all<StationRow>();
  return result.results.map((station) => ({
    id: station.id,
    name: station.name,
    address: station.address,
    latitude: station.latitude,
    longitude: station.longitude,
    capacityKg: station.capacity_kg,
    isActive: (station.is_active ?? 1) === 1,
    filledKg: +Math.max(0, station.filled_kg).toFixed(2),
    fillPercent: Math.min(
      100,
      Math.round((Math.max(0, station.filled_kg) / station.capacity_kg) * 100),
    ),
  }));
}

async function stations(request: Request, env: Env) {
  return json(request, env, await stationData(env));
}

function effectiveStatus(transaction: TransactionRow) {
  return transaction.status === "pending" && Date.now() > transaction.expires_at
    ? "expired"
    : transaction.status;
}

function transactionView(transaction: TransactionViewRow, token?: string, includeStudent = true) {
  return {
    id: transaction.id,
    ...(token ? { token } : {}),
    userId: includeStudent ? transaction.student_id : null,
    userName: includeStudent ? transaction.student_name : null,
    userFaculty: includeStudent ? transaction.student_faculty : null,
    adminId: transaction.admin_id,
    stationId: transaction.station_id,
    collectionPoint: transaction.station_name,
    items: JSON.parse(transaction.items_json),
    totalQuantity: transaction.total_quantity,
    weightKg: transaction.weight_kg,
    points: transaction.points,
    status: effectiveStatus(transaction),
    createdAt: transaction.created_at,
    expiresAt: transaction.expires_at,
    confirmedAt: transaction.confirmed_at,
  };
}

async function transactionData(env: Env, user: UserRow) {
  const scope =
    user.role === "admin"
      ? { clause: "t.station_id = ?", value: user.station_id }
      : { clause: "t.student_id = ?", value: user.id };
  if (!scope.value) return [];
  const rows = await env.DB.prepare(
    `SELECT t.id, t.station_id, t.admin_id, t.student_id, t.items_json, t.total_quantity,
      t.weight_kg, t.points, t.status, t.created_at, t.expires_at, t.confirmed_at,
      s.name AS station_name, u.name AS student_name, u.faculty AS student_faculty
     FROM transactions t
     JOIN stations s ON s.id = t.station_id
     LEFT JOIN users u ON u.id = t.student_id
     WHERE ${scope.clause}
     ORDER BY t.created_at DESC
     LIMIT 250`,
  )
    .bind(scope.value)
    .all<TransactionViewRow>();
  return rows.results.map((transaction) => transactionView(transaction));
}

async function transferData(env: Env, user: UserRow) {
  if (user.role !== "admin" || !user.station_id) return [];
  const rows = await env.DB.prepare(
    `SELECT r.id, r.station_id, s.name AS station_name, r.admin_id, r.weight_kg, r.created_at
     FROM station_transfers r
     JOIN stations s ON s.id = r.station_id
     WHERE r.station_id = ?
     ORDER BY r.created_at DESC
     LIMIT 100`,
  )
    .bind(user.station_id)
    .all<TransferRow>();
  return rows.results.map((transfer) => ({
    id: transfer.id,
    stationId: transfer.station_id,
    collectionPoint: transfer.station_name,
    weightKg: transfer.weight_kg,
    adminId: transfer.admin_id,
    createdAt: transfer.created_at,
  }));
}

async function leaderboardData(env: Env, period: "week" | "all") {
  const from =
    period === "week"
      ? (() => {
          const date = new Date();
          const day = date.getDay() || 7;
          date.setDate(date.getDate() - day + 1);
          date.setHours(0, 0, 0, 0);
          return date.getTime();
        })()
      : 0;
  const rows = await env.DB.prepare(
    `SELECT u.id AS user_id, u.name, COALESCE(u.faculty, 'Khác') AS faculty,
      COALESCE(SUM(t.points), 0) AS points, COALESCE(SUM(t.weight_kg), 0) AS kg
     FROM users u
     JOIN transactions t ON t.student_id = u.id
     WHERE t.status = 'confirmed' AND t.confirmed_at >= ?
     GROUP BY u.id, u.name, u.faculty
     ORDER BY points DESC, kg DESC
     LIMIT 100`,
  )
    .bind(from)
    .all<{ user_id: string; name: string; faculty: string; points: number; kg: number }>();
  return rows.results.map((row) => ({
    userId: row.user_id,
    name: row.name,
    faculty: row.faculty,
    points: row.points,
    kg: +row.kg.toFixed(2),
  }));
}

async function demoCampaignData(env: Env): Promise<DemoCampaignSummary> {
  const [students, totals, participants, allocation, typeRows, qualityRows] = await env.DB.batch([
    env.DB.prepare("SELECT COUNT(*) AS count FROM users WHERE id LIKE 'demo-student-%'"),
    env.DB.prepare(
      `SELECT COUNT(*) AS transaction_count, COALESCE(SUM(weight_kg), 0) AS total_kg,
        COALESCE(SUM(points), 0) AS total_points
       FROM transactions WHERE id LIKE 'demo-2026-%' AND status = 'confirmed'`,
    ),
    env.DB.prepare(
      `SELECT COUNT(DISTINCT student_id) AS count
       FROM transactions WHERE id LIKE 'demo-2026-%' AND status = 'confirmed'`,
    ),
    env.DB.prepare(
      `SELECT COALESCE(SUM(CAST(json_extract(a.metadata_json, '$.points') AS INTEGER)), 0) AS points
       FROM audit_events a
       JOIN transactions t ON t.id = a.entity_id
       WHERE t.id LIKE 'demo-2026-%'
         AND a.action = 'points_allocated'
         AND json_extract(a.metadata_json, '$.destination') = 'fundraising'`,
    ),
    env.DB.prepare(
      `SELECT json_extract(item.value, '$.plasticType') AS label,
        COALESCE(SUM(CAST(json_extract(item.value, '$.quantity') AS REAL)), 0) AS kg
       FROM transactions t, json_each(t.items_json) AS item
       WHERE t.id LIKE 'demo-2026-%' AND t.status = 'confirmed'
       GROUP BY label
       ORDER BY CASE label WHEN 'PET' THEN 1 WHEN 'PP' THEN 2 WHEN 'PS' THEN 3 WHEN 'HDPE' THEN 4 WHEN 'LDPE' THEN 5 ELSE 6 END`,
    ),
    env.DB.prepare(
      `SELECT json_extract(item.value, '$.cleanliness') AS label,
        COALESCE(SUM(CAST(json_extract(item.value, '$.quantity') AS REAL)), 0) AS kg
       FROM transactions t, json_each(t.items_json) AS item
       WHERE t.id LIKE 'demo-2026-%' AND t.status = 'confirmed'
       GROUP BY label
       ORDER BY CASE label WHEN 'CLEAN' THEN 1 WHEN 'NORMAL' THEN 2 WHEN 'DIRTY' THEN 3 ELSE 4 END`,
    ),
  ]);
  const studentCount = (students.results[0] as { count: number } | undefined)?.count ?? 0;
  const summary = totals.results[0] as
    { transaction_count: number; total_kg: number; total_points: number } | undefined;
  const participantCount = (participants.results[0] as { count: number } | undefined)?.count ?? 0;
  const fundraisingPoints = (allocation.results[0] as { points: number } | undefined)?.points ?? 0;
  const totalPoints = summary?.total_points ?? 0;
  const metrics = (result: D1Result<DemoCampaignMetric>) =>
    result.results.map((row) => ({ label: row.label, kg: +row.kg.toFixed(2) }));
  return {
    registeredStudents: studentCount,
    participantCount,
    transactionCount: summary?.transaction_count ?? 0,
    totalKg: +(summary?.total_kg ?? 0).toFixed(2),
    totalPoints,
    fundraisingPoints,
    fundraisingPercent: totalPoints ? Math.round((fundraisingPoints / totalPoints) * 100) : 0,
    byType: metrics(typeRows as D1Result<DemoCampaignMetric>),
    byQuality: metrics(qualityRows as D1Result<DemoCampaignMetric>),
  };
}

async function handleApi(request: Request, env: Env) {
  const url = new URL(request.url);
  const path = url.pathname.replace(/^\/api\/v1/, "") || "/";

  if (request.method === "GET" && path === "/health") return json(request, env, { ok: true });
  if (request.method === "GET" && path === "/stations") return stations(request, env);
  if (request.method === "GET" && path === "/leaderboard") {
    const period = url.searchParams.get("period") === "week" ? "week" : "all";
    return json(request, env, { period, rows: await leaderboardData(env, period) });
  }

  if (request.method === "POST" && path === "/auth/register") {
    const input = await body(request);
    const email = requireString(input.email, "email").toLowerCase();
    const name = requireString(input.name, "name");
    const password = requireString(input.password, "password");
    if (!/^\S+@\S+\.\S+$/.test(email) || password.length < 8) {
      throw new HttpError(400, "Use a valid email and a password of at least 8 characters");
    }
    await enforceAuthRateLimit(env, "register", email);
    const existing = await env.DB.prepare("SELECT id FROM users WHERE email = ?")
      .bind(email)
      .first();
    if (existing) throw new HttpError(409, "Email already registered");
    const wantsAdmin = input.role === "admin";
    const role: Role = wantsAdmin ? "admin" : "student";
    const stationId = wantsAdmin ? requireString(input.stationId, "stationId") : null;
    if (wantsAdmin) {
      const providedCode = typeof input.inviteCode === "string" ? input.inviteCode.trim() : "";
      const matchesMasterCode =
        !!env.ADMIN_INVITE_CODE &&
        !!providedCode &&
        (await timingSafeDigestEqual(providedCode, env.ADMIN_INVITE_CODE));
      let usedManagedInviteId: string | null = null;
      if (!matchesMasterCode && providedCode) {
        const nowCheck = Date.now();
        const inviteRow = await env.DB.prepare(
          `SELECT id, station_id, max_uses, used_count, expires_at, revoked_at
           FROM admin_invites
           WHERE code_hash = ?`,
        )
          .bind(await digest(providedCode))
          .first<{
            id: string;
            station_id: string;
            max_uses: number;
            used_count: number;
            expires_at: number;
            revoked_at: number | null;
          }>();
        if (
          inviteRow &&
          inviteRow.revoked_at === null &&
          inviteRow.expires_at > nowCheck &&
          inviteRow.used_count < inviteRow.max_uses &&
          inviteRow.station_id === stationId
        ) {
          usedManagedInviteId = inviteRow.id;
        }
      }
      if (!matchesMasterCode && !usedManagedInviteId) {
        await logAccess(env, {
          email,
          role: "admin",
          eventType: "register_admin_denied",
          outcome: "blocked",
          request,
          detail: "Invalid or expired admin invite code",
        });
        throw new HttpError(403, "A valid admin invite is required");
      }
      if (usedManagedInviteId) {
        await env.DB.prepare(
          "UPDATE admin_invites SET used_count = used_count + 1 WHERE id = ? AND used_count < max_uses",
        )
          .bind(usedManagedInviteId)
          .run();
      }
      const station = await env.DB.prepare("SELECT id FROM stations WHERE id = ? AND is_active = 1")
        .bind(stationId)
        .first();
      if (!station) throw new HttpError(400, "Station is not active");
    }
    const passwordData = await passwordRecord(password, env.PASSWORD_PEPPER);
    const now = Date.now();
    const user: UserRow = {
      id: randomId(),
      email,
      password_hash: passwordData.hash,
      password_salt: passwordData.salt,
      role,
      name,
      student_id: optionalString(input.studentId),
      faculty: optionalString(input.faculty),
      station_id: stationId,
      points: 0,
      total_kg: 0,
      status: "active",
      last_login_at: now,
      token_version: 1,
      created_at: now,
    };
    await env.DB.prepare(
      `INSERT INTO users (id, email, password_hash, password_salt, role, name, student_id, faculty, station_id, points, total_kg, status, last_login_at, token_version, created_at, updated_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 'active', ?, 1, ?, ?)`,
    )
      .bind(
        user.id,
        user.email,
        user.password_hash,
        user.password_salt,
        user.role,
        user.name,
        user.student_id,
        user.faculty,
        user.station_id,
        now,
        now,
        now,
      )
      .run();
    await logAccess(env, {
      userId: user.id,
      email: user.email,
      role: user.role,
      eventType: "register",
      outcome: "success",
      request,
      detail: wantsAdmin ? `Admin registered for station ${stationId}` : "Student registered",
    });
    const token = await signSession(
      { sub: user.id, role: user.role, stationId, tv: 1, exp: now + 7 * 86_400_000 },
      env.AUTH_SECRET,
    );
    return json(request, env, { token, user: await publicUser(env, user) }, 201);
  }

  if (request.method === "POST" && path === "/auth/login") {
    const input = await body(request);
    const email = requireString(input.email, "email").toLowerCase();
    const password = requireString(input.password, "password");
    await enforceAuthRateLimit(env, "login", email);
    let throttleKey: string;
    try {
      throttleKey = await loginThrottle(env, email);
    } catch (error) {
      await logAccess(env, {
        email,
        eventType: "login_locked",
        outcome: "blocked",
        request,
        detail: error instanceof HttpError ? error.message : "Rate limit lockout",
      });
      throw error;
    }
    const user = await env.DB.prepare(
      `SELECT id, email, password_hash, password_salt, role, name, student_id, faculty, station_id,
        points, total_kg, COALESCE(status, 'active') AS status, last_login_at,
        COALESCE(token_version, 1) AS token_version
       FROM users WHERE email = ?`,
    )
      .bind(email)
      .first<UserRow>();
    if (
      !user ||
      !(await verifyPassword(password, user.password_hash, user.password_salt, env.PASSWORD_PEPPER))
    ) {
      await recordLoginFailure(env, throttleKey, email);
      await logAccess(env, {
        userId: user?.id ?? null,
        email,
        role: user?.role ?? null,
        eventType: "login_failed",
        outcome: "failed",
        request,
        detail: "Invalid email or password",
      });
      throw new HttpError(401, "Email or password is incorrect");
    }
    if ((user.status ?? "active") !== "active") {
      await logAccess(env, {
        userId: user.id,
        email: user.email,
        role: user.role,
        eventType: "login_blocked_account_status",
        outcome: "blocked",
        request,
        detail: `Account status is ${user.status}`,
      });
      throw new HttpError(403, `Account is ${user.status}`);
    }
    const now = Date.now();
    await clearLoginThrottle(env, throttleKey);
    await env.DB.prepare("UPDATE users SET last_login_at = ?, updated_at = ? WHERE id = ?")
      .bind(now, now, user.id)
      .run();
    await logAccess(env, {
      userId: user.id,
      email: user.email,
      role: user.role,
      eventType: "login_success",
      outcome: "success",
      request,
      detail: user.station_id ? `Station ${user.station_id}` : "Student login",
    });
    const token = await signSession(
      {
        sub: user.id,
        role: user.role,
        stationId: user.station_id,
        tv: user.token_version ?? 1,
        exp: now + 7 * 86_400_000,
      },
      env.AUTH_SECRET,
    );
    return json(request, env, { token, user: await publicUser(env, user) });
  }

  if (request.method === "GET" && path === "/me") {
    const { user } = await currentUser(request, env);
    return json(request, env, { user: await publicUser(env, user) });
  }

  if (request.method === "GET" && path === "/snapshot") {
    const { user } = await currentUser(request, env);
    const [profile, stations, transactions, collectionRuns, demoCampaign] = await Promise.all([
      publicUser(env, user),
      stationData(env),
      transactionData(env, user),
      transferData(env, user),
      user.role === "admin" ? demoCampaignData(env) : Promise.resolve(null),
    ]);
    return json(request, env, {
      user: profile,
      stations,
      transactions,
      collectionRuns,
      demoCampaign,
    });
  }

  if (request.method === "POST" && path === "/admin/transactions") {
    const { user } = await currentUser(request, env);
    if (user.role !== "admin" || !user.station_id)
      throw new HttpError(403, "Station admin access required");
    const input = await body(request);
    const calculated = calculate(input.items as ItemInput[]);
    const now = Date.now();
    const token = randomToken();
    const id = randomId();
    const expiresAt = now + 5 * 60_000;
    await env.DB.prepare(
      `INSERT INTO transactions (id, token_hash, station_id, admin_id, student_id, items_json, total_quantity, weight_kg, points, status, created_at, expires_at)
       VALUES (?, ?, ?, ?, NULL, ?, ?, ?, ?, 'pending', ?, ?)`,
    )
      .bind(
        id,
        await digest(token),
        user.station_id,
        user.id,
        JSON.stringify(calculated.items),
        calculated.totalQuantity,
        calculated.weightKg,
        calculated.points,
        now,
        expiresAt,
      )
      .run();
    const station = await env.DB.prepare("SELECT name FROM stations WHERE id = ?")
      .bind(user.station_id)
      .first<{ name: string }>();
    return json(
      request,
      env,
      {
        id,
        token,
        userId: null,
        userName: null,
        userFaculty: null,
        adminId: user.id,
        stationId: user.station_id,
        collectionPoint: station?.name ?? user.station_id,
        ...calculated,
        status: "pending",
        createdAt: now,
        expiresAt,
        confirmedAt: null,
      },
      201,
    );
  }

  const cancelMatch = path.match(/^\/admin\/transactions\/([0-9a-f-]+)\/cancel$/);
  if (request.method === "POST" && cancelMatch) {
    const { user } = await currentUser(request, env);
    if (user.role !== "admin" || !user.station_id)
      throw new HttpError(403, "Station admin access required");
    const now = Date.now();
    const result = await env.DB.prepare(
      `UPDATE transactions SET status = 'cancelled', cancelled_at = ?
       WHERE id = ? AND admin_id = ? AND station_id = ? AND status = 'pending'`,
    )
      .bind(now, cancelMatch[1], user.id, user.station_id)
      .run();
    if (result.meta.changes !== 1) throw new HttpError(409, "Transaction cannot be cancelled");
    return json(request, env, { id: cancelMatch[1], cancelledAt: now });
  }

  const transactionMatch = path.match(/^\/transactions\/([A-Z0-9]+)(?:\/(confirm))?$/);
  if (transactionMatch) {
    const [, token, action] = transactionMatch;
    const { user } = await currentUser(request, env);
    const transaction = await env.DB.prepare(
      `SELECT t.id, t.station_id, t.admin_id, t.student_id, t.items_json, t.total_quantity,
        t.weight_kg, t.points, t.status, t.created_at, t.expires_at, t.confirmed_at,
        s.name AS station_name, u.name AS student_name, u.faculty AS student_faculty
       FROM transactions t
       JOIN stations s ON s.id = t.station_id
       LEFT JOIN users u ON u.id = t.student_id
       WHERE t.token_hash = ?`,
    )
      .bind(await digest(token))
      .first<TransactionViewRow>();
    if (!transaction) throw new HttpError(404, "QR transaction not found");
    const currentStatus = effectiveStatus(transaction);
    if (currentStatus === "expired" && transaction.status === "pending") {
      await env.DB.prepare(
        "UPDATE transactions SET status = 'expired' WHERE id = ? AND status = 'pending'",
      )
        .bind(transaction.id)
        .run();
    }
    if (request.method === "GET" && !action) {
      const canSeeStudent =
        transaction.student_id === user.id ||
        (user.role === "admin" && user.station_id === transaction.station_id);
      return json(request, env, transactionView(transaction, token, canSeeStudent));
    }
    if (request.method === "POST" && action === "confirm") {
      if (user.role !== "student") throw new HttpError(403, "Student access required");
      if (currentStatus !== "pending") throw new HttpError(409, `Transaction is ${currentStatus}`);
      const input = request.headers.get("content-type")?.includes("application/json")
        ? await body(request)
        : {};
      const destination = input.destination ?? "rewards";
      if (destination !== "fundraising" && destination !== "rewards") {
        throw new HttpError(400, "Choose fundraising or rewards for the points");
      }
      const now = Date.now();
      const results = await env.DB.batch([
        env.DB.prepare(
          "UPDATE transactions SET status = 'confirmed', student_id = ?, confirmed_at = ? WHERE id = ? AND status = 'pending' AND expires_at > ?",
        ).bind(user.id, now, transaction.id, now),
        env.DB.prepare(
          `UPDATE users SET points = points + ?, total_kg = total_kg + ?, updated_at = ?
           WHERE id = ?
             AND NOT EXISTS (SELECT 1 FROM point_ledger WHERE transaction_id = ?)
             AND EXISTS (
               SELECT 1 FROM transactions
               WHERE id = ? AND status = 'confirmed' AND student_id = ?
             )`,
        ).bind(
          transaction.points,
          transaction.weight_kg,
          now,
          user.id,
          transaction.id,
          transaction.id,
          user.id,
        ),
        env.DB.prepare(
          `INSERT INTO point_ledger (id, user_id, transaction_id, points, created_at)
           SELECT ?, ?, id, points, ? FROM transactions
           WHERE id = ? AND status = 'confirmed' AND student_id = ?`,
        ).bind(randomId(), user.id, now, transaction.id, user.id),
        env.DB.prepare(
          `INSERT INTO audit_events (id, actor_id, action, entity_type, entity_id, metadata_json, created_at)
           SELECT ?, ?, 'points_allocated', 'transaction', id, ?, ? FROM transactions
           WHERE id = ? AND status = 'confirmed' AND student_id = ?`,
        ).bind(
          randomId(),
          user.id,
          JSON.stringify({
            destination: destination as PointDestination,
            points: transaction.points,
          }),
          now,
          transaction.id,
          user.id,
        ),
      ]);
      if (results[0].meta.changes !== 1)
        throw new HttpError(409, "Transaction has already been used");
      return json(request, env, {
        id: transaction.id,
        points: transaction.points,
        weightKg: transaction.weight_kg,
        destination,
        confirmedAt: now,
      });
    }
  }

  if (request.method === "POST" && path.match(/^\/admin\/stations\/[^/]+\/transfers$/)) {
    const stationId = path.split("/")[3];
    const { user } = await currentUser(request, env);
    if (user.role !== "admin" || user.station_id !== stationId)
      throw new HttpError(403, "Station admin access required");
    const stock = await env.DB.prepare(
      `SELECT
        COALESCE((SELECT SUM(weight_kg) FROM transactions WHERE station_id = ? AND status = 'confirmed'), 0)
        - COALESCE((SELECT SUM(weight_kg) FROM station_transfers WHERE station_id = ?), 0) AS filled_kg`,
    )
      .bind(stationId, stationId)
      .first<{ filled_kg: number }>();
    const weightKg = +Math.max(0, stock?.filled_kg ?? 0).toFixed(2);
    if (!weightKg) throw new HttpError(409, "Station is empty");
    const input = await body(request);
    const now = Date.now();
    await env.DB.prepare(
      "INSERT INTO station_transfers (id, station_id, admin_id, weight_kg, created_at, note) VALUES (?, ?, ?, ?, ?, ?)",
    )
      .bind(randomId(), stationId, user.id, weightKg, now, optionalString(input.note))
      .run();
    await logAudit(env, user.id, "station_transfer", "station", stationId, { weightKg });
    return json(request, env, { stationId, weightKg, createdAt: now }, 201);
  }

  // --- Admin Operations, Account, Token & Access Governance ---
  if (request.method === "GET" && path === "/admin/ops/overview") {
    const { user } = await currentUser(request, env);
    if (user.role !== "admin") throw new HttpError(403, "Admin access required");
    const now = Date.now();
    const last24h = now - 86_400_000;
    const [allStations, userCounts, txCounts, lockoutsRow, failedAccessRow, pendingByStation] =
      await Promise.all([
        stationData(env, true),
        env.DB.prepare(
          `SELECT
            COUNT(*) AS total_users,
            SUM(CASE WHEN role = 'student' THEN 1 ELSE 0 END) AS student_count,
            SUM(CASE WHEN role = 'admin' THEN 1 ELSE 0 END) AS admin_count,
            SUM(CASE WHEN COALESCE(status, 'active') = 'active' THEN 1 ELSE 0 END) AS active_users,
            SUM(CASE WHEN COALESCE(status, 'active') = 'locked' THEN 1 ELSE 0 END) AS locked_users,
            SUM(CASE WHEN COALESCE(status, 'active') = 'suspended' THEN 1 ELSE 0 END) AS suspended_users
           FROM users`,
        ).first<{
          total_users: number;
          student_count: number;
          admin_count: number;
          active_users: number;
          locked_users: number;
          suspended_users: number;
        }>(),
        env.DB.prepare(
          `SELECT
            SUM(CASE WHEN status = 'pending' AND expires_at > ? THEN 1 ELSE 0 END) AS active_qr,
            SUM(CASE WHEN status = 'expired' OR (status = 'pending' AND expires_at <= ?) THEN 1 ELSE 0 END) AS expired_qr,
            SUM(CASE WHEN status = 'confirmed' THEN 1 ELSE 0 END) AS confirmed_qr,
            SUM(CASE WHEN status = 'cancelled' THEN 1 ELSE 0 END) AS cancelled_qr
           FROM transactions`,
        )
          .bind(now, now)
          .first<{
            active_qr: number;
            expired_qr: number;
            confirmed_qr: number;
            cancelled_qr: number;
          }>(),
        env.DB.prepare("SELECT COUNT(*) AS count FROM auth_throttles WHERE locked_until > ?")
          .bind(now)
          .first<{ count: number }>(),
        env.DB.prepare(
          "SELECT COUNT(*) AS count FROM access_logs WHERE outcome != 'success' AND created_at >= ?",
        )
          .bind(last24h)
          .first<{ count: number }>(),
        env.DB.prepare(
          `SELECT station_id,
            SUM(CASE WHEN status = 'pending' AND expires_at > ? THEN 1 ELSE 0 END) AS pending_tokens,
            SUM(CASE WHEN status = 'confirmed' AND confirmed_at >= ? THEN 1 ELSE 0 END) AS confirmed_today
           FROM transactions
           GROUP BY station_id`,
        )
          .bind(now, last24h)
          .all<{ station_id: string; pending_tokens: number; confirmed_today: number }>(),
      ]);

    const perStationMap = new Map(pendingByStation.results.map((r) => [r.station_id, r]));
    return json(request, env, {
      stations: allStations.map((s) => ({
        ...s,
        pendingTokens: perStationMap.get(s.id)?.pending_tokens ?? 0,
        confirmedToday: perStationMap.get(s.id)?.confirmed_today ?? 0,
      })),
      accounts: {
        total: userCounts?.total_users ?? 0,
        students: userCounts?.student_count ?? 0,
        admins: userCounts?.admin_count ?? 0,
        active: userCounts?.active_users ?? 0,
        locked: userCounts?.locked_users ?? 0,
        suspended: userCounts?.suspended_users ?? 0,
      },
      tokens: {
        activeQr: txCounts?.active_qr ?? 0,
        expiredQr: txCounts?.expired_qr ?? 0,
        confirmedQr: txCounts?.confirmed_qr ?? 0,
        cancelledQr: txCounts?.cancelled_qr ?? 0,
      },
      security: {
        activeLockouts: lockoutsRow?.count ?? 0,
        failedOrBlocked24h: failedAccessRow?.count ?? 0,
      },
    });
  }

  if (request.method === "GET" && path === "/admin/users") {
    const { user } = await currentUser(request, env);
    if (user.role !== "admin") throw new HttpError(403, "Admin access required");
    const search = (url.searchParams.get("search") ?? "").trim().toLowerCase();
    const roleFilter = url.searchParams.get("role");
    const statusFilter = url.searchParams.get("status");

    const rows = await env.DB.prepare(
      `SELECT u.id, u.email, u.role, u.name, u.student_id, u.faculty, u.station_id,
        s.name AS station_name, u.points, u.total_kg,
        COALESCE(u.status, 'active') AS status,
        COALESCE(u.token_version, 1) AS token_version,
        u.last_login_at, u.created_at
       FROM users u
       LEFT JOIN stations s ON s.id = u.station_id
       ORDER BY CASE WHEN u.role = 'admin' THEN 0 ELSE 1 END, u.created_at DESC
       LIMIT 300`,
    ).all<{
      id: string;
      email: string;
      role: Role;
      name: string;
      student_id: string | null;
      faculty: string | null;
      station_id: string | null;
      station_name: string | null;
      points: number;
      total_kg: number;
      status: AccountStatus;
      token_version: number;
      last_login_at: number | null;
      created_at: number;
    }>();

    const filtered = rows.results.filter((r) => {
      if (roleFilter && roleFilter !== "all" && r.role !== roleFilter) return false;
      if (statusFilter && statusFilter !== "all" && r.status !== statusFilter) return false;
      if (search) {
        const hay = `${r.name} ${r.email} ${r.student_id ?? ""} ${r.faculty ?? ""} ${r.station_name ?? ""}`.toLowerCase();
        if (!hay.includes(search)) return false;
      }
      return true;
    });

    return json(
      request,
      env,
      filtered.map((r) => ({
        id: r.id,
        email: r.email,
        role: r.role,
        name: r.name,
        studentId: r.student_id,
        faculty: r.faculty,
        stationId: r.station_id,
        collectionPoint: r.station_name,
        points: r.points,
        totalKg: r.total_kg,
        status: r.status,
        tokenVersion: r.token_version,
        lastLoginAt: r.last_login_at,
        createdAt: r.created_at,
      })),
    );
  }

  if (request.method === "POST" && path === "/admin/users") {
    const { user: actor } = await currentUser(request, env);
    if (actor.role !== "admin") throw new HttpError(403, "Admin access required");
    const input = await body(request);
    const email = requireString(input.email, "email").toLowerCase();
    const name = requireString(input.name, "name");
    const password = requireString(input.password, "password");
    if (!/^\S+@\S+\.\S+$/.test(email) || password.length < 8) {
      throw new HttpError(400, "Use a valid email and password of at least 8 characters");
    }
    const existing = await env.DB.prepare("SELECT id FROM users WHERE email = ?").bind(email).first();
    if (existing) throw new HttpError(409, "Email already registered");
    const role: Role = input.role === "admin" ? "admin" : "student";
    const stationId = role === "admin" ? requireString(input.stationId, "stationId") : null;
    if (stationId) {
      const station = await env.DB.prepare("SELECT id FROM stations WHERE id = ?").bind(stationId).first();
      if (!station) throw new HttpError(400, "Station not found");
    }
    const passwordData = await passwordRecord(password, env.PASSWORD_PEPPER);
    const now = Date.now();
    const id = randomId();
    await env.DB.prepare(
      `INSERT INTO users (id, email, password_hash, password_salt, role, name, student_id, faculty, station_id, points, total_kg, status, token_version, created_at, updated_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 'active', 1, ?, ?)`,
    )
      .bind(
        id,
        email,
        passwordData.hash,
        passwordData.salt,
        role,
        name,
        optionalString(input.studentId),
        optionalString(input.faculty),
        stationId,
        now,
        now,
      )
      .run();
    await logAudit(env, actor.id, "account_created", "user", id, { email, role, stationId });
    await logAccess(env, {
      userId: actor.id,
      email: actor.email,
      role: actor.role,
      eventType: "account_created",
      outcome: "success",
      request,
      detail: `Created ${role} account for ${email}`,
    });
    return json(request, env, { id, email, name, role, stationId, status: "active", tokenVersion: 1, createdAt: now }, 201);
  }

  const userStatusMatch = path.match(/^\/admin\/users\/([^/]+)\/status$/);
  if (request.method === "POST" && userStatusMatch) {
    const targetId = userStatusMatch[1];
    const { user: actor } = await currentUser(request, env);
    if (actor.role !== "admin") throw new HttpError(403, "Admin access required");
    if (targetId === actor.id) throw new HttpError(400, "Cannot change your own account status");
    const input = await body(request);
    const nextStatus = input.status;
    if (nextStatus !== "active" && nextStatus !== "locked" && nextStatus !== "suspended") {
      throw new HttpError(400, "Invalid account status");
    }
    const now = Date.now();
    const bumpToken = nextStatus !== "active" ? 1 : 0;
    const result = await env.DB.prepare(
      `UPDATE users
       SET status = ?, token_version = COALESCE(token_version, 1) + ?, updated_at = ?
       WHERE id = ?`,
    )
      .bind(nextStatus, bumpToken, now, targetId)
      .run();
    if (result.meta.changes !== 1) throw new HttpError(404, "User not found");
    const target = await env.DB.prepare("SELECT email, role FROM users WHERE id = ?")
      .bind(targetId)
      .first<{ email: string; role: string }>();
    await logAudit(env, actor.id, "account_status_changed", "user", targetId, { status: nextStatus });
    await logAccess(env, {
      userId: actor.id,
      email: actor.email,
      role: actor.role,
      eventType: `account_${nextStatus}`,
      outcome: nextStatus === "active" ? "success" : "blocked",
      request,
      detail: `Set ${target?.email ?? targetId} status to ${nextStatus}`,
    });
    return json(request, env, { id: targetId, status: nextStatus, updatedAt: now });
  }

  const userRoleMatch = path.match(/^\/admin\/users\/([^/]+)\/role$/);
  if (request.method === "POST" && userRoleMatch) {
    const targetId = userRoleMatch[1];
    const { user: actor } = await currentUser(request, env);
    if (actor.role !== "admin") throw new HttpError(403, "Admin access required");
    const input = await body(request);
    const nextRole: Role = input.role === "admin" ? "admin" : "student";
    const stationId = nextRole === "admin" ? requireString(input.stationId, "stationId") : null;
    if (stationId) {
      const station = await env.DB.prepare("SELECT id FROM stations WHERE id = ?").bind(stationId).first();
      if (!station) throw new HttpError(400, "Station not found");
    }
    const now = Date.now();
    const result = await env.DB.prepare(
      "UPDATE users SET role = ?, station_id = ?, updated_at = ? WHERE id = ?",
    )
      .bind(nextRole, stationId, now, targetId)
      .run();
    if (result.meta.changes !== 1) throw new HttpError(404, "User not found");
    await logAudit(env, actor.id, "account_role_changed", "user", targetId, { role: nextRole, stationId });
    await logAccess(env, {
      userId: actor.id,
      email: actor.email,
      role: actor.role,
      eventType: "account_role_updated",
      outcome: "success",
      request,
      detail: `Updated ${targetId} to ${nextRole}${stationId ? ` @ ${stationId}` : ""}`,
    });
    return json(request, env, { id: targetId, role: nextRole, stationId, updatedAt: now });
  }

  const revokeUserTokensMatch = path.match(/^\/admin\/users\/([^/]+)\/revoke-tokens$/);
  if (request.method === "POST" && revokeUserTokensMatch) {
    const targetId = revokeUserTokensMatch[1];
    const { user: actor } = await currentUser(request, env);
    if (actor.role !== "admin") throw new HttpError(403, "Admin access required");
    const now = Date.now();
    const result = await env.DB.prepare(
      "UPDATE users SET token_version = COALESCE(token_version, 1) + 1, updated_at = ? WHERE id = ?",
    )
      .bind(now, targetId)
      .run();
    if (result.meta.changes !== 1) throw new HttpError(404, "User not found");
    const updated = await env.DB.prepare("SELECT email, token_version FROM users WHERE id = ?")
      .bind(targetId)
      .first<{ email: string; token_version: number }>();
    await logAudit(env, actor.id, "session_tokens_revoked", "user", targetId, {
      tokenVersion: updated?.token_version,
    });
    await logAccess(env, {
      userId: actor.id,
      email: actor.email,
      role: actor.role,
      eventType: "token_revoked",
      outcome: "blocked",
      request,
      detail: `Revoked session tokens for ${updated?.email ?? targetId} (v${updated?.token_version ?? 2})`,
    });
    return json(request, env, { id: targetId, tokenVersion: updated?.token_version ?? 2 });
  }

  if (request.method === "GET" && path === "/admin/tokens") {
    const { user } = await currentUser(request, env);
    if (user.role !== "admin") throw new HttpError(403, "Admin access required");
    const [txRows, inviteRows] = await Promise.all([
      env.DB.prepare(
        `SELECT t.id, t.station_id, t.admin_id, t.student_id, t.items_json, t.total_quantity,
          t.weight_kg, t.points, t.status, t.created_at, t.expires_at, t.confirmed_at,
          s.name AS station_name, u.name AS student_name, u.faculty AS student_faculty
         FROM transactions t
         JOIN stations s ON s.id = t.station_id
         LEFT JOIN users u ON u.id = t.student_id
         ORDER BY t.created_at DESC
         LIMIT 200`,
      ).all<TransactionViewRow>(),
      env.DB.prepare(
        `SELECT i.id, i.code_preview, i.label, i.station_id, s.name AS station_name,
          i.max_uses, i.used_count, i.expires_at, i.revoked_at, i.created_by, i.created_at
         FROM admin_invites i
         LEFT JOIN stations s ON s.id = i.station_id
         ORDER BY i.created_at DESC
         LIMIT 100`,
      ).all<AdminInviteRow>(),
    ]);
    return json(request, env, {
      qrTransactions: txRows.results.map((tx) => transactionView(tx)),
      invites: inviteRows.results.map((inv) => ({
        id: inv.id,
        code: inv.code_preview,
        label: inv.label,
        stationId: inv.station_id,
        stationName: inv.station_name ?? inv.station_id,
        maxUses: inv.max_uses,
        usedCount: inv.used_count,
        expiresAt: inv.expires_at,
        isRevoked: inv.revoked_at !== null,
        createdAt: inv.created_at,
      })),
    });
  }

  const revokeTokenMatch = path.match(/^\/admin\/tokens\/([^/]+)\/revoke$/);
  if (request.method === "POST" && revokeTokenMatch) {
    const txId = revokeTokenMatch[1];
    const { user: actor } = await currentUser(request, env);
    if (actor.role !== "admin") throw new HttpError(403, "Admin access required");
    const now = Date.now();
    const result = await env.DB.prepare(
      "UPDATE transactions SET status = 'cancelled', cancelled_at = ? WHERE id = ? AND status = 'pending'",
    )
      .bind(now, txId)
      .run();
    if (result.meta.changes !== 1) throw new HttpError(409, "Pending QR token not found");
    await logAudit(env, actor.id, "qr_token_revoked", "transaction", txId);
    await logAccess(env, {
      userId: actor.id,
      email: actor.email,
      role: actor.role,
      eventType: "qr_token_revoked",
      outcome: "blocked",
      request,
      detail: `Revoked pending QR token ${txId}`,
    });
    return json(request, env, { id: txId, cancelledAt: now });
  }

  if (request.method === "POST" && path === "/admin/tokens/expire-stale") {
    const { user: actor } = await currentUser(request, env);
    if (actor.role !== "admin") throw new HttpError(403, "Admin access required");
    const now = Date.now();
    const result = await env.DB.prepare(
      "UPDATE transactions SET status = 'expired' WHERE status = 'pending' AND expires_at <= ?",
    )
      .bind(now)
      .run();
    return json(request, env, { expiredCount: result.meta.changes ?? 0 });
  }

  if (request.method === "POST" && path === "/admin/invites") {
    const { user: actor } = await currentUser(request, env);
    if (actor.role !== "admin") throw new HttpError(403, "Admin access required");
    const input = await body(request);
    const label = requireString(input.label, "label");
    const stationId = requireString(input.stationId, "stationId");
    const station = await env.DB.prepare("SELECT id, name FROM stations WHERE id = ?")
      .bind(stationId)
      .first<{ id: string; name: string }>();
    if (!station) throw new HttpError(400, "Station not found");
    const maxUses = Math.max(1, Math.min(50, Number(input.maxUses ?? 3) || 3));
    const ttlHours = Math.max(1, Math.min(720, Number(input.ttlHours ?? 24) || 24));
    const now = Date.now();
    const expiresAt = now + ttlHours * 3_600_000;
    const rawCode = `ADM-${randomToken().slice(0, 4)}-${randomToken().slice(0, 4)}`;
    const id = randomId();
    await env.DB.prepare(
      `INSERT INTO admin_invites (id, code_hash, code_preview, label, station_id, max_uses, used_count, expires_at, created_by, created_at)
       VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?, ?)`,
    )
      .bind(id, await digest(rawCode), rawCode, label, stationId, maxUses, expiresAt, actor.id, now)
      .run();
    await logAudit(env, actor.id, "admin_invite_created", "admin_invite", id, { stationId, maxUses, ttlHours });
    await logAccess(env, {
      userId: actor.id,
      email: actor.email,
      role: actor.role,
      eventType: "invite_created",
      outcome: "success",
      request,
      detail: `Created invite ${rawCode} for ${station.name}`,
    });
    return json(
      request,
      env,
      {
        id,
        code: rawCode,
        label,
        stationId,
        stationName: station.name,
        maxUses,
        usedCount: 0,
        expiresAt,
        isRevoked: false,
        createdAt: now,
      },
      201,
    );
  }

  const revokeInviteMatch = path.match(/^\/admin\/invites\/([^/]+)\/revoke$/);
  if (request.method === "POST" && revokeInviteMatch) {
    const inviteId = revokeInviteMatch[1];
    const { user: actor } = await currentUser(request, env);
    if (actor.role !== "admin") throw new HttpError(403, "Admin access required");
    const now = Date.now();
    const result = await env.DB.prepare(
      "UPDATE admin_invites SET revoked_at = ? WHERE id = ? AND revoked_at IS NULL",
    )
      .bind(now, inviteId)
      .run();
    if (result.meta.changes !== 1) throw new HttpError(404, "Invite token not found or already revoked");
    await logAudit(env, actor.id, "admin_invite_revoked", "admin_invite", inviteId);
    await logAccess(env, {
      userId: actor.id,
      email: actor.email,
      role: actor.role,
      eventType: "invite_revoked",
      outcome: "blocked",
      request,
      detail: `Revoked invite token ${inviteId}`,
    });
    return json(request, env, { id: inviteId, revokedAt: now });
  }

  if (request.method === "GET" && path === "/admin/access-logs") {
    const { user } = await currentUser(request, env);
    if (user.role !== "admin") throw new HttpError(403, "Admin access required");
    const now = Date.now();
    const [logsResult, lockoutsResult] = await Promise.all([
      env.DB.prepare(
        `SELECT id, user_id, email, role, event_type, outcome, ip_address, user_agent, detail, created_at
         FROM access_logs
         ORDER BY created_at DESC
         LIMIT 200`,
      ).all<AccessLogRow>(),
      env.DB.prepare(
        `SELECT throttle_key, email_hint, failure_count, locked_until, updated_at
         FROM auth_throttles
         WHERE locked_until > ?
         ORDER BY locked_until DESC
         LIMIT 50`,
      )
        .bind(now)
        .all<AuthThrottleRow>(),
    ]);
    return json(request, env, {
      logs: logsResult.results.map((l) => ({
        id: l.id,
        userId: l.user_id,
        email: l.email,
        role: l.role,
        eventType: l.event_type,
        outcome: l.outcome,
        ipAddress: l.ip_address,
        deviceInfo: l.user_agent,
        detail: l.detail,
        createdAt: l.created_at,
      })),
      lockouts: lockoutsResult.results.map((t) => ({
        throttleKey: t.throttle_key ?? "",
        emailHint: t.email_hint ?? "Unknown email",
        failureCount: t.failure_count,
        lockedUntil: t.locked_until,
        updatedAt: t.updated_at ?? now,
      })),
    });
  }

  if (request.method === "POST" && path === "/admin/throttles/clear") {
    const { user: actor } = await currentUser(request, env);
    if (actor.role !== "admin") throw new HttpError(403, "Admin access required");
    const input = await body(request);
    const throttleKey = optionalString(input.throttleKey);
    if (throttleKey) {
      await env.DB.prepare("DELETE FROM auth_throttles WHERE throttle_key = ?")
        .bind(throttleKey)
        .run();
    } else {
      await env.DB.prepare("DELETE FROM auth_throttles WHERE locked_until > 0").run();
    }
    await logAccess(env, {
      userId: actor.id,
      email: actor.email,
      role: actor.role,
      eventType: "throttle_cleared",
      outcome: "success",
      request,
      detail: throttleKey ? `Unlocked auth throttle ${throttleKey.slice(0, 8)}` : "Cleared all auth lockouts",
    });
    return json(request, env, { cleared: true });
  }

  const toggleStationMatch = path.match(/^\/admin\/stations\/([^/]+)\/toggle$/);
  if (request.method === "POST" && toggleStationMatch) {
    const stationId = toggleStationMatch[1];
    const { user: actor } = await currentUser(request, env);
    if (actor.role !== "admin") throw new HttpError(403, "Admin access required");
    const station = await env.DB.prepare("SELECT id, name, is_active FROM stations WHERE id = ?")
      .bind(stationId)
      .first<{ id: string; name: string; is_active: number }>();
    if (!station) throw new HttpError(404, "Station not found");
    const nextActive = station.is_active === 1 ? 0 : 1;
    await env.DB.prepare("UPDATE stations SET is_active = ? WHERE id = ?")
      .bind(nextActive, stationId)
      .run();
    await logAudit(env, actor.id, "station_toggled", "station", stationId, {
      isActive: nextActive === 1,
    });
    await logAccess(env, {
      userId: actor.id,
      email: actor.email,
      role: actor.role,
      eventType: nextActive === 1 ? "station_resumed" : "station_paused",
      outcome: "success",
      request,
      detail: `${station.name} -> ${nextActive === 1 ? "active" : "paused"}`,
    });
    return json(request, env, { id: stationId, isActive: nextActive === 1 });
  }

  throw new HttpError(404, "Route not found");
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (request.method === "OPTIONS")
      return new Response(null, { status: 204, headers: headers(request, env) });
    try {
      return await handleApi(request, env);
    } catch (error) {
      const status = error instanceof HttpError ? error.status : 500;
      const message = error instanceof HttpError ? error.message : "Unexpected server error";
      const log = status >= 500 ? console.error : console.warn;
      log(
        JSON.stringify({
          message: "api request failed",
          status,
          path: new URL(request.url).pathname,
          error: error instanceof Error ? error.message : String(error),
        }),
      );
      return json(request, env, { error: message }, status);
    }
  },
} satisfies ExportedHandler<Env>;
