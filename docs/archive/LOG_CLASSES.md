# Log Classes and Field Variability

The log generator now creates **6 different classes** of logs, each with unique field combinations. This simulates a real system with multiple components logging different types of events.

---

## Log Classes

### 1. **Service Logs** (`class=service`)
**Frequency:** 25% of logs  
**Purpose:** Microservice operation tracking

**Fields:**
- `class`: service
- `level`: DEBUG/INFO/WARN/ERROR
- `service`: auth-service, payment-service, notification-service, analytics-service, reporting-service
- `operation`: processOrder, validateUser, fetchData, etc.
- `duration`: milliseconds
- `requestId`: unique identifier
- `status`: ok/slow (based on duration > 400ms)

**Example:**
```
2025-11-14T16:39:47.930646Z class=service level=INFO service=payment-service operation=calculatePrice duration=204ms requestId=15604 status=ok
```

**Sample Queries:**
```
class:service AND service:payment-service
class:service AND status:slow | stats count by service
class:service | stats avg(duration) by service
```

---

### 2. **Database Logs** (`class=database`)
**Frequency:** 20% of logs  
**Purpose:** Database query tracking

**Fields:**
- `class`: database
- `level`: DEBUG/INFO/WARN/ERROR
- `database`: postgres, mysql, mongodb, redis, elasticsearch
- `queryType`: SELECT/UPDATE
- `table`: table_0 through table_9
- `queryTime`: milliseconds
- `rowsAffected`: number of rows
- `pool`: available/exhausted (based on queryTime > 800ms)

**Example:**
```
2025-11-14T16:39:50.933834Z class=database level=INFO database=elasticsearch queryType=UPDATE table=table_0 queryTime=125ms rowsAffected=45 pool=available
```

**Sample Queries:**
```
class:database AND pool:exhausted
class:database | stats max(queryTime) by database
class:database AND queryType:UPDATE | stats count by table
```

---

### 3. **Cache Logs** (`class=cache`)
**Frequency:** 15% of logs  
**Purpose:** Cache operation tracking

**Fields:**
- `class`: cache
- `level`: DEBUG (cache logs are typically debug level)
- `operation`: get, set, delete, expire, flush
- `key`: key:0 through key:9999
- `result`: hit/miss (80% hit rate)
- `ttl`: time-to-live in seconds
- `hitRate`: cache hit rate percentage (0.75-0.95)

**Example:**
```
2025-11-14T16:39:54.933852Z class=cache level=DEBUG operation=flush key=key:2129 result=hit ttl=650 hitRate=0.89
```

**Sample Queries:**
```
class:cache AND result:miss
class:cache | stats count by operation
class:cache | stats avg(hitRate)
```

---

### 4. **Auth Logs** (`class=auth`)
**Frequency:** 15% of logs  
**Purpose:** Authentication and session tracking

**Fields (Login):**
- `class`: auth
- `level`: INFO/WARN (WARN on failure)
- `action`: login
- `user`: username
- `ip`: IP address
- `success`: true/false (90% success rate)
- `sessionId`: session identifier
- `mfa`: enabled/disabled

**Fields (Logout):**
- `class`: auth
- `level`: DEBUG/INFO/WARN/ERROR
- `action`: logout
- `user`: username
- `sessionId`: session identifier
- `sessionDuration`: duration in seconds

**Examples:**
```
2025-11-14T16:39:56.931147Z class=auth level=INFO action=login user=user123 ip=192.168.77.78 success=true sessionId=sess_96607 mfa=enabled

2025-11-14T16:40:12.931820Z class=auth level=INFO action=logout user=john.doe sessionId=sess_50672 sessionDuration=3456s
```

**Sample Queries:**
```
class:auth AND action:login AND success:false
class:auth AND mfa:disabled
class:auth AND action:logout | stats avg(sessionDuration) by user
```

---

### 5. **Payment Logs** (`class=payment`)
**Frequency:** 15% of logs  
**Purpose:** Payment transaction tracking

**Fields:**
- `class`: payment
- `level`: INFO/ERROR (ERROR on failed payments)
- `transactionId`: unique transaction identifier
- `amount`: transaction amount (10.00 - 1000.00)
- `currency`: USD, EUR, GBP
- `gateway`: stripe, paypal
- `status`: success/failed (95% success rate)
- `processingTime`: milliseconds

