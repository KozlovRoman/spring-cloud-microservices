<p align="center">
  <img src="https://seeklogo.com/images/S/spring-logo-9A2BC78AAF-seeklogo.com.png" width="120">
</p>

# Bank Microservice
### Microservice Architecture

Java RESTful API for Deposit creation regarding some Account and its Bill.

### Technologies
- Spring Boot
- Spring Cloud
- PostrgreSQL
- RabbitMQ
- Docker

## How does it work?
### Business Logic Architecture

Firstly, I will consider only the services that execute business logic and the relationships between them.

 <p align="center">
  <img src="https://i.ibb.co/2y8YdD8/Business-Logic-Architecture.png" width="600">
</p>

In this project, the business logic diagram includes Account service, Bill service, Deposit service and its databases.

Deposit service does not interact with Bill database or Account database in any way, but it needs all this information. Deposit service receives this information with the help of HTTP requests, according to the REST protocol, Deposit service will send a request to Bill service in order to receive some bill. And it will also send a request to Account service using REST protocol in order to also get some account.

Thus, Deposit service must be able to send HTTP requests.

Notification service will be responsible for notifications. It connects to Deposit service using RabbitMQ message queue.

Some request comes to Deposit service in order to make some deposit. Deposit service makes a request to Bill service, to Account service and successfully makes a deposit.

Next, from Deposit service, we are going to put the message into RabbitMQ queue. RabbitMQ will send this message to Notification service, and in Notification service we will receive the message that a deposit has been made to some email.

There is a service for sending messages below. This service is from Gmail.

### Architecture SpringCloud

Secondly, consider the application schema including infrastructure services.

<p align="center">
  <img src="https://i.ibb.co/bLJxK3L/Architecture-Spring-Cloud.png" width="800">
</p>

When the application starts, Account service will call Config service (this is an infrastructure service) in order to return the application.yml for Account service.

The same will happen with Deposit service. Config service will give its application.yml.

It's the same with Bill and Notification services.

All configuration will be in Config service. At the start, all applications will contact Config service.

Discovery service is designed for registering of all applications (our services) and it always knows the addresses of all applications and the addresses of its instances.

Eureka from Netflix acts as Discovery service.

All services contact Discovery service and register there.

Discovery service will know that we have such and such a number and such and such addresses.

The same will happen with Gateway service, it will also contact Config service to get its .yml (or property) configuration. And it will also register with Discovery service. It will say that "I am Gateway service and I have some instances, I am at some address."

This is how our services will interact with infrastructure services.

But we also have a load balancer (which is difficult to display in this diagram). It will participate in balancing REST requests.

If we have several Account services (several instances), then before Gateway service sends a request to instance 1 or 2, the load balancer will understand which instance is less loaded and to that one it will send the request that came to Gateway.

It is the same with Deposit service.

RabbitMQ as a message broker will be very handy for sending messages / notifications asynchronously.

### Entities Architecture

Next, I present the main entities that will be used and its relationships with each other.

<p align="center">
  <img src="https://i.ibb.co/cQ4CBfj/Entities-Architecture.png" width="800">
</p>

In Account service table, there will be a primary key - this is "accountId", there will also be other lines inside the table (see the picture above). 
The additional table will also be created to link "account" and "bill". Here I use the scheme generation with the help of JPA (not very often used). 
"billId" is in another database - Bill service DB, and this "billId" is the primary key to Bill table.

There is also Deposit service DB. Primary key is "depositId". In this table, "bill_id" is a foreign key to another database (i.e. to which account id this deposit was made).
Also in Bill service DB table there is one more foreign key between the databases - this is "account_id" (a field in the Bill entity), which is the primary key in Account 
table in Account service DB database.

### Functional services

This project Bank System was decomposed into 4 core microservices. All of them are independently deployable applications, organized around certain business domains.

#### Account service

Contains general user input logic and validation: incomes/expenses items, savings and account settings.

