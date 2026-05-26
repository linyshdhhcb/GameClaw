# Revenue / Payment Query Templates

ClickHouse SQL templates for revenue and payment analysis. All templates use the `payments` and `users` tables.

---

## 1. 近7天付费总额

**描述：** 查询最近7天的每日付费总金额，按天分组展示收入趋势。

```sql
SELECT
    toDate(pay_time) AS pay_date,
    sum(amount) AS total_revenue,
    countDistinct(user_id) AS paying_users,
    count() AS payment_count
FROM payments
WHERE pay_date >= today() - 7
  AND pay_date < today()
  AND status = 'success'
GROUP BY pay_date
ORDER BY pay_date ASC
```

**参数说明：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `pay_time` | 支付时间戳字段 | - |
| `amount` | 支付金额字段 | - |
| `user_id` | 用户唯一标识字段 | - |
| `status` | 支付状态字段，`success` 表示成功 | success |
| `7` | 回溯天数 | 7 |

---

## 2. ARPU (Average Revenue Per User)

**描述：** 计算指定日期的 ARPU（每用户平均收入），即当日总收入除以当日活跃用户数。

```sql
SELECT
    rev.pay_date,
    rev.total_revenue,
    active.active_users,
    round(rev.total_revenue / active.active_users, 2) AS arpu
FROM (
    SELECT
        toDate(pay_time) AS pay_date,
        sum(amount) AS total_revenue
    FROM payments
    WHERE toDate(pay_time) = today() - 1
      AND status = 'success'
    GROUP BY pay_date
) AS rev
INNER JOIN (
    SELECT
        toDate(event_time) AS event_date,
        uniqExact(user_id) AS active_users
    FROM events
    WHERE toDate(event_time) = today() - 1
    GROUP BY event_date
) AS active ON rev.pay_date = active.event_date
```

**参数说明：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `total_revenue` | 当日付费总额 | - |
| `active_users` | 当日活跃用户数（DAU） | - |
| `arpu` | ARPU = total_revenue / active_users | - |
| `today() - 1` | 目标日期 | today()-1 |

> **注意：** 如果当日无付费记录，`rev` 子查询为空会导致结果缺失。可用 `COALESCE(sum(amount), 0)` 兜底。

---

## 3. ARPPU (Average Revenue Per Paying User)

**描述：** 计算指定日期的 ARPPU（每付费用户平均收入），即当日总收入除以当日付费用户数。

```sql
SELECT
    toDate(pay_time) AS pay_date,
    sum(amount) AS total_revenue,
    countDistinct(user_id) AS paying_users,
    round(sum(amount) / countDistinct(user_id), 2) AS arppu
FROM payments
WHERE toDate(pay_time) = today() - 1
  AND status = 'success'
GROUP BY pay_date
```

**参数说明：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `total_revenue` | 当日付费总额 | - |
| `paying_users` | 当日付费用户数 | - |
| `arppu` | ARPPU = total_revenue / paying_users | - |
| `today() - 1` | 目标日期 | today()-1 |

> **对比：** ARPU 面向全部活跃用户，ARPPU 仅面向付费用户。ARPPU 通常远高于 ARPU。

---

## 4. 付费率

**描述：** 计算指定日期的付费率，即当日付费用户数占当日活跃用户数的比例。

```sql
SELECT
    pay_date,
    paying_users,
    active_users,
    round(paying_users / active_users * 100, 2) AS pay_rate_pct
FROM (
    SELECT
        toDate(pay_time) AS pay_date,
        countDistinct(user_id) AS paying_users
    FROM payments
    WHERE toDate(pay_time) = today() - 1
      AND status = 'success'
    GROUP BY pay_date
) AS p
INNER JOIN (
    SELECT
        toDate(event_time) AS event_date,
        uniqExact(user_id) AS active_users
    FROM events
    WHERE toDate(event_time) = today() - 1
    GROUP BY event_date
) AS a ON p.pay_date = a.event_date
```

**参数说明：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `paying_users` | 当日付费用户数 | - |
| `active_users` | 当日活跃用户数（DAU） | - |
| `pay_rate_pct` | 付费率百分比 = paying_users / active_users * 100 | - |
| `today() - 1` | 目标日期 | today()-1 |

---

## 5. 分档位付费分布

**描述：** 按付费档位（sku_tier）分组统计付费用户数和付费金额，用于分析各档位的贡献占比。

```sql
SELECT
    sku_tier,
    countDistinct(user_id) AS paying_users,
    sum(amount) AS total_revenue,
    round(sum(amount) / sum(sum(amount)) OVER () * 100, 2) AS revenue_pct
FROM payments
WHERE toDate(pay_time) >= today() - 7
  AND toDate(pay_time) < today()
  AND status = 'success'
GROUP BY sku_tier
ORDER BY total_revenue DESC
```

**参数说明：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `sku_tier` | 付费档位字段（如 small/medium/large 或具体金额档位） | - |
| `paying_users` | 该档位付费用户数 | - |
| `total_revenue` | 该档位付费总额 | - |
| `revenue_pct` | 该档位收入占比百分比 | - |
| `7` | 回溯天数 | 7 |

> **扩展：** 如需按具体 SKU 分析，将 `sku_tier` 替换为 `sku_id` 或 `product_name` 字段即可。

---

## 6. LTV (Life Time Value)

**描述：** 计算不同注册日期cohort的用户在注册后N天内的累计人均付费（LTV），支持多天对比。

```sql
SELECT
    cohort_date,
    new_users,
    day_n,
    cumulative_revenue,
    round(cumulative_revenue / new_users, 2) AS ltv
FROM (
    SELECT
        toDate(u.register_date) AS cohort_date,
        countDistinct(u.user_id) AS new_users
    FROM users AS u
    WHERE toDate(u.register_date) >= today() - 30
      AND toDate(u.register_date) < today()
    GROUP BY cohort_date
) AS cohort
INNER JOIN (
    SELECT
        toDate(u.register_date) AS cohort_date,
        dateDiff('day', toDate(u.register_date), toDate(p.pay_time)) AS day_n,
        sum(p.amount) AS cumulative_revenue
    FROM users AS u
    INNER JOIN payments AS p ON u.user_id = p.user_id
    WHERE p.status = 'success'
      AND toDate(u.register_date) >= today() - 30
      AND toDate(u.register_date) < today()
    GROUP BY cohort_date, day_n
) AS rev ON cohort.cohort_date = rev.cohort_date
ORDER BY cohort_date ASC, day_n ASC
```

**参数说明：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `register_date` | 用户注册日期字段 | - |
| `day_n` | 注册后第N天（0=当天，1=次日，以此类推） | - |
| `cumulative_revenue` | 该cohort在第N天的累计付费总额 | - |
| `new_users` | 该cohort的新用户数 | - |
| `ltv` | LTV = cumulative_revenue / new_users | - |
| `30` | cohort回溯天数 | 30 |

> **注意：** 此查询返回的是每天的分段LTV。如需计算到第N天的总LTV，需在外层对 `day_n <= N` 的行做 `cumulative_revenue` 累加：
> ```sql
> SELECT cohort_date, new_users, sum(cumulative_revenue) AS total_revenue, round(sum(cumulative_revenue) / new_users, 2) AS ltv_n FROM (...) WHERE day_n <= 7 GROUP BY cohort_date, new_users
> ```
> 这样即可得到 LTV7（注册后7天内的LTV）等指标。