**Example:**
```
2025-11-14T16:39:45.932883Z class=payment level=INFO transactionId=txn_424420 amount=417.13 currency=USD gateway=stripe status=success processingTime=1376ms
```

**Sample Queries:**
```
class:payment AND status:failed
class:payment | stats sum(amount) by gateway
class:payment AND currency:EUR | stats count by status
class:payment | stats avg(processingTime) by gateway
```

---

### 6. **Business Logs** (`class=business`)
**Frequency:** 10% of logs  
**Purpose:** General business logic tracking

**Fields:**
- `class`: business
- `level`: DEBUG/INFO/WARN/ERROR
- `operation`: processOrder, validateUser, fetchData, etc.
- `user`: username
- `duration`: milliseconds
- `requestId`: unique identifier
- `status`: ok/slow (based on duration > 400ms)

**Example:**
```
2025-11-14T16:39:43.935232Z class=business level=INFO operation=processOrder user=john.doe duration=202ms requestId=64149 status=ok
```

**Sample Queries:**
```
class:business AND status:slow
class:business | stats count by operation, user
class:business | stats avg(duration) by operation
```

---

## Key Differences Between Classes

### Field Uniqueness
Each class has **unique fields** that only appear in that class:

| Class | Unique Fields |
|-------|---------------|
| service | `service` |
| database | `database`, `queryType`, `table`, `rowsAffected`, `pool` |
| cache | `key`, `result`, `hitRate`, `operation` (get/set/etc) |
| auth | `action`, `ip`, `success`, `mfa`, `sessionDuration` |
| payment | `transactionId`, `amount`, `currency`, `gateway`, `processingTime` |
| business | (no unique fields, similar to service but without `service` field) |

### Common Fields
Some fields appear across multiple classes:
- `level`: All classes
- `duration`: service, business
- `status`: service, business, payment (but different meanings)
- `user`: auth, business
- `requestId`: service, business

---

## Advanced Query Examples

### Cross-Class Aggregations

**Count logs by class:**
```
* | stats count by class
```

**Average duration across all classes that have it:**
```
duration:[* TO *] | stats avg(duration) by class
```

**Errors by class:**
```
level:ERROR | stats count by class
```

### Class-Specific Analytics

**Payment failure analysis:**
```
class:payment AND status:failed | stats count by gateway, currency
```

**Slow database queries by database type:**
```
class:database AND pool:exhausted | stats max(queryTime), count by database
```

**Cache performance:**
```
class:cache | stats avg(hitRate) by operation
```

**Authentication patterns:**
```
class:auth AND action:login | stats count by user, success
```

**Service performance comparison:**
```
class:service | stats avg(duration), count by service | sort avg(duration) desc
```

### Time-Based Analysis

**Payment transactions over time:**
```
class:payment | timechart span=1m sum(amount), count
```

**Database load timeline:**
```
class:database | timechart span=30s count by database
```

**Failed logins over time:**
```
class:auth AND success:false | timechart span=5m count
```

---

## Filtering by Class

The `class` field allows you to focus on specific system components:

**Only payment processing:**
```
class:payment
```

**Everything except cache (reduce noise):**
```
-class:cache
```

**Authentication and payments:**
```
class:(auth OR payment)
```

**Service and business logic only:**
```
class:(service OR business)
```

---

## Field Discovery

Since different classes have different fields, you can explore what's available:

**Find all unique classes:**
```
* | stats count by class
```

**See what fields exist in payment logs:**
```
class:payment
```
(Look at the facets panel to see all fields)

**Find logs with specific fields:**
```
transactionId:[* TO *]  # Has transactionId field
amount:[* TO *]         # Has amount field
```

---

## Benefits of Diverse Log Classes

1. **Realistic Simulation**: Mimics real systems with multiple components
2. **Field Variability**: Different classes have different fields, requiring flexible parsing
3. **Use Case Testing**: Can test filtering, aggregation, and search across heterogeneous logs
4. **Performance Analysis**: Each class represents a different subsystem to monitor
5. **Query Complexity**: Supports advanced queries that filter by component type

---

## Next Steps

1. Try filtering by `class` to see different log types
2. Use aggregations to compare metrics across classes
3. Build dashboards for specific components (e.g., payment monitoring)
4. Test parser configurations with different log formats
5. Create alerts for specific conditions (e.g., `class:payment AND status:failed`)
