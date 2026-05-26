# Retention Query Templates

ClickHouse SQL templates for user retention analysis. All templates use the `events` and `users` tables.

---

## 1. 次日留存

**描述：** 计算指定日期的新用户在次日是否回访，得出次日留存率。

```sql
SELECT
    cohort_date,
    countDistinct(user_id) AS new_users,
    countDistinct(retained.user_id) AS retained_users,
    round(retained_users / new_users * 100, 2) AS retention_rate
FROM (
    SELECT
        toDate(first_event_time) AS cohort_date,
        user_id
    FROM (
        SELECT
            user_id,
            min(event_time) AS first_event_time
        FROM events
        GROUP BY user_id
    )
    WHERE cohort_date = today() - 1
) AS cohort
LEFT JOIN (
    SELECT DISTINCT user_id
    FROM events
    WHERE toDate(event_time) = today()
) AS retained ON cohort.user_id = retained.user_id
GROUP BY cohort_date
```

**参数说明：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `today() - 1` | 注册日期（cohort日期） | today()-1 |
| `today()` | 留存观测日期（注册日期+1天） | today() |
| `first_event_time` | 用户首次事件时间，用于确定cohort | - |

> **简化写法：** 如果 `users` 表有 `register_date` 字段，可直接用 `users` 表替代子查询：
> ```sql
> FROM users WHERE toDate(register_date) = today() - 1
> ```

---

## 2. 7日留存

**描述：** 计算指定日期的新用户在第7天是否回访，得出7日留存率。

```sql
SELECT
    cohort_date,
    countDistinct(user_id) AS new_users,
    countDistinct(retained.user_id) AS retained_users,
    round(retained_users / new_users * 100, 2) AS retention_rate
FROM (
    SELECT
        toDate(first_event_time) AS cohort_date,
        user_id
    FROM (
        SELECT
            user_id,
            min(event_time) AS first_event_time
        FROM events
        GROUP BY user_id
    )
    WHERE cohort_date = today() - 7
) AS cohort
LEFT JOIN (
    SELECT DISTINCT user_id
    FROM events
    WHERE toDate(event_time) = today()
) AS retained ON cohort.user_id = retained.user_id
GROUP BY cohort_date
```

**参数说明：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `today() - 7` | 注册日期（cohort日期） | today()-7 |
| `7` | 留存天数偏移量 | 7 |
| `retention_rate` | 7日留存率百分比 | - |

---

## 3. 30日留存

**描述：** 计算指定日期的新用户在第30天是否回访，得出30日留存率。

```sql
SELECT
    cohort_date,
    countDistinct(user_id) AS new_users,
    countDistinct(retained.user_id) AS retained_users,
    round(retained_users / new_users * 100, 2) AS retention_rate
FROM (
    SELECT
        toDate(first_event_time) AS cohort_date,
        user_id
    FROM (
        SELECT
            user_id,
            min(event_time) AS first_event_time
        FROM events
        GROUP BY user_id
    )
    WHERE cohort_date = today() - 30
) AS cohort
LEFT JOIN (
    SELECT DISTINCT user_id
    FROM events
    WHERE toDate(event_time) = today()
) AS retained ON cohort.user_id = retained.user_id
GROUP BY cohort_date
```

**参数说明：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `today() - 30` | 注册日期（cohort日期） | today()-30 |
| `30` | 留存天数偏移量 | 30 |
| `retention_rate` | 30日留存率百分比 | - |

---

## 4. 分渠道留存

**描述：** 按渠道分组计算次日留存率，用于评估不同渠道的用户质量。

```sql
SELECT
    cohort.channel,
    countDistinct(cohort.user_id) AS new_users,
    countDistinct(retained.user_id) AS retained_users,
    round(retained_users / new_users * 100, 2) AS retention_rate
FROM (
    SELECT
        u.user_id,
        u.channel,
        toDate(u.register_date) AS cohort_date
    FROM users AS u
    WHERE toDate(u.register_date) = today() - 1
) AS cohort
LEFT JOIN (
    SELECT DISTINCT user_id
    FROM events
    WHERE toDate(event_time) = today()
) AS retained ON cohort.user_id = retained.user_id
GROUP BY cohort.channel
ORDER BY retention_rate DESC
```

**参数说明：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `channel` | 渠道字段，位于 `users` 表 | - |
| `register_date` | 用户注册日期字段 | - |
| `today() - 1` | cohort日期 | today()-1 |
| `today()` | 留存观测日期 | today() |

> **扩展：** 如需计算7日/30日分渠道留存，调整日期偏移即可。也可组合 `channel` 和 `region` 做更细粒度的交叉分析。

---

## 5. 留存趋势

**描述：** 查询最近N天每日新用户的次日留存率趋势，用于监控留存健康度变化。

```sql
SELECT
    cohort_date,
    new_users,
    retained_users,
    round(retained_users / new_users * 100, 2) AS retention_rate
FROM (
    SELECT
        toDate(register_date) AS cohort_date,
        countDistinct(user_id) AS new_users
    FROM users
    WHERE toDate(register_date) >= today() - 14
      AND toDate(register_date) < today()
    GROUP BY cohort_date
) AS cohort
LEFT JOIN (
    SELECT
        toDate(u.register_date) AS cohort_date,
        countDistinct(e.user_id) AS retained_users
    FROM users AS u
    INNER JOIN events AS e ON u.user_id = e.user_id
    WHERE toDate(e.event_time) = toDate(u.register_date) + INTERVAL 1 DAY
      AND toDate(u.register_date) >= today() - 14
      AND toDate(u.register_date) < today()
    GROUP BY cohort_date
) AS retained ON cohort.cohort_date = retained.cohort_date
ORDER BY cohort_date ASC
```

**参数说明：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `today() - 14` | 趋势起始日期 | today()-14 |
| `today()` | 趋势结束日期（不含） | today() |
| `INTERVAL 1 DAY` | 留存天数间隔 | 1 DAY |
| `register_date` | 用户注册日期字段 | - |

> **扩展：** 将 `INTERVAL 1 DAY` 改为 `INTERVAL 7 DAY` 可查看7日留存趋势。注意调整起始日期确保有足够数据。