| HTTP METHOD | PATH | USAGE |
| -----------| ------ | ------ |
| GET | /{accountId} | Account getting by id | 
| POST | / | Account creation |
| PUT | /{accountId} | Account updation |
| DELETE | /{accountId} | Account deletion |

### Sample JSON for Account Service
##### Account creation : 
```sh
{
    "name": "Baxter",
    "email": "baxter@cat.xyz",
    "phone": "+1234567",
    "bills": [
    1
    ]
}
```
#### Bill Service

Creates a bill for the assigned account.

| HTTP METHOD | PATH | USAGE |
| -----------| ------ | ------ |
| GET | /{billId} | Bill getting by id | 
| POST | / | Bill creation |
| PUT | /{billId} | Bill updation |
| DELETE | /{billId} | Bill deletion |

### Sample JSON for Bill Service
##### Bill creation :

```sh
{
    "accountId": 1,
    "amount": 5000,
    "overdraftEnabled": "true"
}
```

#### Deposit Service

Makes a deposit by making a request to Account Service and Bill Service

| HTTP METHOD | PATH | USAGE |
| -----------| ------ | ------ |
| GET | /deposits | Deposit getting | 

It includes several requests for accounts and its bills:

##### Account Service Client

| HTTP METHOD | PATH | USAGE |
| -----------| ------ | ------ |
| GET | /accounts/{accountId} | Account getting by id | 

##### Bill Response DTO

| HTTP METHOD | PATH | USAGE |
| -----------| ------ | ------ |
| GET | /bills/{billId} | Bill getting by id | 
| PUT | /bills/{billId} | Bill updation by id | 
| GET | /account/{accountId} | Bills getting by account id |  

### Sample JSON for Deposit Service
##### Deposit creation : 
```sh
{
    "billId": 1,
    "amount": 3000
} 
```

#### Notification Service

This service is responsible for notifications. By using SimpleMailMessage class you can create some notification regarding your deposit.

### Infrastructure services

There's a bunch of common patterns in distributed systems, which could help us to make described core services work. Spring cloud provides powerful tools that enhance Spring 
Boot applications behaviour to implement those patterns. I'll cover them briefly.

#### Config Service

Spring Cloud Config is horizontally scalable centralized configuration service for distributed systems. This is a single place for storing configurations of other services.

Just build Spring Boot application with `spring-cloud-starter-config` dependency, autoconfiguration will do the rest.

Now you don't need any embedded properties in your application. Just provide `bootstrap.yml` :

```yml
spring:
  cloud:
    config:
      server:
        native:
          search-locations: classpath:/services
  profiles:
    active: native
  security:
    user:
      password: admin

server:
  port: 8001
```

#### API Gateway

In theory, a client could make requests to each of the microservices directly. But obviously, there are challenges and limitations with this option, like necessity to know all endpoints addresses, perform http request for each piece of information separately, merge the result on a client side. Another problem is non web-friendly protocols which might be used on the backend.

Usually a much better approach is to use API Gateway. It is a single entry point into the system, used to handle requests by routing them to the appropriate backend service or by invoking multiple backend services and aggregating the results. Also, it can be used for authentication, insights, stress and canary testing, service migration, static response handling, active traffic management.

In this project, I use Netflix realization - Zuul.

```yml
hystrix:
  command:
    default:
      execution:
        isolation:
          thread:
            timeoutInMilliseconds: 20000

ribbon:
  ReadTimeout: 20000
  ConnectTimeout: 20000

zuul:
  ignoredServices: '*'
  host:
    connect-timeout-millis: 20000
    socket-timeout-millis: 20000

  routes:
    account-service:
      path: /accounts/**
      serviceId: account-service
      stripPrefix: false

    bill-service:
      path: /bills/**
      serviceId: bill-service
      stripPrefix: false

    deposit-service:
      path: /deposits/**
      serviceId: deposit-service
      stripPrefix: false

server:
  port: 8989
```

#### Discovery Service

