# NexGen E-Commerce Microservices Platform

NexGen is a cloud-based, scalable, and event-driven e-commerce microservices platform built using Spring Boot, React, MS SQL Server, Redis, Kafka, and Docker. It follows a modular architecture with multiple microservices communicating via Kafka and exposed through Kong API Gateway.

## 🔧 Technologies Used

- **Backend**: Spring Boot (Java 17+), Spring Security, Spring Data JPA, Spring Validation
- **Frontend**: ReactJS (Planned)
- **Database**: MS SQL Server
- **Cloud**: Microsoft Azure (Planned)
- **Cache**: Redis
- **Event Bus**: Apache Kafka (Confluent)
- **API Gateway**: Kong Gateway
- **DevOps**: Docker, Docker Compose
- **Monitoring**: Spring Boot Actuator, Prometheus
- **Documentation**: Swagger/OpenAPI

---

## 🧩 Microservices Overview

### 1. **User Service**
- **Authentication**: JWT-based login/logout with refresh token
- **Features**: Register, Login, Update Profile, Change Password, Get User by Username
- **Security**: Spring Security + Redis-backed JWT Token Blacklisting
- **Events**: Kafka events published for `REGISTERED`, `LOGIN`, `UPDATED_PROFILE`, `CHANGED_PASSWORD`
- **Caching**: Redis caching for fetching user data
- **Swagger**: Integrated with detailed documentation

### 2. **Inventory Service**
- **CRUD**: Create, Update, Delete, and Get inventory items
- **Pagination**: Supported for listing product items
- **Events**: Kafka events for `CREATED`, `UPDATED`, and `DELETED`
- **Caching**: Redis caching for get, update, and delete operations
- **Swagger**: Integrated with detailed documentation

### 3. **Product Service** 
- **CRUD**: Create, Update, Delete, and Get product items
- **Pagination**: Supported for listing inventory items
- **Events**: Kafka events for `CREATED`, `UPDATED`, and `OUT_OF_STOCK`
- **Caching**: Redis caching for get, update, and delete operations
- **Swagger**: Integrated with detailed documentation
- **Monitoring**: Monitoring metrics via Prometheus and actuator

### 4. **Cart Service** *(Planned)*
- **Features**: Add/Remove items from cart, Update quantity, Calculate total
- **Integration**: Communicates with Product and Inventory services

### 5. **Order Service** *(Loading)*
- **Features**: Place orders, Track orders, Payment integration
- **Events**: Kafka-driven order processing and stock syncing

### 6. **Notification Service** *(Planned)*
- **Features**: Send emails, SMS, or in-app notifications
- **Triggers**: Subscribes to Kafka topics for user and order events

---

## ⚙️ Architecture

```plaintext
                +-------------------------+
                |    Kong API Gateway     |
                +-----------+-------------+
                            |
        +-------------------+---------------------+
        |                   |                     |
+-------v-----+     +-------v-----+       +--------v-------+
|  User Svc   |     | Inventory   |       |  Future Svc... |
+-------------+     +-------------+       +----------------+
       |                   |
       |                   |
+------v-----+     +-------v-----+
|   Redis    |     |   Kafka     |
+------------+     +-------------+

Database: MS SQL Server (Per service)
```

---

## 🔐 Security

- JWT Token Authentication
- Spring Security with filters for token validation
- Redis token store for blacklist and refresh token tracking

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Docker & Docker Compose
- Kafka (Confluent) and Zookeeper
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
- [x] Kafka event publishing on all key user and inventory events
- [x] Redis caching for optimized performance
- [x] Swagger documentation for User and Inventory APIs
- [x] Full CRUD for Inventory with pagination
- [x] Swagger secured with JWT Bearer

---

## 🧪 Upcoming Work

- [ ] Implement Cart and Notification services
- [ ] Kafka and Redis integration for other services
- [ ] Role-based Authorization (RBAC)
- [ ] Deployment of application to Azure Cloud

---

## 🧼 Best Practices Followed

- ✅ DTO pattern used
- ✅ Exception-safe service and controller layers
- ✅ Redis Caching with Spring Cache Abstractions
- ✅ Event publishing decoupled via producer service
- ✅ Logs and error handling for observability
- ✅ Modular code structure for future scaling

---

## 📂 Project Structure

```
nexgen/
├── user-service/
├── inventory-service/
├── product-service/     
├── cart-service/        # (planned)
├── order-service/       # (loading)
├── notification-service/# (planned)
└── api-gateway/ (Kong)
```

---

## 👨‍💻 Author

**Vikas Dangi**  
Java Developer | Spring Boot | React | Microservices | Azure

📧 Connect with me on [LinkedIn](https://www.linkedin.com/in/vikasdangi/)
---
