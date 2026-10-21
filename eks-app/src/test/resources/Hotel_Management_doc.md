# Hotel Management System Using SpringBoot Microservices

Welcome to the Hotel Management System project !!!

This repository contains application that is designed to manage various aspects of a hotel, including user registration, user authentication and authorization, booking rooms, adding hotels, and handling user ratings etc.

## Prerequisites

- Java 17
- Maven
- Spring Boot
- Spring Data JPA
- MySQL
- GIT
- JWT
- Lombok
- Feign Client
- Spring Security
- Spring Cloud
- Spring Cloud Gateway MVC
- Spring Cloud Config
- Spring Cloud Loadbalancer
- Resilience4j
- Netflix Eureka
- Docker

## Microservice Patterns Used 

#### Database Per Service Pattern:

A database dedicated to one service can't be accessed by other services. This is one of the reasons that makes it much easier to scale and understand from a whole end-to-end business aspect. This structure also reduces coupling as one service can’t tie itself to the tables of another. Services are forced to communicate via published interfaces. The downside is that dedicated databases require a failure protection mechanism for events where communication fails.

#### Saga Pattern: 

Utilized during payment and refund processes to ensure that a series of local transactions are either all executed or compensated. Each step is an individual local transaction with a corresponding compensating transaction.
- Example: If a payment fails, any blocked room is released. Similarly, if a refund cannot be processed, the reservation remains active.

#### API Gateway Pattern: 

All client requests are routed through an API Gateway (Spring Cloud Gateway), which directs requests to the appropriate microservices. The client also doesn’t need to know how to find or communicate with a multitude of ever-changing services. We can also create a gateway for specific types of clients (for example, backends for frontends) which improve ergonomics and reduce the number of roundtrips needed to fetch data. Plus, an API gateway pattern can take care of crucial tasks like authentication, SSL termination and caching, which makes your app more secure and user-friendly.

#### Circuit breaker design pattern:

Implementing this pattern as a function in a circuit breaker design requires an object to be called to monitor failure conditions. When a failure condition is detected, the circuit breaker will trip. Once this has been tripped, all calls to the circuit breaker will result in an error and be directed to a different service. Alternatively, calls can result in a default error message being retrieved.

There are three states of the circuit breaker pattern functions that developers should be aware of. These are:

- **Open:** A circuit breaker pattern is open when the number of failures has exceeded the threshold. When in this state, the microservice gives errors for the calls without executing the desired function.

- **Closed:** When a circuit breaker is closed, it's in the default state and all calls are responded to normally. This is the ideal state developers want a circuit breaker microservice to remain in — in a perfect world, of course.

- **Half-open:** When a circuit breaker is checking for underlying problems, it remains in a half-open state. Some calls may be responded to normally, but some may not be. It depends on why the circuit breaker switched to this state initially.

## Features

This application includes three role **Guest, User, and UserAdmin** each defining access levels for various URLs based on JWT tokens. Below are the services provided, along with their corresponding HTTP methods and access roles.

Below are the services used, their **GET/POST** Request, Roles needed to access URL and their features:

#### Eureka-Service:
 
Service Discovery to Register and discover microservices.

#### Config-Service:
 
Centralized configuration management for microservices. This service fetches config files from the git repository.

#### Auth-Gateway-Service:

Unified access point for microservices with JWT user authentication and authorization.

   - **Guest Role**
       > **POST:** Register User.
       > 
       > **POST:** Get JWT Token.

   - **Admin Role**
       > **GET:** Get User Credentials.
       > 
       > **GET:** Get User Roles.

#### User-Service:

   - **Admin Role**
       > **GET:** Get User details.

   - **UserAdmin Role**
       > **GET:** Get User by UserId.
       > 
       > **GET:** Get User by UserName.
       > 
       > **GET:** Get Wallet Details by UserId.

   - **User Role**
       > **GET:** Get Wallet Balance by UserId.
       > 
       > **POST:** Update Wallet Balance by UserId.

#### Hotel-Service:

   - **Guest Role**
       > **GET:** Get Hotel details.
       > 
       > **GET:** Get Hotel details by HotelId.
       > 
       > **GET:** Get Hotel details by Location.
       > 
       > **GET:** Get Hotel details by Name.
       > 
       > **GET:** Get Room details by HotelId.
       > 
       > **GET:** Get Room details by RoomId.
       > 
       > **GET:** Get Available Room details by HotelId.
       > 
       > **GET:** Get Hotel with Room details.

   - **Admin Role**
       > **POST:** Add Hotel.
       > 
       > **DELETE:** Delete Hotel by HotelId.
       > 
       > **GET:** Get Booked Room details by HotelId.
       > 
       > **POST:** Update Room status by HotelId.
       > 
       > **POST:** Add Room by HotelId.
       > 
       > **DELETE:** Delete Room by RoomId.

#### Rating-Service:

   - **Guest Role**
       > **GET:** Get All Ratings.

   - **UserAdmin Role**
       > **GET:** Get Rating by UserId.
       > 
       > **GET:** Get Rating by HotelId.

   - **User Role**
       > **POST:** Add Rating.

#### Payment-Service:

   - **Admin Role**
       > **GET:** Get Payment Details.

   - **UserAdmin Role**
       > **GET:** Get Payment Details by Id.
       > 
       > **POST:** Refund Payment by PaymentId.

   - **User Role**
       > **POST:** Make Payment by UserId.

#### Booking-Service:
   - **User Role**
       > **POST:** Create Booking.

   - **UserAdmin Role**
       > **GET:** Get Booking by Id.
       > 
       > **POST:** Cancel Booking by Id.

   - **Admin Role**
       > **GET:** Get All Booking.

## Installation

1. Clone the repository.

       git clone https://github.com/KarthikeyanNagarajan/Hotel-Management-System-Microservice.git

2. Navigate to the project directory.
      
       cd Hotel-Management-System-Microservice

3. Configure the properties of each microservice according to your requirements.

4. Run the Application in below order.

  - ##### Start Eureka-Service
        cd Eureka-Service
        mvn spring-boot:run

  - ##### Start Config-Service
        cd Config-Service
        mvn spring-boot:run

  - ##### Start Auth-Gateway-Service
        cd Auth-Gateway-Service
        mvn spring-boot:run

  - ##### Start User-Service
        cd User-Service
        mvn spring-boot:run

  - ##### Start Hotel-Service
        cd Hotel-Service
        mvn spring-boot:run

  - ##### Start Rating-Service
        cd Rating-Service
        mvn spring-boot:run

  - ##### Start Payment-Service
        cd Payment-Service
        mvn spring-boot:run

  - ##### Start Booking-Service
        cd Booking-Service
        mvn spring-boot:run

5. Check Service Status in Eureka Dashboard. It will show which services are up and running and their corresponding instances.
  - ##### Eureka Dashboard: 
        http://localhost:8761

6. Import Postman Collection which contains all the URL routes for all services.
           
       Hotel-Management-System-Microservice.postman_collection.json

## Contributing

Contributions are welcome! Please fork the repository and create a pull request.
