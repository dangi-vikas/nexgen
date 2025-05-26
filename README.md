# NexGen E-Commerce Microservices Platform

NexGen is a cloud-based, scalable, and event-driven e-commerce microservices platform built using Spring Boot, React, MS SQL Server, Redis, Kafka, and Docker. It follows a Saga Choreography architecture with multiple microservices communicating via Kafka and exposed through Kong API Gateway.

## 🔧 Technologies Used

- **Backend**: Spring Boot (Java 17+), Spring Security, Spring Data JPA, Spring Validation
- **Frontend**: ReactJS (Planned)
- **Database**: MS SQL Server (per service)
- **Cloud**: Microsoft Azure (Planned)
- **Cache**: Redis
- **Event Bus**: Apache Kafka (Confluent) with **SASL_SSL Authentication**
- **API Gateway**: Kong Gateway
- **DevOps**: Docker, Docker Compose
- **Monitoring**: Spring Boot Actuator, Prometheus, Grafana
- **Observability**: Centralized logging (Loki, Promtail), structured logs
- **Resilience**: Spring Cloud Resilience4j (Retry, Circuit Breaker, Rate Limiter)
- **Documentation**: Swagger/OpenAPI
- **Orchestration Pattern**: Saga Pattern using Kafka Choreography

---

## 🧩 Microservices Overview

### 1. **User Service**
- **Authentication**: JWT-based login/logout with refresh token
- **Features**: Register, Login, Update Profile, Change Password, Get User by Username
- **Security**: Spring Security + Redis-backed JWT Token Blacklisting
- **Events**: Kafka events published for `REGISTERED`, `LOGIN`, `UPDATED_PROFILE`, `CHANGED_PASSWORD`
- **Caching**: Redis caching for fetching user data
- **Swagger**: Integrated with detailed documentation
- **Monitoring**: Monitoring metrics via Prometheus and Grafana

### 2. **Inventory Service**
- **CRUD**: Create, Update, Delete, and Get inventory items
- **Pagination**: Supported for listing product items
- **Events**: Kafka events for `CREATED`, `UPDATED`, and `OUT_OF_STOCK`
- **Caching**: Redis caching for get, update, and delete operations
- **Swagger**: Integrated with detailed documentation
- **Monitoring**: Monitoring metrics via Prometheus and Grafana

### 3. **Product Service** 
- **CRUD**: Create, Update, Delete, and Get product items
- **Pagination**: Supported for listing inventory items
- **Events**: Kafka events for `CREATED`, `UPDATED`, and `DELETED`
- **Caching**: Redis caching for get, update, and delete operations
- **Swagger**: Integrated with detailed documentation
- **Monitoring**: Monitoring metrics via Prometheus and Grafana

### 4. **Cart Service** 
- **Features**: Add/Remove items from cart, Update quantity, Calculate total
- **Integration**: Communicates with Product and Inventory services
- **Caching**: Redis caching for get, update, and delete operations
- **Swagger**: Integrated with detailed documentation
- **Saga Coordinator**: Cart to initiate Order Sagas
- **Monitoring**: Monitoring metrics via Prometheus and Grafana

### 5. **Order Service** 
- **Features**: Place orders, Track orders, Payment integration
- **Events**: Order placed → inventory → payment → confirmation 
- **Failure Handling**: Compensating transactions
- **Resilience**: Retry + Circuit Breaker for service dependencies
- **Swagger**: Integrated with detailed documentation
- **Monitoring**: Monitoring metrics via Prometheus and Grafana


### 6. **Notification Service** *(Planned)*
- **Features**: Send emails, SMS, or in-app notifications
- **Triggers**: Subscribes to Kafka topics for user and order events
- **Channels**: Email, SMS, In-app
- **Retry**: On failure using Resilience4j retry
- **Swagger**: Integrated with detailed documentation
- **Monitoring**: Monitoring metrics via Prometheus and Grafana

