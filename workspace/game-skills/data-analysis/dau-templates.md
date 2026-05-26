# DAU (Daily Active Users) Query Templates

ClickHouse SQL templates for DAU analysis. All templates use the `events` table as the primary data source.

---

## 1. 近7天DAU趋势

**描述：** 查询最近7天每日活跃用户数，按天分组展示趋势变化。

```sql
SELECT
    toDate(event_time) AS event_date,
    uniqExact(user_id) AS dau
FROM events
WHERE event_date >= today() - 7
  AND event_date < today()
GROUP BY event_date
ORDER BY event_date ASC
```

**参数说明：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `event_time` | 事件时间戳字段 | - |
| `user_id` | 用户唯一标识字段 | - |
| `7` | 回溯天数，可改为任意正整数 | 7 |

---

## 2. 近30天DAU趋势

**描述：** 查询最近30天每日活跃用户数，适用于月度活跃趋势分析。

```sql
SELECT
    toDate(event_time) AS event_date,
    uniqExact(user_id) AS dau
FROM events
WHERE event_date >= today() - 30
  AND event_date < today()
GROUP BY event_date
ORDER BY event_date ASC
```

**参数说明：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `event_time` | 事件时间戳字段 | - |
| `user_id` | 用户唯一标识字段 | - |
| `30` | 回溯天数，可改为任意正整数 | 30 |

> **性能提示：** 30天数据量较大时，可用 `uniq(user_id)` 替代 `uniqExact(user_id)` 获得近似值，速度更快。

---

## 3. DAU环比/同比

**描述：** 计算当日DAU与昨日DAU（环比）及上周同日DAU（同比），展示增长/下降百分比。

```sql
SELECT
    today_dau.event_date,
    today_dau.dau AS dau,
    yesterday_dau.dau AS dau_prev_day,
    round((today_dau.dau - yesterday_dau.dau) / yesterday_dau.dau * 100, 2) AS mom_pct,
    last_week_dau.dau AS dau_last_week,
    round((today_dau.dau - last_week_dau.dau) / last_week_dau.dau * 100, 2) AS yoy_pct
FROM (
    SELECT toDate(event_time) AS event_date, uniqExact(user_id) AS dau
    FROM events
    WHERE event_date = today() - 1
    GROUP BY event_date
) AS today_dau
LEFT JOIN (
    SELECT toDate(event_time) AS event_date, uniqExact(user_id) AS dau
    FROM events
    WHERE event_date = today() - 2
    GROUP BY event_date
) AS yesterday_dau ON today_dau.event_date = yesterday_dau.event_date + INTERVAL 1 DAY
LEFT JOIN (
    SELECT toDate(event_time) AS event_date, uniqExact(user_id) AS dau
    FROM events
    WHERE event_date = today() - 8
    GROUP BY event_date
) AS last_week_dau ON today_dau.event_date = last_week_dau.event_date + INTERVAL 7 DAY
```

**参数说明：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `today() - 1` | 目标日期，调整偏移量可查任意日期 | today()-1 |
| `INTERVAL 1 DAY` | 环比间隔 | 1 DAY |
| `INTERVAL 7 DAY` | 同比间隔 | 7 DAY |
| `mom_pct` | 环比变化百分比（Day-over-Day） | - |
| `yoy_pct` | 同比变化百分比（Week-over-Week） | - |

---

## 4. 分渠道DAU

**描述：** 按渠道（channel）分组统计当日DAU，用于分析各渠道用户活跃情况。

```sql
SELECT
    channel,
    uniqExact(user_id) AS dau
FROM events
WHERE toDate(event_time) = today() - 1
GROUP BY channel
ORDER BY dau DESC
```

**参数说明：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `channel` | 渠道字段名，存储用户来源渠道 | - |
| `user_id` | 用户唯一标识字段 | - |
| `today() - 1` | 目标日期 | today()-1 |

> **扩展：** 如需多天趋势，可在 `GROUP BY` 中加入 `toDate(event_time)` 并调整 `WHERE` 条件。

---

## 5. 分地区DAU

**描述：** 按地区（region）分组统计当日DAU，用于分析各地区用户活跃分布。

```sql
SELECT
    region,
    uniqExact(user_id) AS dau
FROM events
WHERE toDate(event_time) = today() - 1
GROUP BY region
ORDER BY dau DESC
```

**参数说明：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `region` | 地区字段名，存储用户所在地区 | - |
| `user_id` | 用户唯一标识字段 | - |
| `today() - 1` | 目标日期 | today()-1 |

> **扩展：** 可将 `region` 替换为 `country`、`province` 等字段实现不同粒度的地区分析。也可组合 `channel` 和 `region` 做交叉分析：
> ```sql
> GROUP BY channel, region
> ```