Another commonly known architecture pattern is Service discovery. It allows automatic detection of network locations for service instances, which could have dynamically assigned addresses because of auto-scaling, failures and upgrades.

The key part of Service discovery is Registry. I use Netflix Eureka in this project. Eureka is a good example of the client-side discovery pattern, when client is responsible for determining locations of available service instances (using Registry server) and load balancing requests across them.

With Spring Boot, you can easily build Eureka Registry with `spring-cloud-starter-eureka-server` dependency, `@EnableEurekaServer` annotation and simple configuration properties

Client support enabled with `@EnableDiscoveryClient` annotation an `bootstrap.yml` with application name:

```yml
spring:
  application:
    name: registry
  cloud:
    config:
      uri: http://config-service:8001
      fail-fast: true
      password: admin
      username: user

eureka:
  instance:
    preferIpAddress: true
  client:
    register-with-eureka: false
    fetch-registry: false
    server:
      waitTimeInMsWhenSyncEmpty: 0
  server:
    peer-node-read-timeout-ms: 100000
```
Also, Eureka provides a simple interface, where you can track running services and a number of available instances: `http://localhost:8761`

#### Load balancer, Circuit breaker and Http client

Netflix OSS provides another great set of tools.

##### Ribbon

Ribbon is a client side load balancer which gives you a lot of control over the behaviour of HTTP and TCP clients. Compared to a traditional load balancer, there is no need in additional hop for every over-the-wire invocation - you can contact desired service directly.

Out of the box, it natively integrates with Spring Cloud and Service Discovery. Eureka Client provides a dynamic list of available servers so Ribbon could balance between them.

##### Hystrix

Hystrix is the implementation of Circuit Breaker pattern, which gives a control over latency and failure from dependencies accessed over the network. The main idea is to stop cascading failures in a distributed environment with a large number of microservices. That helps to fail fast and recover as soon as possible - important aspects of fault-tolerant systems that self-heal.

Besides circuit breaker control, with Hystrix you can add a fallback method that will be called to obtain a default value in case the main command fails.

Moreover, Hystrix generates metrics on execution outcomes and latency for each command, that we can use to monitor system behavior.

##### Feign

Feign is a declarative Http client, which seamlessly integrates with Ribbon and Hystrix. Actually, with one `spring-cloud-starter-feign` dependency and `@EnableFeignClients` annotation you have a full set of Load balancer, Circuit breaker and Http client with sensible ready-to-go default configuration.

### How to run all the things?

Keep in mind, that you are going to start 7 Spring Boot applications, 3 Databases and RabbitMQ. Make sure you have `6 Gb` memory available for Docker. 
You can always run just vital services though: Gateway, Registry, Config, Account service and Bill service.

After setting up all the configurations, you must definitely do gradle build.

In order to build a docker image using a dockerfile for each service, the docker build command, for instance, for Account service will help us: `docker run -t account-service .` (where dot is the directory where the docker file is located).

To run the docker image, you need to use the docker run command for each service, for example: `docker run -p 8081: 8081 account-service: latest` (where `latest` is the most recent version of the image).

Docker-compose is configured in a .yml file.

In this project, I used a bash script in the docker files of each service (for the order in which containers are launched).

Images for each service are created by using `docker-compose build` command.

Directly launch is performed by command `docker-compose up`.

### Before you start
- Install Docker and Docker Compose.
- Launch the databases
- Launch RabbitMQ in Docker
- Make sure to build the project

### Important endpoints
- http://localhost:8989 - Gateway
- http://localhost:8761 - Eureka Dashboard
- http://localhost:15672 - RabbitMQ management (default login/password: guest/guest)

### Notes
- All Spring Boot applications require already running Config Server for startup. But we can start all containers simultaneously because of `depends_on` docker-compose option.
- Each microservice has its own database, so there is no way to bypass API and access persistance data directly.

## Contributions are welcome!

Bank Microservice is open source, and would greatly appreciate your help. Feel free to suggest and implement improvements.