---

## ⚙️ Architecture

```
               +--------------------------+
               |      Kong Gateway        |
               +------------+-------------+
                            |
        +-------------------+---------------------+
        |                   |                     |
+-------v-----+     +-------v-----+       +--------v-------+
|  User Svc   |     | Inventory   |       |  Product Svc   |
+-------------+     +-------------+       +----------------+
       |                   |                     |
       |                   |                     |
+------v-----+     +-------v-----+       +--------v-------+
|   Redis    |     |   Kafka     |<----->|  Order Svc     |
+------------+     +-------------+       +----------------+
                                ↑
                                |
                       +--------v--------+
                       | Notification Svc|
                       +-----------------+

Monitoring: Prometheus + Grafana
Logging: Centralized via structured logs
Database: MS SQL Server (Per service)
```

---

## 🔐 Security

* JWT-based authentication with Spring Security
* Redis for token blacklist & refresh token tracking
* Secure Kafka communication with SSL and SASL_SSL
* Swagger secured with JWT Bearer Token
* Endpoints secured via Kong JWT plugin and SSL

---

## 📊 Monitoring & Observability

* **Actuator Endpoints** for all services
* **Prometheus** for scraping metrics
* **Grafana Dashboards** for real-time visualization
* **Centralized Structured Logging** Loki, Promtail
* **Custom metrics**: Kafka retry counts, error rates, cache misses, API resonse time, API endpoind hits

---

## 🛡 Resilience

* **Resilience4j** for Retry, Circuit Breaker, and Rate Limiting
* **Kafka Retry with Redis-backed cache** to persist failed events
* **Circuit breakers** protect services from downstream failures
* **Timeout policies** configured for all service-to-service calls

---

## 🔀 Event-Driven Saga Pattern (Choreography)

* Order Service publishes `ORDER_PLACED` event
* Inventory Service reduces stock, publishes `INVENTORY_UPDATED`
* Payment Service processes payment
* Notification Service sends confirmation
* **Rollback strategy** using compensating transactions
* **Kafka Topics** act as coordination channels for the saga


## 🚀 Getting Started

### Prerequisites
- Java 17+
- Docker & Docker Compose
- Kafka (Confluent Platform with SSL and SASL_SSL enabled) and Zookeeper
- Redis
- MS SQL Server

### Run the stack

```bash
docker-compose up -d
```
Run each service locally using IntelliJ.

---

## ✅ Completed Features

- [x] JWT Auth with refresh token
- [x] Kafka event publishing on all the key events
- [x] Redis caching for optimized performance
- [x] Swagger documentation for all the microservices
- [x] Full CRUD for all the services with pagination
- [x] Swagger secured with JWT Bearer
- [x] Created API Gateway and secure connections using Kong
      
---

## 🧪 Upcoming Work

- [ ] Implement Notification services
- [ ] Creating Profiles
- [ ] Role-based Authorization (RBAC)
- [ ] Creating front end for the application
- [ ] Deployment of application to Azure Cloud

---

## 🧼 Best Practices Followed

- ✅ DTO pattern used
- ✅ Exception-safe service and controller layers
- ✅ Redis Caching with Spring Cache Abstractions
- ✅ Event publishing decoupled 
- ✅ Logs and error handling for observability
- ✅ Modular code structure for future scaling
- ✅ Secured Microservices and API endpoints
- ✅ Resillience implemented using Resillience4j, Kong and Redis 

---

## 📂 Project Structure

```
nexgen/
├── user-service/
├── inventory-service/
├── product-service/     
├── cart-service/        
├── order-service/     
├── notification-service/# (planned)
└── api-gateway/ (Kong)
```

---

## 👨‍💻 Author

**Vikas Dangi**  
Java Developer | Spring Boot | React | Microservices | Azure

📧 Connect with me on [LinkedIn](https://www.linkedin.com/in/vikasdangi/)
---
